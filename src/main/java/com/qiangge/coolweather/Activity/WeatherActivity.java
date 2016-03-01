package com.qiangge.coolweather.Activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qiangge.coolweather.R;
import com.qiangge.coolweather.util.HttpCallbackListener;
import com.qiangge.coolweather.util.HttpUtil;
import com.qiangge.coolweather.util.Utility;

/**
 * Created by 罗强强 on 2016/3/1.
 */
public class WeatherActivity extends Activity {
    private LinearLayout weatherInfoLayout;

    private TextView cityNameText;//显示市名
    private TextView publicTimeText;//显示发布时间
    private TextView weatherDespText;//显示天气描述信息
    private TextView temp1Text;//显示气温1
    private TextView temp2Text;//显示气温2
    private TextView currentDateText;//显示当前日期

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);

        weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
        cityNameText = (TextView) findViewById(R.id.city_name);
        publicTimeText = (TextView) findViewById(R.id.publish_text);
        weatherDespText = (TextView) findViewById(R.id.weather_desp);
        temp1Text = (TextView) findViewById(R.id.temp1);
        temp2Text = (TextView) findViewById(R.id.temp2);
        currentDateText = (TextView) findViewById(R.id.current_date);

        String countryCode = getIntent().getStringExtra("country_code");
        if(!TextUtils.isEmpty(countryCode)){
            publicTimeText.setText("同步中.....");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherCode(countryCode);
        }else{
            showWeather();
        }
    }
    //从SharedPreferences文件读取存储的天气信息，并显示到界面上
    private void showWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
        cityNameText.setText(prefs.getString("city_name",""));
        temp1Text.setText(prefs.getString("temp1",""));
        temp2Text.setText(prefs.getString("temp2",""));
        weatherDespText.setText(prefs.getString("weather_desp",""));
        publicTimeText.setText("今天"+prefs.getString("publish_time","")+"发布");
        currentDateText.setText(prefs.getString("current_date",""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
    }

    //查询县级代号所对应的天气
    private void queryWeatherCode(String countryCode) {
        String address = "http://www.weather.com.cn/data/list3/city"+countryCode+".xml";
        queryFromServer(address,"countryCode");
    }
    //查询天气代号所对应得天气
    private void queryWeatherInfo(String weatherCode){
        String address = "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
        queryFromServer(address,"weatherCode");
    }
    //根据传入的地址和类型去向服务器查询天气代号或者天气信息
    private void queryFromServer(String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String respones) {
                if("countryCode".equals(type)){
                    if(!TextUtils.isEmpty(respones)){
                        //从服务器返回的数据中解析出天气代号
                        String [] array = respones.split("\\|");
                        if(array !=null && array.length==2){
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                }else if("weatherCode".equals(type)){
                    //处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this,respones);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publicTimeText.setText("同步失败！");
                    }
                });
            }
        });
    }
}
