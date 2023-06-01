package com.aaron.runningmeter.detailScreen

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aaron.runningmeter.R
import com.aaron.runningmeter.databinding.DetailScreenFragmentBinding
import com.aaron.runningmeter.models.Route
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.io.OutputStream
import java.math.RoundingMode
import java.text.DecimalFormat

class DetailScreenFragment(val route: Route) : Fragment(), OnMapReadyCallback {

    lateinit var binder: DetailScreenFragmentBinding
    private lateinit var viewModel: DetailScreenViewModelInterface
    private lateinit var gMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = DetailScreenFragmentBinding.inflate(inflater)
        route.distance = route.distance?.div(1000)
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.CEILING
        route.distance = df.format(route.distance).toDouble()
        binder.apply {
            route = this@DetailScreenFragment.route
            lifecycleOwner = viewLifecycleOwner
        }
        return binder.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this)[DetailScreenViewModel::class.java]
        val mapView = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapView.getMapAsync(this)
        val hours: Int = (route.time ?: 1) / 3600
        var reminder = (route.time ?: 1) % 3600
        val minutes = reminder / 60
        reminder %= 60
        binder.txtTime.text = getString(R.string.timeData, getTimeStamp())
        //binder.txtTime.text = getString(R.string.timeData,"$hours:$minutes:$reminder")
    }

    private fun getTimeStamp(): String {
        val hours: Int = (route.time)?.div(3600) ?: 0
        var reminder = (route.time)?.rem(3600)
        val minutes = reminder?.div(60)
        reminder = reminder?.rem(60)
        return "${String.format("%02d",hours)}:${String.format("%02d",minutes)}:${String.format("%02d",reminder)}"
    }

    private fun shareImage(image: Bitmap) {
        val icon: Bitmap = image
        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "route")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        activity?.let { activity ->
            val outstream: OutputStream?
            activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )?.let { uri ->
                try {
                    outstream = activity.contentResolver.openOutputStream(uri)
                    icon.compress(Bitmap.CompressFormat.JPEG, 100, outstream)
                    outstream?.close()
                } catch (e: Exception) {
                    System.err.println(e.toString())
                }

                share.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(share, getString(R.string.shareImage)))
            }
        }
    }

    private fun loadViewModel() {
        viewModel.load(route) {
            viewModel.paintRoute(gMap)
            binder.btnDelete.setOnClickListener {
                viewModel.deleteRoute {
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
            binder.btnShare.setOnClickListener {
                binder.loader.visibility = View.VISIBLE
                binder.btnShare.isEnabled = false
                gMap.snapshot { image ->
                    image?.let {
                        shareImage(image)
                    }
                    binder.loader.visibility = View.GONE
                    binder.btnShare.isEnabled = true
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
        loadViewModel()
    }

}
