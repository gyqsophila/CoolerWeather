package com.alphagao.coolerweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alphagao.coolerweather.gson.Forecast;
import com.alphagao.coolerweather.gson.Weather;
import com.alphagao.coolerweather.utils.HttpUtil;
import com.alphagao.coolerweather.utils.Utility;
import com.bumptech.glide.Glide;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        initBackImg(prefs);
        initWeatherInfo(prefs);
    }

    private void initWeatherInfo(SharedPreferences prefs) {
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weather_id = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.GONE);
            requestWeather(weather_id);
        }
    }

    private void initBackImg(SharedPreferences prefs) {
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
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

    private void requestWeather(String weather_id) {
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
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), getString(R.string.weather_load_error),
                        Toast.LENGTH_SHORT);
            }
        });
    }

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
    }
}
