package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

// TODO (STEP 2: Create the activities package and utils package and add the MainActivity class to it and create the constant object in utils.)\
// START
object Constants {

    // TODO (STEP 3: Add a function to check the network connection is available or not.)
    /**
     * Verifica se o aparelho está conectado a internet.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network      = connectivityManager.activeNetwork ?: return false
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}
// END