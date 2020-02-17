package com.example.matterd.weatherapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.EditText
import android.widget.ListView
import android.widget.Toolbar
import java.net.URL
import java.net.URLEncoder
import android.view.View
import android.view.inputmethod.InputMethodManager
import org.json.JSONException
import org.json.JSONObject
import android.support.design.widget.Snackbar
import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection


class MainActivity : Activity() {
    private var weatherList: MutableList<Weather> = mutableListOf()
    private var weatherArrayAdapter: WeatherArrayAdapter? = null
    private var weatherListView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setActionBar(toolbar)

        weatherListView = findViewById(R.id.weatherListView)
        weatherArrayAdapter = WeatherArrayAdapter(this, weatherList)
        weatherListView?.let { it.adapter = weatherArrayAdapter }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val locationEditText = findViewById<EditText>(R.id.locationEditText)
            val url = createURL(locationEditText.text.toString())

            if (url != null) {
                dismissKeyboard(locationEditText)
                GetWeatherTask().execute(url)
            } else {
                Snackbar.make(findViewById(R.id.coordinatorLayout),
                    R.string.invalid_url, Snackbar.LENGTH_LONG).show()
            }

        }
    }

    private fun createURL(city: String): URL? {
        val apiKey = getString(R.string.api_key)
        val baseUrl = getString(R.string.web_service_url)

        try {
            // create URL for specified city and imperial units (Fahrenheit)
            val urlString = baseUrl.format(URLEncoder.encode(city, "UTF-8"), 16, apiKey)
            Log.d("createURL", "url is $urlString")

            return URL(urlString)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null // URL was malformed
    }

    private fun dismissKeyboard(view: View) {
        val imm = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetWeatherTask : AsyncTask<URL, Void, JSONObject>() {

        override fun doInBackground(vararg params: URL): JSONObject? {
            var connection: HttpURLConnection? = null

            try {
                connection = params[0].openConnection() as HttpURLConnection
                val response = connection.responseCode

                if (response == HttpURLConnection.HTTP_OK) {
                    val builder = StringBuilder()

                    try {
                        BufferedReader(
                            InputStreamReader(connection.inputStream)
                        ).use { reader ->
                            var line: String?

                            while (true) {
                                line = reader.readLine()
                                if (line == null)
                                    break

                                builder.append(line)
                            }
                        }
                    } catch (e: IOException) {
                        Snackbar.make(
                            findViewById(R.id.coordinatorLayout),
                            R.string.read_error, Snackbar.LENGTH_LONG
                        ).show()
                        e.printStackTrace()
                    }

                    return JSONObject(builder.toString())
                } else {
                    Snackbar.make(
                        findViewById(R.id.coordinatorLayout),
                        R.string.connect_error, Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    findViewById(R.id.coordinatorLayout),
                    R.string.connect_error, Snackbar.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                connection!!.disconnect() // close the HttpURLConnection
            }

            return null
        }

        // process JSON response and update ListView
        override fun onPostExecute(weather: JSONObject) {
            convertJSONtoArrayList(weather) // repopulate weatherList
            weatherArrayAdapter?.notifyDataSetChanged() // rebind to ListView
            weatherListView?.smoothScrollToPosition(0) // scroll to top
        }
    }

    // create Weather objects from JSONObject containing the forecast
    private fun convertJSONtoArrayList(forecast: JSONObject) {
        weatherList.clear()

        try {
            // get forecast's "list" JSONArray
            val list = forecast.getJSONArray("list")

            // convert each element of list to a Weather object
            for (i in 0 until list.length()) {
                val day = list.getJSONObject(i) // get one day's data

                // get the day's temperatures ("temp") JSONObject
                val temperatures = day.getJSONObject("temp")

                val temperatureMin = temperatures.getDouble("min") //(temperatures.getDouble("min") - 32) * 5.0/9
                val temperatureMax = temperatures.getDouble("max") //(temperatures.getDouble("max") - 32) * 5.0/9

                // get day's "weather" JSONObject for the description and icon
                val weather = day.getJSONArray("weather").getJSONObject(0)


                // add new Weather object to weatherList
                weatherList.add(
                    Weather(
                        day.getLong("dt"), // date/time timestamp
                        temperatureMin,
                        temperatureMax,
                        day.getDouble("humidity"), // percent humidity
                        weather.getString("description"), // weather conditions
                        weather.getString("icon")
                    )
                ) // icon name
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }
}

