package com.aaron.runningmeter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aaron.runningmeter.R
import com.aaron.runningmeter.models.Route

class ListAdapter(val routes: List<Route>, val listener: (Route) -> Unit): RecyclerView.Adapter<ListAdapter.ListAdapterViewHolder>() {

    override fun getItemCount() = routes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ListAdapterViewHolder(inflater.inflate(R.layout.route_item,parent,false))
    }

    override fun onBindViewHolder(holder: ListAdapterViewHolder, position: Int) {
        val route = routes[position]
        holder.txtRoute.text = route.alias
        val hours: Int = (route.time ?: 1) / 3600
        var reminder = (route.time ?: 1) % 3600
        val minutes = reminder / 60
        reminder %= 60
        holder.txtTime.text = "${String.format("%02d",hours)}:${String.format("%02d",minutes)}:${String.format("%02d",reminder)}"
        holder.txtDate.text = route.date
        holder.view.setOnClickListener {
            listener(route)
        }
    }

    inner class ListAdapterViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val txtRoute: TextView = view.findViewById(R.id.txtDistance)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
    }
}