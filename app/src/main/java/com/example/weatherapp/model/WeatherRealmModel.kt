package com.example.weatherapp.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import java.util.*

open class WeatherRealmModel(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var cityName: String = "",
    var temperature: Int = 0,
    var description: String = "",
    var country: String = "",
    var windSpeed: String = "",
    var sunrise: String = "",
    var sunset: String = "",
    var lastUpdated: Long = System.currentTimeMillis() // Timestamp of the last update
) : RealmObject {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        cityName = "",
        temperature = 0,
        description = "",
        country = "",
        windSpeed = "",
        sunrise = "",
        sunset = "",
        lastUpdated = System.currentTimeMillis()
    )
}

