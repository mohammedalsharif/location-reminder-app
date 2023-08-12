package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*


class SaveReminderFragment : BaseFragment(), EasyPermissions.PermissionCallbacks{
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val geofenceId = UUID.randomUUID().toString()


            reminderDataItem = ReminderDataItem(
                title = title,
                description = description,
                location = location,
                latitude = latitude,
                longitude = longitude,
                id = geofenceId
            )

            validatePermissionsAndSave()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }


    private fun hasBackgroundLocationPermission() =
        if(Constants.RUNNING_Q_OR_LATER){
            EasyPermissions.hasPermissions(requireContext(),Constants.BACKGROUND_LOCATION_PERMISSION)
        }else{
            true
        }

    private fun hasForegroundLocationPermission() =
        EasyPermissions.hasPermissions(
            requireContext(),
            Constants.FINE_LOCATION_PERMISSION,
            Constants.COARSE_LOCATION_PERMISSION
        )


    private fun requestBackgroundLocationPermission(){
        EasyPermissions.requestPermissions(
            this,
            "You need to grant background location permission in order to select the location.",
            Constants.BACKGROUND_LOCATION_REQUEST,
            Constants.BACKGROUND_LOCATION_PERMISSION
        )
    }
    private fun requestForegroundLocationPermission(){
        EasyPermissions.requestPermissions(
            this,
            "You need to grant foreground location permission in order to select the location.",
            Constants.FOREGROUND_LOCATION_REQUEST,
            Constants.FINE_LOCATION_PERMISSION,
            Constants.COARSE_LOCATION_PERMISSION
        )
    }


    private fun getPermissions(){
        if(!hasForegroundLocationPermission()){
            requestForegroundLocationPermission()
        }
        if(!hasBackgroundLocationPermission()){
            requestBackgroundLocationPermission()
        }
    }

    private fun hasAllPermissions(): Boolean{
        return hasBackgroundLocationPermission() && hasForegroundLocationPermission()
    }

    private fun validatePermissionsAndSave(){
        if (_viewModel.validateEnteredData(reminderDataItem)) {
            if (hasAllPermissions()) {
                if(isLocationServiceEnabled()){
                    validateAndStartGeofencing()
                }else{
                    showDialogForGPS()
                }
            } else {
                getPermissions()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.i("TAG", "onPermissionsGranted: ")
        validatePermissionsAndSave()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        when(requestCode){
            Constants.FOREGROUND_LOCATION_REQUEST -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
                    AppSettingsDialog.Builder(this).build().show()
                }else{
                    requestForegroundLocationPermission()
                }
            }
            Constants.BACKGROUND_LOCATION_REQUEST -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
                    AppSettingsDialog.Builder(this).build().show()
                }else{
                    //requestBackgroundLocationPermission()
                }
            }
        }
    }



/*    private fun enableLocationService(){
        if (!isLocationServiceEnabled()){
            showDialog()
        }
    }*/

    private fun isLocationServiceEnabled(): Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    private fun showDialogForGPS(){
        AlertDialog.Builder(context)
            .setTitle("Location Services Disabled")
            .setMessage(getString(R.string.location_required_error))
            .setIcon(R.drawable.gps_off)
            .setPositiveButton("Enable") { dialog, which ->
                enableLocationService()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableLocationService(){
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.create())
        val client = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // Add reminder to the database
            //validatePermissionsAndSave()
            _viewModel.validateAndSaveReminder(reminderDataItem)
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        Constants.REQUEST_CHECK_SETTINGS, //the request code to identify the result
                        null, //an optional intent to send with the request
                        0, //the flags for the intent sender
                        0, //the flags for the intent
                        0, //the extra flags for the intent
                        null //an optional bundle of additional data to include with the intent.
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }else{
                Log.i("test", "Location settings not enabled: ${exception.message}")
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun validateAndStartGeofencing(){
        val latLng = LatLng(reminderDataItem.latitude!!, reminderDataItem.longitude!!)
        // Create geofence object
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(latLng.latitude,latLng.longitude,Constants.GEOFENCE_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Create geofencing request object
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Create pending intent for geofence events
        val intent = Intent(requireActivity().applicationContext,GeofenceBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(requireActivity().applicationContext,
            0,
            intent,
            if (Constants.RUNNING_S_OR_LATER){
                PendingIntent.FLAG_MUTABLE
            }
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )


        geofencingClient.addGeofences(geofencingRequest,pendingIntent).run {
            addOnSuccessListener {
                Log.i("geofence", "Added reminder successfully")
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                _viewModel.showToast.value = getString(R.string.geofences_not_added)
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("test", "onActivityResult: ")
        if (requestCode == Constants.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // Do something here, such as start a location tracking service
                validateAndStartGeofencing()
            } else {
                // The user did not enable the location service
            }
        }
    }


}
