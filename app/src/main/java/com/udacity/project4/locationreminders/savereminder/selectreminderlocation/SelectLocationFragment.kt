package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.view.*
import org.koin.android.ext.android.inject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

class SelectLocationFragment : BaseFragment(),
    OnMapReadyCallback,
    EasyPermissions.PermissionCallbacks,
    LocationListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var locationManager: LocationManager

    private var latLng: LatLng? = null
    private var address = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)

        setDisplayHomeAsUpEnabled(true)

        hideSaveButton()

        setupMapView(savedInstanceState)

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.btnSaveReminder.setOnClickListener {
            onLocationSelected()
        }


        return binding.root
    }

    private fun setupMapView(savedInstanceState: Bundle?){
        val mapView = childFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment

        with(mapView){
            onCreate(savedInstanceState)
            getMapAsync(this@SelectLocationFragment)
        }
    }


    private fun onLocationSelected() {
        //         send back the selected location details to the view model
        _viewModel.latitude.value = latLng?.latitude
        _viewModel.longitude.value = latLng?.longitude
        _viewModel.reminderSelectedLocationStr.value= address
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.navigationCommand.value =
            NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        Log.i("TAG", "onMapReady:  ")

        map = googleMap

        setPoiClick(map)

        setMapStyle(map)


        setMapLongClick(map)


        //request background location permission
        //getBackgroundPermission()

        //get current location
        currentLocation()

        //handle my location button

        map.setOnMyLocationButtonClickListener {
            handleCurrentLocationButton(requireContext())
            false
        }
    }

    private fun handleCurrentLocationButton(context: Context){
        if (isLocationServiceEnabled()){
            currentLocation()
        }else{
            enableLocationService()
        }
    }


    private fun isLocationServiceEnabled(): Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun setMapLongClick(map: GoogleMap){
        map.setOnMapLongClickListener {it
            map.clear()
            latLng = it
            setPosition(it)
            showSaveButton()
        }
    }

    private fun hideSaveButton(){
        with(binding.btnSaveReminder){
            translationY = 250f //move the button down the screen
            alpha = 0f //make the button invisible
        }
    }

    private fun showSaveButton(){
        with(binding.btnSaveReminder){
            animate()
                .translationY(0f) //move the button back to its original place
                .alpha(1f) //make the button visible
                .setDuration(400)
                .start()
        }
    }



/*    private fun hasBackgroundLocationPermission() =
        if(Constants.RUNNING_Q_OR_LATER){
            EasyPermissions.hasPermissions(requireContext(),Constants.BACKGROUND_LOCATION_PERMISSION)
        }else{
            true
        }*/


    private fun hasForegroundLocationPermission() =
        EasyPermissions.hasPermissions(
            requireContext(),
            Constants.FINE_LOCATION_PERMISSION,
            Constants.COARSE_LOCATION_PERMISSION
        )

    private fun requestForegroundLocationPermission(){
        EasyPermissions.requestPermissions(
            this,
            "You need to grant foreground location permission in order to select the location.",
            Constants.FOREGROUND_LOCATION_REQUEST,
            Constants.FINE_LOCATION_PERMISSION,
            Constants.COARSE_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
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
        }
    }

    @SuppressLint("MissingPermission")
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when(requestCode){
            Constants.FOREGROUND_LOCATION_REQUEST ->{
                map.uiSettings.isMyLocationButtonEnabled = true
                map.isMyLocationEnabled = true
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude,location.longitude)
        moveCamera(latLng)
    }

    @SuppressLint("MissingPermission")
    private fun currentLocation() {
        if (hasForegroundLocationPermission()){
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                Constants.MIN_TIME_UPDATE,
                Constants.MIN_DISTANCE_UPDATE,
                this)
            map.uiSettings.isMyLocationButtonEnabled = true
            map.isMyLocationEnabled = true
        }else{
            requestForegroundLocationPermission()
        }
    }

    private fun getAddress(latLng: LatLng): String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        //if the province is null it will return unknown province
        val province = try {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.get(0)?.adminArea
        }catch (e: Exception){
            "Unknown province"
        }

        //if the city is null it will return unknown city
        val city = try {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.get(0)?.locality
        } catch (e: Exception){
            "Unknown city"
        }

        return "$province - $city"
    }

    private fun addMarker(latLng: LatLng) {

        val snippet = String.format(
            Locale.getDefault(),
            getString(R.string.lat_long_snippet),
            latLng.latitude,
            latLng.longitude
        )
        address = getAddress(latLng)
        with(map){
            addMarker(
                MarkerOptions().position(latLng).title(address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet(snippet))?.showInfoWindow()
        }
    }

    private fun setPoiClick(map: GoogleMap){
        map.setOnPoiClickListener { poi ->
            map.clear()
            latLng = poi.latLng
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                poi.latLng.latitude,
                poi.latLng.longitude
            )
            address = poi.name
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .snippet(snippet)
            )
            addCircle(poi.latLng)
            moveCamera(poi.latLng)
            showSaveButton()
            poiMarker?.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap){
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(),
                    R.raw.map_style)
            )

            if (!success){
                Log.i("STYLE", "style failed :(")
            }
        }catch (e: Resources.NotFoundException){
            Log.i("STYLE", "can't find style : $e")
        }
    }

    private fun moveCamera(latLng: LatLng){
        with(map){
            animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,Constants.ZOOM_LEVEL))
        }
    }

    private fun setPosition(latLng: LatLng){
        addMarker(latLng)
        moveCamera(latLng)
        addCircle(latLng)
    }

    private fun addCircle(latLng: LatLng){
        // Add a colored circle overlay to the geofence
        val geofenceRadius = 500 // in meters
        val circleOptions = CircleOptions()
            .center(latLng)
            .radius(geofenceRadius.toDouble())
            .fillColor(Color.argb(50, 102, 204, 0)) // set fill color
            .strokeColor(Color.GREEN) // set stroke color
            .strokeWidth(2f) // set stroke width
        map.addCircle(circleOptions)
    }


    private fun enableLocationService(){
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.create())
        val client = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // we start a location tracking service
            // but will do nothing for now
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        Constants.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    /**
     * Theses methods are overridden to prevent app from crashing
     * on lower APIs 30 or Lower
     */
    override fun onProviderEnabled(provider: String) {
        //super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        //super.onProviderDisabled(provider)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //super.onStatusChanged(provider, status, extras)
    }

}