package com.aaron.runningmeter.ListScreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.aaron.runningmeter.models.GCTestDB
import com.aaron.runningmeter.models.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface ListFragmentViewModelInterface {
    var routes: LiveData<List<Route>>?
    fun load(completion: () -> Unit)
}

class ListFragmentViewModel(application: Application) : AndroidViewModel(application), ListFragmentViewModelInterface {
    val myApp = application
    var db: GCTestDB = GCTestDB.getAppDataBase(application)
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    override var routes: LiveData<List<Route>>? = db.routeDao().getAllRoutes()

    override fun load(completion: () -> Unit) {
        uiScope.launch {
            completion()
        }
    }

    fun getRoutes() {
        routes = db.routeDao().getAllRoutes()
    }
}
