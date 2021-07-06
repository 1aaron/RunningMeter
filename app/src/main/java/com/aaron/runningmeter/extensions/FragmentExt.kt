package com.aaron.runningmeter.extensions

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.aaron.runningmeter.databinding.LocationPermissionScreenBinding

fun Fragment.showLocationPermissionDialog(acceptAction: () -> Unit) {
    val dialogView = LocationPermissionScreenBinding.inflate(layoutInflater,null,false)
    val dialog = AlertDialog.Builder(requireContext()).apply {
        setView(dialogView.root)
    }.create()
    dialogView.acceptButton.setOnClickListener {
        acceptAction()
        dialog.dismiss()
    }
    dialog.show()
}