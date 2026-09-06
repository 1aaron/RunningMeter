package com.aaron.runningmeter.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.aaron.runningmeter.R
import com.aaron.runningmeter.adapters.SectionsPagerAdapter
import com.aaron.runningmeter.databinding.ActivityMainBinding
import com.aaron.runningmeter.services.LocationService
import com.google.android.gms.ads.MobileAds
import com.google.android.material.tabs.TabLayout
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. SOLUCIÓN DEFINITIVA EDGE-TO-EDGE:
        // Esta es la API recomendada para manejar el comportamiento de insets de manera moderna.
        WindowCompat.enableEdgeToEdge(window)
        
        setTheme(R.style.AppTheme_NoActionBar)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        // 2. APLICAR PADDING CORRECTAMENTE:
        // Usamos WindowInsetsCompat.Type.systemBars() para obtener dimensiones exactas de la barra.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        setContentView(view)

        // Inicialización de Ads y UI
        MobileAds.initialize(this) {
            // Log.e("ADS","initialize") // Descomentado si es necesario
        }
        
        val sectionsPagerAdapter = SectionsPagerAdapter(
            this,
            supportFragmentManager
        )
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
    }

    // 3. LÓGICA DE SEGURIDAD:
    // Previene que el usuario salga si la localización está activa.
    override fun onBackPressed() {
        if (LocationService.isAttached) {
            Toast.makeText(this, R.string.in_record, Toast.LENGTH_LONG).show()
        } else {
            super.onBackPressed()
        }
    }
}