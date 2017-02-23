package com.alphagao.coolerweather.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.alphagao.coolerweather.R;
import com.alphagao.coolerweather.gson.SearchBasic;
import com.alphagao.coolerweather.gson.Weather;
import com.alphagao.coolerweather.utils.HttpUtil;
import com.alphagao.coolerweather.utils.Utility;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Alpha on 2016/12/22.
 */

public class AutoUpdateService extends Service {

    private static final String TAG = "AutoUpdateService";
    public static final String LOC_NAME = "loc_name";
    public static final String LOC_LATITUDE = "loc_latitude";
    public static final String LOC_LONGITUDE = "loc_longitude";
    private SharedPreferences prefs;
    private LocationClient locationClient;
    private String districtName;
    private double latitude;
    private double longitude;
    private SearchBasic searchBasic;
    public static Handler handler;
    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        getLocation();
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateWeather();
        updateBingPic();
        setUpdateUnit();
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather old_weather = Utility.handleWeatherResponse(weatherString);
            String weatherID = old_weather.basic.weatherId;
            String weatherUrl = getString(R.string.api_weather, weatherID);
            HttpUtil.sendOKHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseTxt = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseTxt);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("weather", responseTxt);
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(getApplicationContext(), "自动更新失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateBingPic() {
        String bingPicUrl = getString(R.string.api_bing_pic);
        HttpUtil.sendOKHttpRequest(bingPicUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), "自动更新图片失败", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void setUpdateUnit() {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int time_unit = 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + time_unit;
        Intent intent = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation == null) {
                Toast.makeText(AutoUpdateService.this, "网络定位失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation
                    || bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                //也可以使用 bdLocation.getWeatherIdAndCityName()
                if (bdLocation.getDistrict() != null) {
                    Log.d(TAG, "onReceiveLocation: " + bdLocation.getDistrict());
                    if (handler != null) {
                        districtName = bdLocation.getDistrict();
                        latitude = bdLocation.getLatitude();
                        longitude = bdLocation.getLongitude();
                        //根据地址查询该地区天气 ID
                        queryCountryIdFromName(districtName);
                        //Toast.makeText(AutoUpdateService.this, "定位地址是：" + districtName, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }

    }

    /**
     * 根据城市名获取城市 ID
     *
     * @param districtName 城市名
     * @return 城市 ID
     */
    private void queryCountryIdFromName(String districtName) {
        HttpUtil.sendOKHttpRequest(getString(R.string.api_city_search, districtName),
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Toast.makeText(AutoUpdateService.this, "定位地址 ID 查询失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        searchBasic = Utility.handleSearchResponse(response.body().string());
                        //若定位成功则向实例化 Handler 的线程发送消息
                        if (validateLocInfo(searchBasic)) {
                            handler.sendEmptyMessage(0);
                        }
                    }
                });
    }

    /**
     * 对查询的模型进行正确性校验
     *
     * @param searchBasic 结果模型
     * @return 是否定位成功
     */
    private boolean validateLocInfo(SearchBasic searchBasic) {
        Double searchLatitude = Double.valueOf(searchBasic.basic.latitude);
        Double seatchLongiitude = Double.valueOf(searchBasic.basic.longitude);
        Double locationLatitude = Double.valueOf(latitude);
        Double locationLongitude = Double.valueOf(longitude);
        /*Log.d(TAG, "validateLocInfo: searchLatitude:" + searchLatitude);
        Log.d(TAG, "validateLocInfo: seatchLongiitude:" + seatchLongiitude);
        Log.d(TAG, "validateLocInfo: locationLatitude:" + locationLatitude);
        Log.d(TAG, "validateLocInfo: locationLongitude:" + locationLongitude);*/
        //检测误差在一定范围之内，定位成功
        if (Math.abs(searchLatitude - locationLatitude) <= 0.5 &&
                Math.abs(seatchLongiitude - locationLongitude) <= 0.5) {
            return true;
        }
        return false;
    }

    public class MyBinder extends Binder {

        public String[] getWeatherIdAndCityName() {
            return new String[]{searchBasic.basic.cityId, districtName};
        }
    }

    private void getLocation() {
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(new MyLocationListener());
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(10 * 60 * 1000);
        option.setIsNeedAddress(true);
        locationClient.setLocOption(option);
        locationClient.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationClient.stop();
    }
}
