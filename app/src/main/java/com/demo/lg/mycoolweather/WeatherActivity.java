package com.demo.lg.mycoolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.demo.lg.mycoolweather.gson.Forecast;
import com.demo.lg.mycoolweather.gson.Weather;
import com.demo.lg.mycoolweather.util.HttpUtil;
import com.demo.lg.mycoolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    private ScrollView scrollViewWeather;
    private TextView cityTitle;
    private TextView updateTimeTitle;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView backgroundImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        scrollViewWeather = (ScrollView) findViewById(R.id.scrollView_weather);
        cityTitle = (TextView) findViewById(R.id.text_cityTitle);
        updateTimeTitle = (TextView) findViewById(R.id.text_updateTimeTitle);
        degreeText = (TextView) findViewById(R.id.text_degree);
        weatherInfoText = (TextView) findViewById(R.id.text_weatherInfo);
        forecastLayout = (LinearLayout) findViewById(R.id.ll_forecast);
        aqiText = (TextView) findViewById(R.id.text_aqi);
        pm25Text = (TextView) findViewById(R.id.text_pm25);
        comfortText = (TextView) findViewById(R.id.text_comfort);
        carWashText = (TextView) findViewById(R.id.text_washCar);
        sportText = (TextView) findViewById(R.id.text_sport);
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = sharedPreferences.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            scrollViewWeather.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        backgroundImg = (ImageView) findViewById(R.id.img_background);
        String backgroundImgUrl = sharedPreferences.getString("bgImgUrl", null);
        if (backgroundImgUrl != null) {
            Glide.with(this).load(backgroundImgUrl).into(backgroundImg);
        } else {
            loadBackgroundImg();
        }
    }

    private void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=0a257c596bb94362ad5df2a94e686229";
        Log.i(TAG, "requestWeather: url:" + weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && (weather.status).equals("ok")) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                            loadBackgroundImg();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        cityTitle.setText(cityName);
        updateTimeTitle.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            TextView dateText = view.findViewById(R.id.text_date);
            TextView infoText = view.findViewById(R.id.text_info);
            TextView maxText = view.findViewById(R.id.text_max);
            TextView minText = view.findViewById(R.id.text_min);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        scrollViewWeather.setVisibility(View.VISIBLE);
    }

    private void loadBackgroundImg() {
        String imgRequestUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(imgRequestUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String imgUrl = response.body().string();
                SharedPreferences.Editor editor =
                        PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this)
                                .edit();
                editor.putString("bgImgUrl", imgUrl);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(imgUrl).into(backgroundImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }
}
