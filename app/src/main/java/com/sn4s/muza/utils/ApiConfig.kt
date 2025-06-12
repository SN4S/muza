package com.sn4s.muza.utils

import com.sn4s.muza.BuildConfig

object ApiConfig {
    val BASE_URL: String
        get() = if (BuildConfig.DEBUG) {
            "http://192.168.88.188:8000/" //  localhost
        } else {
            "https://your-production-api.com/" // Replace with your actual production URL
        }
}