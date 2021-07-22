package com.aaron.runningmeter.secondViewBase

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aaron.runningmeter.ListScreen.ListFragment
import com.aaron.runningmeter.R
import com.aaron.runningmeter.databinding.FragmentBaseBinding

class BaseFragment : Fragment() {
    private lateinit var binder: FragmentBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.content_main, ListFragment())
            ?.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binder = FragmentBaseBinding.inflate(inflater)
        return binder.root
    }
}
