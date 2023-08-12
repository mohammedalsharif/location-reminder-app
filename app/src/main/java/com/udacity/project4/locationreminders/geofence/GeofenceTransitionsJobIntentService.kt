package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GeofenceTransition
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                Log.e("Geofence", "Geofencing error: ${geofencingEvent.errorCode}")
                return
            }
        }

        // Get the geofence transition type
        val geofenceTransition = geofencingEvent?.geofenceTransition

        // send a notification to the user when he enters the geofence area
        if (
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
            ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
        ){
            val geofences= geofencingEvent.triggeringGeofences as List<Geofence>
            Log.i("notification", geofences[0].latitude.toString())

            sendNotification(geofences)
        }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        Log.i("notification", "send notification")
        //Receiving the geofences to send the notification
        val requestIds: List<String> = triggeringGeofences.map { it.requestId }

        //Looping through the list of geofences to notify the user
        requestIds.forEach { requestId ->
            //Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
//        Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                Log.i("notification", "result.title.toString()")
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    Log.i("notification", reminderDTO.title.toString())
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }

    }

}
