package com.aaron.grainchaintest.mapScreen

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.aaron.grainchaintest.utils.Globals

class MapFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private var locations = arrayListOf<Location>()
    private val myApp = application
    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: $it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    fun reviewPermissions(): Boolean {
        var permissionsAccepted = true
        var permissionsToCheck = Globals.PERMISSIONS_TO_ASK.copyOf()
        if (Build.VERSION.SDK_INT < 29)
            permissionsToCheck = permissionsToCheck.dropLast(1).toTypedArray()
        if (Build.VERSION.SDK_INT < 28)
            permissionsToCheck = permissionsToCheck.dropLast(1).toTypedArray()
        for (permission in permissionsToCheck) {
            if (ContextCompat.checkSelfPermission(myApp, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsAccepted = false
                return permissionsAccepted
            }
        }
        return permissionsAccepted
    }
}