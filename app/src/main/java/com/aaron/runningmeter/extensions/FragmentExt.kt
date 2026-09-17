package com.aaron.runningmeter.extensions

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.aaron.runningmeter.databinding.LocationPermissionScreenBinding

fun Context.showLocationPermissionDialog(layoutInflater: LayoutInflater, acceptAction: () -> Unit) {
    val dialogView = LocationPermissionScreenBinding.inflate(layoutInflater, null, false)
    val dialog = AlertDialog.Builder(this).apply {
        setView(dialogView.root)
    }.create()
    dialogView.acceptButton.setOnClickListener {
        acceptAction()
        dialog.dismiss()
    }
    dialog.show()
}