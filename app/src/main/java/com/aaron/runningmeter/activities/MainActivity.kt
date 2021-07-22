package com.aaron.runningmeter.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.aaron.runningmeter.R
import com.aaron.runningmeter.adapters.SectionsPagerAdapter
import com.aaron.runningmeter.databinding.ActivityMainBinding
import com.aaron.runningmeter.services.LocationService
import com.google.android.gms.ads.MobileAds
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        MobileAds.initialize(this) {
            Log.e("ADS","initialize")
        }
        val sectionsPagerAdapter =
            SectionsPagerAdapter(
                this,
                supportFragmentManager
            )
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
    }

    override fun onBackPressed() {
        if (LocationService.isAttached){
            Toast.makeText(this,getString(R.string.in_record),Toast.LENGTH_LONG).show()
        } else {
            super.onBackPressed()
        }
    }
}