package com.alphagao.coolerweather.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.alphagao.coolerweather.R;
import com.alphagao.coolerweather.gson.Weather;
import com.alphagao.coolerweather.utils.HttpUtil;
import com.alphagao.coolerweather.utils.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Alpha on 2016/12/22.
 */

public class AutoUpdateService extends Service {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        int time_unit = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + time_unit;
        Intent intent = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }
}
