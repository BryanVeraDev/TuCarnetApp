package com.example.tucarnetapp.ui.home

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tucarnetapp.R
import com.example.tucarnetapp.ui.home.fragment.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        bottomNav = findViewById(R.id.bottomNavigationView)

        val sectionToOpen = intent.getStringExtra("open_section")

        when (sectionToOpen){
            "profile" -> {
                replaceFragment(HomeFragment())
                bottomNav.selectedItemId = R.id.nav_home
            }
            "carnet" ->{

            }
            else -> {
                replaceFragment(HomeFragment())
                bottomNav.selectedItemId = R.id.nav_home
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId){
                R.id.nav_home -> replaceFragment(HomeFragment())
            }
            true

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}