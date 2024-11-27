package com.example.weatherapp.model

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.copyFromRealm
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RealmHelper {

    val realmConfiguration = RealmConfiguration
        .Builder(schema = setOf(WeatherRealmModel::class))
        .name("WeatherDb")
        .build()

    // Save weather data to Realm
    suspend fun saveWeatherDataToRealm(
        cityName: String,
        temperature: Int,
        description: String,
        country: String,
        windSpeed: String,
        sunrise: String,
        sunset: String,
        lastUpdated: Long = System.currentTimeMillis()
    ) {
        withContext(Dispatchers.IO) {
            val realm = Realm.open(realmConfiguration)
            realm.write {
                val weather = WeatherRealmModel().apply {
                    this.cityName = cityName
                    this.temperature = temperature
                    this.description = description
                    this.country = country
                    this.windSpeed = windSpeed
                    this.sunrise = sunrise
                    this.sunset = sunset
                    this.lastUpdated = System.currentTimeMillis() // Save the timestamp
                }
                copyToRealm(weather)
            }
        }
    }

    suspend fun loadWeatherDataFromRealm(): Map<String, WeatherResponse> {
        return withContext(Dispatchers.IO) {
            val realm = Realm.open(realmConfiguration)
            val weatherResults: RealmResults<WeatherRealmModel> = realm.query(WeatherRealmModel::class)
                .sort("lastUpdated", Sort.DESCENDING)
                .find()

            // Convert WeatherRealmModel to WeatherResponse and return as a Map
            weatherResults.associateBy({ it.cityName }) { it.toWeatherResponse() }
        }
    }

    fun WeatherRealmModel.toWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            coord = Coord(lon = 0.0, lat = 0.0),
            weather = listOf(
                Weather(
                    id = 0,
                    main = this.description,
                    description = this.description,
                    icon = ""
                )
            ),
            base = "",
            main = Main(
                temp = this.temperature.toDouble(),
                feels_like = 0.0,
                temp_min = 0.0,
                temp_max = 0.0,
                pressure = 0,
                humidity = 0,
                sea_level = 0,
                grnd_level = 0
            ),
            visibility = 0,
            wind = Wind(speed = this.windSpeed.toDouble(), deg = 0),
            clouds = Clouds(all = 0),
            dt = System.currentTimeMillis() / 1000,
            sys = Sys(type = 0, id = 0, country = this.country, sunrise = 0, sunset = 0),
            timezone = 0,
            id = 0,
            name = this.cityName,
            cod = 200
        )
    }


}



