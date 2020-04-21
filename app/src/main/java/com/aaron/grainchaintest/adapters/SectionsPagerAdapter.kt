package com.aaron.grainchaintest.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.aaron.grainchaintest.R
import com.aaron.grainchaintest.mapScreen.MapFragment
import com.aaron.grainchaintest.secondViewBase.BaseFragment

private val TAB_TITLES = arrayOf(
        R.string.map_tab_text,
        R.string.list_tab_text
)

class SectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    private val tabAmount = 2
    override fun getItem(position: Int): Fragment {
        val fragment = if (position == 0) {
            MapFragment.getInstance()
        } else {
            BaseFragment.getInstance()
        }
        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return tabAmount
    }
}