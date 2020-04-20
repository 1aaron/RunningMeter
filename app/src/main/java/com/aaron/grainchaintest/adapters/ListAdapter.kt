package com.aaron.grainchaintest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aaron.grainchaintest.databinding.RouteItemBinding
import com.aaron.grainchaintest.models.Route

class ListAdapter(val routes: List<Route>, val listener: (Route) -> Unit): RecyclerView.Adapter<ListAdapter.ListAdapterViewHolder>() {

    lateinit var itemBinder: RouteItemBinding

    override fun getItemCount() = routes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        itemBinder = RouteItemBinding.inflate(inflater)
        return ListAdapterViewHolder(itemBinder.root)
    }

    override fun onBindViewHolder(holder: ListAdapterViewHolder, position: Int) {
        val route = routes[position]
        holder.txtRoute.text = route.alias
        val hours: Int = route.time ?: 1 / 3600
        var reminder = route.time ?: 1 % 3600
        val minutes = reminder / 60
        reminder %= 60
        holder.txtTime.text = "$hours:$minutes:$reminder"
    }

    inner class ListAdapterViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val txtRoute = itemBinder.txtRoute
        val txtTime = itemBinder.txtTime
    }
}