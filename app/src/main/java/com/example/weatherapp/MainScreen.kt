package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.weatherapp.model.RealmHelper
import com.example.weatherapp.model.RealmHelper.loadWeatherDataFromRealm
import com.example.weatherapp.model.RealmHelper.realmConfiguration
import com.example.weatherapp.model.RealmHelper.saveWeatherDataToRealm
import com.example.weatherapp.model.WeatherRealmModel
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.utils.RetrofitInstance
import com.google.android.gms.location.LocationServices
import io.realm.kotlin.Realm
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeatherApp() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val apiKey = "4b21b6d635a1cbfb56a6aed26bd5552f"

    val cities = listOf(
        "New York" to Pair(40.7128, -74.0060),
        "Singapore" to Pair(1.352083, 103.819839),
        "Mumbai" to Pair(19.075983, 72.877655),
        "Delhi" to Pair(28.704060, 77.102493),
        "Sydney" to Pair(-33.8688, 151.2093),
        "Melbourne" to Pair(-37.8136, 144.9631)
    )

    var weatherData by remember { mutableStateOf<Map<String, WeatherResponse>>(emptyMap()) }
    var currentWeatherResponse by remember { mutableStateOf<WeatherResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    @SuppressLint("ServiceCast")
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun convertUnixToTime(unixTimestamp: Long): String {
        val instant = Instant.ofEpochSecond(unixTimestamp)
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return localTime.format(formatter)
    }

    fun getWeatherDataForCities() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val weatherMap = mutableMapOf<String, WeatherResponse>()
                for ((city, coords) in cities) {
                    val response = RetrofitInstance.api.getCurrentWeather(
                        coords.first,
                        coords.second,
                        apiKey
                    )
                    saveWeatherDataToRealm(
                        cityName = response.name,
                        temperature = (response.main.temp - 273.15).toInt(),
                        description = response.weather.firstOrNull()?.description.orEmpty(),
                        country = response.sys.country,
                        windSpeed = response.wind.speed.toString(),
                        sunrise = convertUnixToTime(response.sys.sunrise),
                        sunset = convertUnixToTime(response.sys.sunset)
                    )
                    weatherMap[city] = response
                }
                weatherData = weatherMap
            } catch (e: Exception) {
                errorMessage = e.message.toString()
            } finally {
                isLoading = false
            }
        }
    }

    fun getCurrentLocationWeather(latitude: Double, longitude: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getCurrentWeather(latitude, longitude, apiKey)
                currentWeatherResponse = response
                saveWeatherDataToRealm(
                    cityName = response.name,
                    temperature = (response.main.temp - 273.15).toInt(),
                    description = response.weather.firstOrNull()?.description.orEmpty(),
                    country = response.sys.country,
                    windSpeed = response.wind.speed.toString(),
                    sunrise = convertUnixToTime(response.sys.sunrise),
                    sunset = convertUnixToTime(response.sys.sunset)
                )
            } catch (e: Exception) {
                errorMessage = e.message.toString()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@LaunchedEffect
        }
        coroutineScope.launch {
            weatherData = loadWeatherDataFromRealm()
            isLoading = false

            if (isNetworkAvailable(context)) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        getWeatherDataForCities()
                        getCurrentLocationWeather(location.latitude, location.longitude)
                    } else {
                        errorMessage = "Unable to get location"
                    }
                }
            } else {
                errorMessage = "No internet connection"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            currentWeatherResponse?.let { response ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Current Location Weather",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image( painter = painterResource(id = R.drawable.weather),
                            contentDescription = "Logo",
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "${(response.main.temp - 273.15).toInt()}°C",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = response.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "Country: ${response.sys.country}")
                        Text(text = "Weather: ${response.weather.firstOrNull()?.description}")
                        Text(text = response.wind.toString())
                        Row {
                            val sunrise = convertUnixToTime(response.sys.sunrise)
                            val sunset = convertUnixToTime(response.sys.sunset)
                            Text(text = "Sunrise: $sunrise am ")
                            Text(text = " Sunset: $sunset pm")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (isNetworkAvailable(context)){
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Other Cities",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp
                    )
                }
            }else{
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offline Saved Data",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Display weather data for cities
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weatherData.toList()) { (city, response) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(response.main.temp - 273.15).toInt()}°C",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = city,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "Country: ${response.sys.country}")
                            Text(text = "Weather: ${response.weather.firstOrNull()?.description}")
                            Text(text = response.wind.toString())
                            Row {
                                val sunrise = convertUnixToTime(response.sys.sunrise)
                                val sunset = convertUnixToTime(response.sys.sunset)
                                Text(text = "Sunrise: $sunrise am ")
                                Text(text = " Sunset: $sunset pm")
                            }
                        }
                    }
                }
            }


            if (errorMessage.isNotEmpty()) {
                Text(text = "Error: $errorMessage")
            }
        }
    }
}
