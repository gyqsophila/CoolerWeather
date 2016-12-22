package com.alphagao.coolerweather.gson;

/**
 * Created by Alpha on 2016/12/22.
 */

public class AQI {

    public AQICity city;

    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
