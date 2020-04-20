package com.aaron.grainchaintest.detailScreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.aaron.grainchaintest.R
import com.aaron.grainchaintest.databinding.DetailScreenFragmentBinding
import com.aaron.grainchaintest.models.Route
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class DetailScreenFragment(val route: Route) : Fragment(), OnMapReadyCallback {

    lateinit var binder: DetailScreenFragmentBinding
    private lateinit var viewModel: DetailScreenViewModelInterface
    private lateinit var gMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = DetailScreenFragmentBinding.inflate(inflater)
        return binder.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(DetailScreenViewModel::class.java)
        val mapView = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapView.getMapAsync(this)
        binder.txtDistance.text = "Distance: ${route.distance?.div(1000)}"//TODO: convert to kms
        binder.txtRoute.text = "Route: ${route.alias}"
        val hours: Int = (route.time ?: 1) / 3600
        var reminder = (route.time ?: 1) % 3600
        val minutes = reminder / 60
        reminder %= 60
        binder.txtTime.text = "Time: $hours:$minutes:$reminder"
        viewModel.load(route) {
            viewModel.paintRoute(gMap)
            binder.btnDelete.setOnClickListener {
                viewModel.deleteRoute {
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map
    }

}
