package com.qiangge.coolweather.Activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.qiangge.coolweather.R;
import com.qiangge.coolweather.db.CoolWeatherDB;
import com.qiangge.coolweather.model.City;
import com.qiangge.coolweather.model.Country;
import com.qiangge.coolweather.model.Province;
import com.qiangge.coolweather.util.HttpCallbackListener;
import com.qiangge.coolweather.util.HttpUtil;
import com.qiangge.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 罗强强 on 2016/2/29.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();
    
    private List<Province> provinceList;//省列表
    private List<City> cityList;//市列表
    private List<Country> countryList;//县列表
    
    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的城市
    private int currentLevel;//当前选中的级别

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prfes = PreferenceManager.getDefaultSharedPreferences(ChooseAreaActivity.this);
        if(prfes.getBoolean("city_selected",false)){
            Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return ;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(ChooseAreaActivity.this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(ChooseAreaActivity.this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String countryCode = countryList.get(position).getCountryCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("country_code",countryCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvince();
    }

    //查询全国所有的省，优先从数据库查询，如果没有查询到在到服务器中查询
    private void queryProvince() {
        provinceList = coolWeatherDB.loadProvinces();
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else {
            queryFromServer(null,"province");
        }
    }

   //查询选中市内的所有县，优先冲数据库中查询，如果没有在到服务器中查询
    private void queryCounties() {
        countryList = coolWeatherDB.loadCounties(selectedCity.getId());
        if(countryList.size()>0){
            dataList.clear();
            for(Country country : countryList){
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else{
            queryFromServer(selectedCity.getCityCode(),"country");
        }
    }
    //查询选中省内所有的市，优先从数据库中查询，如果没有在到服务器中查询
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if(cityList.size()>0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else{
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }
    //根据传入的代号和类型从服务器上查询省市县数据
    private void queryFromServer(final String code,final String type) {
        String address;
        if(!TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else{
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String respones) {
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(coolWeatherDB,respones);
                }else if("city".equals(type)){
                    result = Utility.handleCitiesResponse(coolWeatherDB,respones,selectedProvince.getId());
                }else if("country".equals(type)){
                    result = Utility.handleCountiesResponse(coolWeatherDB,respones,selectedCity.getId());
                }
                if(result){
                    //通过runOnUiThread()方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvince();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("country".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败！",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    //关闭进度对话框
    private void closeProgressDialog() {
        if(progressDialog !=null){
            progressDialog.dismiss();
        }
    }
    //显示进度对话框
    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(ChooseAreaActivity.this);
            progressDialog.setMessage("正在加载.....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    //捕获Back按键，根据当前的级别来判断，此时应该返回市列表，省列表，还是直接退出

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if(currentLevel == LEVEL_CITY) {
            queryProvince();
        }else {
            finish();
        }
    }
}
