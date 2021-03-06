package com.example.jkakeno.stormyupdated;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


//This app uses okhttp library from square as the API to connect to the network and weather API from darksky to access forecast data from the service.
//The app uses fragments to display data on phone and tablets. It supports landscape and portrait mode.
//It displays the current, weekly, hourly weather and weather summary for the selected day or hour for a fixed location.

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
//Tags to save data and pass to another class via intent
    public static final String DAYS = "DAYS";
    public static final String HOURS = "HOURS";
    public static final String VIEWPAGER_FRAGMENT = "viewpager_fragment";
    private Forecast mForecast;
    final double latitude = 37.8267;
    final double longitude =-122.4233;
    String apiKey = "9ebd472fb313c16504d62d5ec16f018e";
    boolean isTablet;

//Bind views to member variables
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.detailsButton) Button mDetailsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//Store the resource identifier in a variable
        isTablet= getResources().getBoolean(R.bool.is_tablet);
//Set the initial view
        setContentView(R.layout.activity_main);
//Use butter knife to bind the views
        ButterKnife.bind(this);
//Set the progress bar visibility to invisible
        mProgressBar.setVisibility(View.INVISIBLE);
//Set listener to the refresh button
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//Get forecast when press refresh button
                getForecast(latitude,longitude);
            }
        });
//Get forecast for phone and tablet
        if (!isTablet) {
            getForecast(latitude, longitude);
        } else {
            getForecast(latitude, longitude);
        }
//Set listener for detail button
        mDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDetailsButtonSelected();
            }
        });
    }

    private void onDetailsButtonSelected() {
//Store daily forecast, hourly forecast in tags and put the tags value in a bundle
        Bundle bundle = new Bundle();
        bundle.putParcelableArray(DAYS, mForecast.getDailyForecast());
        bundle.putParcelableArray(HOURS, mForecast.getHourlyForecast());
//Create the fragment manager
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
//When devise is phone
        if (!isTablet) {
//Create the follow up fragment
            ViewPagerFragment viewPagerFragment = new ViewPagerFragment();
            viewPagerFragment.setArguments(bundle);
//Replace the place holder with the follow up fragment with what's in the tag
            transaction.replace(R.id.placeHolder, viewPagerFragment, VIEWPAGER_FRAGMENT);
//When devise is tablet
        } else {
//Create the follow up fragment
            DualPanelFragment dualPanelFragment = new DualPanelFragment();
            dualPanelFragment.setArguments(bundle);
//Replace the place holder with the follow up fragment with what's in the tag
            transaction.replace(R.id.placeHolder, dualPanelFragment, VIEWPAGER_FRAGMENT);
        }
//Add the initial fragment to the back stack to be able to return with a back button
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void getForecast(double latitude, double longitude) {
        String  forecastUrl ="https://api.darksky.net/forecast/" + apiKey + "/" +latitude+ "," +longitude;
        if (isNetworkAvailable()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toggleRefresh();
                }
            });
//Create a new OkHttpClient object to connect to the internet
            OkHttpClient client = new OkHttpClient();
//Create a request that the client will send to the server with the URL variable
            Request request = new Request.Builder().url(forecastUrl).build();
//Put the request inside a call object
            Call call = client.newCall(request);
//Make the call asyncronous and override onFailure () and onResponse ()
            call.enqueue(new Callback() {
//CALL FAILED
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }
//CALL SUCCESS
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
//If the call is successful then a response will be received, log the response body and create a mForecast object
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
//RESPONSE IS SUCCESSFUL update the display
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        updateDisplay();
                                }
                            });
//RESPONSE IS NOT SUCCESSFUL alert the user of the error
                        } else {
                            alertUserAboutError();
                        }
//Handel the call onResponse()
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
//Handel parseForecastDetails(), getDailyForecast(), getHourlyForecast, getCurrentDetails()
                    } catch (JSONException e){
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
//If network is not available toast message
        }else{
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG).show();
        }
    }


    private void toggleRefresh() {
        if (mProgressBar.getVisibility()==View.INVISIBLE){
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }


    private void updateDisplay() {
        Current current = mForecast.getCurrent();
//Update texts on the display
        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At" + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(current.getHumidity() + "");
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary());
//Update the weather icon
        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
   }

//Create a forecast object, set the current, hourly forecast, daily forecast and return the forecast
    private Forecast parseForecastDetails (String jsonData) throws JSONException{
        Forecast forecast = new Forecast();
        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));
        return forecast;
    }

//Create an array of days for the daily forecast, set the summary, temperature max, weather icon, time, time zone for each array item and return the array
    private Day[] getDailyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");
        Day [] days = new Day[data.length()];
        for (int i=0; i<data.length(); i++){
            JSONObject jsonHour = data.getJSONObject(i);
            Day day = new Day();
            day.setSummary(jsonHour.getString("summary"));
            day.setTemperatureMax(jsonHour.getDouble("temperatureMax"));
            day.setIcon(jsonHour.getString("icon"));
            day.setTime(jsonHour.getLong("time"));
            day.setTimeZone(timezone);
            days[i] = day;
        }
        return days;
    }

//Create an array of hours for the hourly forecast, set the summary, temperature, weather icon, time, time zone for each array item and return the array
    private Hour[] getHourlyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");
        Hour[] hours = new Hour[data.length()];
        for (int i=0; i<data.length(); i++){
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();
            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimeZone(timezone);
            hours[i] = hour;
        }
        return hours;
    }


//Create a current object, set the humidity, time, weather icon, precipitation chance, summary, temp, time zone and return the current
    private Current getCurrentDetails(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG,"From JSON: " + timezone);
        JSONObject currently = forecast.getJSONObject("currently");
        Current current =new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimeZone(timezone);
        Log.d(TAG, current.getFormattedTime());
        return current;
    }

//Check the network availability
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo !=null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

//Alert user about error with an alert dialog fragment
    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }
}
