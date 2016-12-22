package com.alphagao.coolerweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Alpha on 2016/12/22.
 */

public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public NOW now;
    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
