package com.alphagao.coolerweather;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alphagao.coolerweather.gson.Forecast;
import com.alphagao.coolerweather.gson.Weather;
import com.alphagao.coolerweather.services.AutoUpdateService;
import com.alphagao.coolerweather.utils.HttpUtil;
import com.alphagao.coolerweather.utils.Utility;
import com.bumptech.glide.Glide;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Alpha on 2016/12/22.
 */

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";
    @BindView(R.id.title_city)
    TextView titleCity;
    @BindView(R.id.title_update_time)
    TextView titleUpdateTime;
    @BindView(R.id.degree_txt)
    TextView degreeTxt;
    @BindView(R.id.weather_info_txt)
    TextView weatherInfoTxt;
    @BindView(R.id.forecast_layout)
    LinearLayout forecastLayout;
    @BindView(R.id.aqi_txt)
    TextView aqiTxt;
    @BindView(R.id.pm25_txt)
    TextView pm25Txt;
    @BindView(R.id.comfort_txt)
    TextView comfortTxt;
    @BindView(R.id.car_wash_txt)
    TextView carWashTxt;
    @BindView(R.id.sport_txt)
    TextView sportTxt;
    @BindView(R.id.weather_layout)
    ScrollView weatherLayout;
    @BindView(R.id.bing_pic_img)
    ImageView bingPicImg;
    @BindView(R.id.swipe_refeash)
    SwipeRefreshLayout swipeRefeash;
    @BindView(R.id.nav_button)
    Button navButton;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    private String weatherId;
    private AutoUpdateService.MyBinder binder;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        setBackgroundFit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        initBackImg(prefs);
        initWeatherInfo(prefs);
        initRefresh();
    }

    private void setBackgroundFit() {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initWeatherInfo(SharedPreferences prefs) {
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            weatherId = getIntent().getStringExtra("weather_id");
            Log.d(TAG, "initWeatherInfo: " + weatherId);
            weatherLayout.setVisibility(View.GONE);
            requestWeather(weatherId);
        }
    }

    private void loadBingPic() {
        String requestBingPic = getString(R.string.api_bing_pic);
        HttpUtil.sendOKHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), "背景图加载失败。", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载每日背景图
     *
     * @param prefs
     */
    private void initBackImg(SharedPreferences prefs) {
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
    }

    /**
     * 初始化下拉刷新的配置
     */
    private void initRefresh() {
        swipeRefeash.setColorSchemeResources(R.color.colorAccent);
        swipeRefeash.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }

    /**
     * 请求新的天气数据
     * @param weather_id 该地区天气 ID
     */
    public void requestWeather(String weather_id) {
        weatherId = weather_id;
        String weatherUrl = getString(R.string.api_weather, weather_id);
        //Log.d(TAG, "requestWeather: "+weatherUrl);
        HttpUtil.sendOKHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseTxt = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseTxt);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseTxt);
                            editor.apply();
                            showWeatherInfo(weather);
                            loadBingPic();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.weather_load_error), Toast.LENGTH_SHORT);
                        }
                        swipeRefeash.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), getString(R.string.weather_load_error),
                        Toast.LENGTH_SHORT);
                swipeRefeash.setRefreshing(false);
            }
        });
    }

    /**
     * 显示嫌弃数据
     * @param weather 天气对象
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeTxt.setText(degree);
        weatherInfoTxt.setText(weatherInfo);

        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            TextView dateTxt = (TextView) view.findViewById(R.id.date_txt);
            TextView infoTxt = (TextView) view.findViewById(R.id.info_txt);
            TextView maxTxt = (TextView) view.findViewById(R.id.max_txt);
            TextView minTxt = (TextView) view.findViewById(R.id.min_txt);
            dateTxt.setText(forecast.date);
            infoTxt.setText(forecast.more.info);
            maxTxt.setText(forecast.temperature.max + "℃");
            minTxt.setText(forecast.temperature.min + "℃");
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiTxt.setText(weather.aqi.city.aqi);
            pm25Txt.setText(weather.aqi.city.pm25);
        }
        String comfort = getString(R.string.weather_comf_kg, weather.suggestion.comfot.info);
        String carWash = getString(R.string.weather_car_kg, weather.suggestion.carWash.info);
        String sport = getString(R.string.weather_sport_kg, weather.suggestion.sport.info);
        comfortTxt.setText(comfort);
        carWashTxt.setText(carWash);
        sportTxt.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        AutoUpdateService.handler = handler;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (binder != null) {
                Log.d(TAG, "handleMessage: " + binder.getWeatherIdAndCityName()[0]);
                Log.d(TAG, "handleMessage: weatherId:"+weatherId);
                //当位置发生变化的时候
                if (!binder.getWeatherIdAndCityName()[0].equals(weatherId) ) {
                    alertToChangeLocation();
                }
            }
        }
    };

    //弹窗提示用户是否切换到定位城市
    private void alertToChangeLocation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.weather_loc_title))
                .setMessage(getString(R.string.weather_loc_content, binder.getWeatherIdAndCityName()[1]))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.weather_loc_change), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestWeather(binder.getWeatherIdAndCityName()[0]);
                    }
                })
                .setNegativeButton(getString(R.string.weather_loc_not_change), null)
                .show();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (AutoUpdateService.MyBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @OnClick(R.id.nav_button)
    public void onClick() {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
