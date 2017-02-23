package com.alphagao.coolerweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Alpha on 2017/2/22.
 */

public class SearchBasic {

    public Basic basic;
    public String status;

    public class Basic {
        public String city;
        @SerializedName("cnty")
        public String country;
        @SerializedName("id")
        public String cityId;
        @SerializedName("lat")
        public String latitude;
        @SerializedName("lon")
        public String longitude;
        @SerializedName("prov")
        public String province;
    }

}
