package ro.cluj.totemz.screens.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ro.cluj.totemz.BaseFragment
import ro.cluj.totemz.BasePresenter
import ro.cluj.totemz.R
import ro.cluj.totemz.model.FragmentTypes
import ro.cluj.totemz.model.FriendLocation
import ro.cluj.totemz.model.MyLocation
import ro.cluj.totemz.screens.camera.CameraPresenter
import ro.cluj.totemz.screens.camera.CameraView
import ro.cluj.totemz.screens.camera.FragmentCamera
import ro.cluj.totemz.utils.createAndAddMarker
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by sorin on 11.10.16.
 *
 * Copyright (c) 2016 moovel GmbH<br>
 *
 * All rights reserved<br>
<p></p>
 */
class FragmentMap : BaseFragment(), PermissionListener, OnMapReadyCallback,
        GoogleMap.OnCameraMoveListener,
        LocationSource.OnLocationChangedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, CameraView {

    var googleMap: GoogleMap? = null
    lateinit var mapView: MapView
    lateinit var googleApiClient: GoogleApiClient
    var isMapReady = false

    // Map properties
    val DEFAULT_ZOOM = 13f
    private val disposables = CompositeDisposable()
    lateinit var presenter: CameraPresenter
    val TAG = FragmentCamera::class.java.simpleName

    companion object {
        fun newInstance(): FragmentMap {
            val fragment = FragmentMap()
            return fragment
        }
    }

    override fun getFragType(): FragmentTypes {
        return FragmentTypes.FRAG_MAP
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        context?.let {
            if (context is Activity) {
                disposables.add(rxBus.toObservable()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { o ->
                            when (o) {
                                is FriendLocation -> googleMap?.let {
                                    googleMap?.createAndAddMarker(o.location, R.mipmap.ic_totem)
                                }
                                else -> {
                                }
                            }
                        })
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.frag_map, container, false)
        presenter = CameraPresenter()
        mapView = view.findViewById(R.id.map_totemz) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        Dexter.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        //init google API client
        googleApiClient = GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()


        try {
            MapsInitializer.initialize(activity.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mapView.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


    // Permission request callback
    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        response?.let {
            if (response.permissionName == Manifest.permission.ACCESS_FINE_LOCATION) {
                googleMap?.let {
                    it.isMyLocationEnabled = true
                    it.uiSettings.isMyLocationButtonEnabled = true
                    getLocationAndAnimateMarker()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            Timber.i("Map Ready")
            isMapReady = true
            this.googleMap = googleMap
        }
    }

    override fun onStart() {
        super.onStart()
        googleApiClient.connect()
    }

    override fun onStop() {
        super.onStop()
        googleApiClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        mapView.onDestroy()
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            googleMap?.clear()
            val lat = location.latitude
            val lng = location.longitude
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), DEFAULT_ZOOM))
            googleMap?.createAndAddMarker(LatLng(lat, lng), R.mipmap.ic_totem)
        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        getLocationAndAnimateMarker()
    }


    override fun onCameraMove() {
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    private fun getLocationAndAnimateMarker() {
        val location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        location?.let {
            val lat = location.latitude
            val lng = location.longitude
            rxBus.send(MyLocation(LatLng(lat, lng)))
            googleMap?.createAndAddMarker(LatLng(lat, lng), R.mipmap.ic_totem)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), DEFAULT_ZOOM))
            val subInterval = Observable.interval(6000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe {
                rxBus.send(MyLocation(LatLng(lat, lng)))
            }
            disposables.add(subInterval)
        }
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
    }

    override fun getPresenter(): BasePresenter<*> {
        return presenter
    }

}