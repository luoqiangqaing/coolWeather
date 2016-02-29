package com.qiangge.coolweather.util;

/**
 * Created by 罗强强 on 2016/2/29.
 */
public interface HttpCallbackListener {
    void onFinish(String respones);
    void onError(Exception e);
}
