package com.aaron.grainchaintest.ListScreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel

interface ListFragmentViewModelInterface {

}

class ListFragmentViewModel(application: Application) : AndroidViewModel(application), ListFragmentViewModelInterface {
    val myApp = application

}
