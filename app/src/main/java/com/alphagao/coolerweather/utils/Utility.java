package com.alphagao.coolerweather.utils;

import android.text.TextUtils;

import com.alphagao.coolerweather.db.City;
import com.alphagao.coolerweather.db.County;
import com.alphagao.coolerweather.db.Province;
import com.alphagao.coolerweather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Alpha on 2016/12/21.
 */

public class Utility {
    private static final String TAG = "Utility";

    public static boolean handleProvinceResponse(String response) {
        if (!(TextUtils.isEmpty(response))) {
            try {
                JSONArray provinceArray = new JSONArray(response);
                for (int i=0;i<provinceArray.length();i++) {
                    JSONObject provinceObj = provinceArray.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObj.getString("name"));
                    province.setProvinceCode(provinceObj.getInt("id"));
                    province.save();//保存到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean handleCityResponse(String response ,int provinceID) {
        if (!(TextUtils.isEmpty(response))) {
            try {
                JSONArray cityArray = new JSONArray(response);
                for (int i=0;i<cityArray.length();i++) {
                    JSONObject cityObj = cityArray.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObj.getString("name"));
                    city.setCityCode(cityObj.getInt("id"));
                    city.setProvinceID(provinceID);
                    city.save();//保存到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public static boolean handleCountyResponse(String response ,int cityID) {
        if (!(TextUtils.isEmpty(response))) {
            try {
                JSONArray countyArray = new JSONArray(response);
                for (int i=0;i<countyArray.length();i++) {
                    JSONObject countyObj = countyArray.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObj.getString("name"));
                    county.setWeatherID(countyObj.getString("weather_id"));
                    county.setCityID(cityID);
                    county.save();//保存到数据库
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Weather handleWeatherResponse(String response) {
        try {
            //Log.d(TAG, "handleWeatherResponse: "+response);
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
