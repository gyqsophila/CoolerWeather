package com.alphagao.coolerweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Alpha on 2016/12/22.
 */

public class NOW {
    @SerializedName("tmp")
    public String temperature;
    @SerializedName("cond")
    public More more;

    public class More{
        @SerializedName("txt")
        public String info;
    }
}
