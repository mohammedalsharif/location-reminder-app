package com.udacity.project4.utils

import android.os.Build
import androidx.annotation.RequiresApi

object Constants {
    //map settings
    const val  ZOOM_LEVEL = 15f

    //checking if the api is 29 or higher
    val RUNNING_Q_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    //checking if the api is 31 or higher
    //to handle the pending intent flag
    val RUNNING_S_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    //checking if the api is 33 or higher
    //used to know when to request notification permission
    //since from api 33 notification permission is runtime permission
    val RUNNING_TIRAMISU_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU


    //foreground permission location
    const val FOREGROUND_LOCATION_REQUEST = 1
    const val FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION
    const val COARSE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_COARSE_LOCATION

    //background permission
    const val REQUEST_CHECK_SETTINGS = 1111
    const val BACKGROUND_LOCATION_REQUEST = 2
    const val BACKGROUND_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

    //notification permission
    const val NOTIFICATION_REQUEST = 2
    const val NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS

    //location tracking constants
    const val MIN_TIME_UPDATE = 60 * 1000L
    const val MIN_DISTANCE_UPDATE = 0f


    //geofence constants
    const val GEOFENCE_RADIUS = 500f // in meters

}