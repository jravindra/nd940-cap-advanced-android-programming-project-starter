package com.example.android.politicalpreparedness.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.text.SimpleDateFormat
import java.util.*

fun isNetworkReachable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val activeNetwork = connectivityManager.getNetworkCapabilities((network))

    return when {
        activeNetwork!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }

}


fun Date.toSimpleString(): String {
    val format = SimpleDateFormat("EEE dd MMM yyyy", Locale.US)
    return format.format(this)
}