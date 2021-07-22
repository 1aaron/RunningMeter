package com.aaron.runningmeter.ListScreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.aaron.runningmeter.R
import com.aaron.runningmeter.adapters.ListAdapter
import com.aaron.runningmeter.databinding.ListFragmentBinding
import com.aaron.runningmeter.detailScreen.DetailScreenFragment
import com.aaron.runningmeter.models.Route

class ListFragment : Fragment() {
    private lateinit var viewModel: ListFragmentViewModelInterface
    private lateinit var binder: ListFragmentBinding
    var recyclerAdapter: ListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = ListFragmentBinding.inflate(inflater)
        return binder.root
    }

    private var routeClickListener: (Route) -> Unit = { route: Route ->
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.content_main,DetailScreenFragment(route))
            ?.addToBackStack(null)
            ?.commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ListFragmentViewModel::class.java)
        binder.routesList.setHasFixedSize(true)
        binder.routesList.layoutManager = LinearLayoutManager(context)
        viewModel.load {
            viewModel.routes?.observe(viewLifecycleOwner, { value ->
                if (value.isNotEmpty()) {
                    binder.emptyView.visibility = View.GONE
                    binder.routesList.visibility = View.VISIBLE
                } else {
                    binder.emptyView.visibility = View.VISIBLE
                    binder.routesList.visibility = View.GONE
                }
                recyclerAdapter = ListAdapter(value,routeClickListener)
                binder.routesList.adapter = recyclerAdapter
                recyclerAdapter?.notifyDataSetChanged()
            })
        }
    }

}
