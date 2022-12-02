package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Localização de latitude e longetude
    private lateinit var mFusedLocationClient : FusedLocationProviderClient
    private var mProgressDialog : Dialog? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // https://api.openweathermap.org/data/2.5/weather?lat=37.4219983&lon=-122.084&appid=fc3b90ce0a6ba52735abbc56fee4c26b

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Sua localização está desligada",
                Toast.LENGTH_SHORT
            ).show()

            // direciona para configurações de localização
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)

        } else {

            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report!!.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "Você não permitiu a localização",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationaleDialogForPermission()
                    }

                }).onSameThread().check()

        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        /*val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY*/

        val mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            //.setMinUpdateIntervalMillis(5000)
            //.setMaxUpdateDelayMillis(10000)
            .setMaxUpdates(1) // chama a requisição apenas uma vezf
            .build()


        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallBack,
            Looper.myLooper()
        )
    }

    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult : LocationResult) {
            val mLastLocation : Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")

            // TODO (STEP 7: Call the api calling function here.)
            if (latitude != null && longitude != null) {
                getLocationWeatherDetails(latitude, longitude)
            }
        }
    }

    private fun showRationaleDialogForPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permissão")
            .setMessage("Você deve conceder permissão para o App acessar sua localização")
            .setPositiveButton("Configurações") {_, _ ->
                try {

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancelar") {dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Verifica se a localização está ligada
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // TODO (STEP 5: Create a function to make an api call using Retrofit Network Library.)
    // START
    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails(latitude : Double, longitude : Double){

        // TODO (STEP 6: Here we will check whether the internet
        //  connection is available or not using the method which
        //  we have created in the Constants object.)
        // START
        if (Constants.isNetworkAvailable(this@MainActivity)) {

            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService = retrofit.create(WeatherService::class.java)

            val listCall : Call<WeatherResponse> = service.getWeather(
                latitude,
                longitude,
                Constants.METRIC_UNIT,
                Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object  : Callback<WeatherResponse>{
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {

                    if (response!!.isSuccess) {

                        hideProgressDialog()

                        val weatherList : WeatherResponse = response.body()
                        setupUi(weatherList)

                        Log.i("Response Result", weatherList.name)

                    } else {

                        val rc = response.code()
                        when(rc) {
                            400 -> {
                                Log.e("Error 400", "Bad connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic error")
                            }
                        }

                    }

                }

                override fun onFailure(t: Throwable?) {
                    hideProgressDialog()
                    Log.e("Errorrr", t!!.message.toString())
                }

            })

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
        // END
    }
    // END

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUi(weatherList : WeatherResponse) {

        for (i in weatherList.weather.indices) {
            binding.tvMain.text = weatherList.weather[i].main
            binding.tvMainDescription.text = weatherList.weather[i].description
            binding.tvTemp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise)
            binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset)
            binding.tvMin.text = weatherList.main.temp_min.toString()
            binding.tvMax.text = weatherList.main.temp_max.toString()
            binding.tvSpeed.text = weatherList.wind.speed.toString()
            binding.tvSpeedUnit.text = weatherList.wind.deg.toString()
            binding.tvName.text = weatherList.name
            binding.tvCountry.text = weatherList.sys.country

            Log.i("Weather name", weatherList.weather.toString())
        }

    }

    private fun getUnit(value : String) : String? {
        var valor = "ºC"
        if ("US" == value || "LR" == value || "MM" == value){
            valor = "ºF"
        }
        return valor
    }

    private fun unixTime(timex: Long) : String? {
        Log.i("hora", timex.toString())
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

}