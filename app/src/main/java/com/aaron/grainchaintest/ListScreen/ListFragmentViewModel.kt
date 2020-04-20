package com.aaron.grainchaintest.ListScreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aaron.grainchaintest.models.GCTestDB
import com.aaron.grainchaintest.models.Route
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
    lateinit var db: GCTestDB
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    override var routes: LiveData<List<Route>>? = null

    override fun load(completion: () -> Unit) {
        db = GCTestDB.getAppDataBase(myApp.applicationContext)
        uiScope.launch {
            getRoutes()
            completion()
        }
    }

    suspend fun getRoutes() {
        routes = db.routeDao().getAllRoutes()
    }

}
