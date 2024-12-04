package com.example.closeup

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng

class MenuActivity : AppCompatActivity() {

    private lateinit var btnAbrirCamara: Button
    private lateinit var imgCapturada: ImageView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "streets-v2"

        val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"
        Mapbox.getInstance(this)

//        //FIXME: Inicia la actividad con ciertos requisitos, buscar requisitos y documentacion del mapa, probar con un target
        setContentView(R.layout.activity_menu)

        //Puntero al boton
        //btnAbrirCamara = findViewById(R.id.btn_open_camera)

        //Puntero al contenedor
        mapView = findViewById(R.id.mapView)

        // FIXME: Crashea al no poder guardar la imagen
        //TODO: Almacenar imagen con su ubicacion en firebase storage

        // imgCapturada = findViewById(R.id.imgCapturada)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl){
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(-33.4489,-70.6693))
                    .zoom(10.0)
                    .build()
            }
        }

//        mapView.getMapboxMap().loadStyleUri(
//            Style.MAPTILER_STREETS
//        ) {
//            getCurrentLocation { latitude, longitude ->
//                val point = Point.fromLngLat(longitude, latitude)
//                mapView.getMapboxMap().setCamera(
//                    CameraOptions.Builder().center(point).zoom(14.0).build()
//                )
//                addMarker(point)
//            }
//        }


//        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
//            if(result.resultCode == RESULT_OK){
//                val imageBitmap = result.data?.extras?.get("data")as Bitmap
//                imgCapturada.setImageBitmap(imageBitmap)
//            }
//        }

//        btnAbrirCamara.setOnClickListener {
//            abrirCamara()
//        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun abrirCamara(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intent.resolveActivity(packageManager) != null){
            takePictureLauncher.launch(intent)
        }
    }
}