package com.example.android.politicalpreparedness.representative

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.android.politicalpreparedness.BuildConfig
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.repository.ElectionRepository
import com.example.android.politicalpreparedness.representative.adapter.RepresentativeListAdapter
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.util.*

class DetailFragment : Fragment() {

    private lateinit var binding: FragmentRepresentativeBinding

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        const val REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 35
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

    private lateinit var representativeViewModel: RepresentativeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application
        val database = ElectionDatabase.getInstance(requireContext())
        val repository = ElectionRepository(database)

        val representativeViewModelFactory = RepresentativeViewModelFactory(repository, application)
        representativeViewModel =
            ViewModelProvider(this, representativeViewModelFactory).get(RepresentativeViewModel::class.java)

        binding = FragmentRepresentativeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = representativeViewModel
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val representativeListAdapter = RepresentativeListAdapter()
        binding.representativesRecycler.adapter = representativeListAdapter

        representativeViewModel.representatives.observe(viewLifecycleOwner) { representativesList ->
            representativesList?.let {
                representativeListAdapter.submitList(representativesList)
                binding.noDataHintGroup.visibility = View.GONE
            }
        }

        binding.state.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                representativeViewModel.address.value?.state = binding.state.selectedItem as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                representativeViewModel.address.value?.state = binding.state.selectedItem as String
            }

        }

        representativeViewModel.errorOnFetchingNetworkData.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    activity,
                    R.string.network_error,
                    Toast.LENGTH_LONG
                ).show()
                binding.loadingRepresentativesImage.visibility = View.GONE
                representativeViewModel.displayNetworkErrorComplete()
            }
        }

        representativeViewModel.representativesFetched.observe(viewLifecycleOwner) {
            if (it == false) {
                binding.noDataHintGroup.visibility = View.GONE
                binding.loadingRepresentativesImage.visibility = View.VISIBLE
            } else {
                binding.loadingRepresentativesImage.visibility = View.GONE
            }
        }

        binding.buttonSearch.setOnClickListener {
            findMyRepresentatives()
        }

        binding.buttonLocation.setOnClickListener {
            getLocationAndFindMyRepresentatives()
        }

        return binding.root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE &&
                    grantResults[0] == PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
            ).setAction(R.string.settings) {
                // Displays App settings screen.
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else if (grantResults.isEmpty() ||
            grantResults[0] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE &&
                    grantResults[0] == PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
            ).setAction(R.string.settings) {
                // Displays App settings screen.
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            checkDeviceLocationSettings()
        }
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        if (foregroundAndBackgroundLocationPermissionGranted()) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {

                    try {
                        startIntentSenderForResult(
                            exception.resolution.intentSender,
                            REQUEST_TURN_DEVICE_LOCATION_ON,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Timber.d("Error getting location settings resolution: %s", sendEx.message)
                    }
                } else {
                    Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        R.string.location_required_error, Snackbar.LENGTH_LONG
                    ).setAction(android.R.string.ok) {
                        checkDeviceLocationSettings()
                    }.show()
                }
            }

            locationSettingsResponseTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    getLocation()
                }
            }
        } else {
            checkPermissionsAndGetRepresentatives()
        }
    }

    private fun getLocationAndFindMyRepresentatives() {
        hideKeyboard()

        checkPermissionsAndGetRepresentatives()
    }

    private fun findMyRepresentatives() {
        hideKeyboard()

        representativeViewModel.onFindMyRepresentativesClicked()
    }

    private fun checkPermissionsAndGetRepresentatives() {
        if (foregroundAndBackgroundLocationPermissionGranted()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionGranted(): Boolean {
        return foregroundLocationPermissionGranted() && backgroundLocationPermissionGranted()
    }

    private fun foregroundLocationPermissionGranted(): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    @TargetApi(29)
    private fun backgroundLocationPermissionGranted(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
    }

    @TargetApi(29)
    fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionGranted()) {
            return
        }

        requestForegroundLocationPermission()
        requestBackgroundLocationPermission()
    }

    private fun requestForegroundLocationPermission() {
        if (foregroundLocationPermissionGranted()) {
            return
        }

        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    @TargetApi(29)
    private fun requestBackgroundLocationPermission() {
        if (backgroundLocationPermissionGranted()) {
            return
        }

        val permissionsArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        val resultCode = REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE

        if (foregroundLocationPermissionGranted()) {
            if (runningQOrLater) {
                requestPermissions(
                    permissionsArray,
                    resultCode
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val address = geoCodeLocation(location)
                representativeViewModel.onUseMyLocationClicked(address)
            }
        }
    }

    private fun geoCodeLocation(location: Location): Address {
        val geocoder = Geocoder(context, Locale.getDefault())
        return geocoder.getFromLocation(location.latitude, location.longitude, 1)
            .map { address ->
                Address(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.locality,
                    address.adminArea,
                    address.postalCode
                )
            }
            .first()
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}