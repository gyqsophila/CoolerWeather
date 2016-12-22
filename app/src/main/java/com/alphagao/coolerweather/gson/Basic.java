package com.alphagao.coolerweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Alpha on 2016/12/22.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
