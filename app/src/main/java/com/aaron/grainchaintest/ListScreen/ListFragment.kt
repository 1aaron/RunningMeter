package com.aaron.grainchaintest.ListScreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.aaron.grainchaintest.databinding.ListFragmentBinding

class ListFragment : Fragment() {

    companion object {
        private var INSTANCE: ListFragment? = null

        @JvmStatic
        fun getInstance(): ListFragment {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: ListFragment()
                            .also { INSTANCE = it }
                }
        }
    }

    private lateinit var viewModel: ListFragmentViewModelInterface
    private lateinit var binder: ListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = ListFragmentBinding.inflate(inflater)
        return binder.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ListFragmentViewModel::class.java)
        binder.routesList.setHasFixedSize(true)
        binder.routesList.layoutManager = LinearLayoutManager(context)
    }

}
