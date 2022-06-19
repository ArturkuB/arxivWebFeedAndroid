package com.example.pracalicencjacka.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.Utils
import com.example.pracalicencjacka.parseWorker
import com.example.pracalicencjacka.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var mainActivityViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
//        checkInternetConnection()
        mainActivityViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        mainActivityViewModel.setRealm()
        setNavigationGraph()
        val prefs: SharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val firstStart: Boolean = prefs.getBoolean("firstStart", true)

        val bar: ProgressBar = findViewById(R.id.progress)
        val container: FragmentContainerView = findViewById(R.id.nav_host_fragment)
        if (Utils().isOnline(this)) {
            if (firstStart) {
                val prefsEdit: SharedPreferences.Editor = prefs.edit()
                prefsEdit.putString("selectorTitleDefault", ".list-title")
                prefsEdit.putString("selectorAuthorsDefault", ".list-authors")
                prefsEdit.putString("selectorPdfLinkDefault", ".list-identifier a:nth-child(2)")
                prefsEdit.putString("selectorTitle", ".list-title")
                prefsEdit.putString("selectorAuthors", ".list-authors")
                prefsEdit.putString("selectorPdfLink", ".list-identifier a:nth-child(2)")
                prefsEdit.putBoolean("firstStart", false)
                prefsEdit.apply()
                try {
                    mainActivityViewModel.parseMainSite()
                    mainActivityViewModel.loadingState.observe(this) {
                        bar.isVisible = it
                        container.isVisible = !it
                    }
                    prefsEdit.putBoolean("firstStart", false)
                    prefsEdit.apply()
                    Log.i("firststart", "parsing")
                }
                catch (e : Exception){
                    prefsEdit.putBoolean("firstStart", true)
                    prefsEdit.apply()
                    Toast.makeText(this, "Timeout, Parsing error", Toast.LENGTH_LONG).show()
                }
            } else {
                lifecycleScope.launch(Main) {
                    mainActivityViewModel.queryAndSet()
                }
            }
        } else {
            Log.i("NO", "INTERNET")
            lifecycleScope.launch(Main) {
                mainActivityViewModel.queryAndSet()
            }
        }
    }

    private fun setNavigationGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        mainActivityViewModel.countSubscribedSubcategories {
                if (it > 0) {
                    navGraph.setStartDestination(R.id.subscribedFieldsFragment)
                } else {
                    navGraph.setStartDestination(R.id.chooseFieldFragment)
                }
            navController.graph = navGraph
        }
    }

    override fun onPause() {
        Thread.sleep(10000)
        val myWorkRequest =
            PeriodicWorkRequest.Builder(parseWorker::class.java, 15, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "backgroundTask",
            ExistingPeriodicWorkPolicy.KEEP,
            myWorkRequest
        )
        super.onPause()
    }
}