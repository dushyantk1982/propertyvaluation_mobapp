package com.propval.propertyvaluation

import Adapter.ImageAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class Dashboard : AppCompatActivity() {

    private var userid: String? = null
    private var token: String? = null
    private var name: String? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var viewPager: ViewPager2
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val images = listOf(R.drawable.propertyvaluation_a, R.drawable.propertyvaluation_b) // Replace with your image resource IDs
    private lateinit var glsearchInput: TextInputEditText
    private lateinit var title: MaterialToolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = findViewById(R.id.toolbar)
        userid = intent.getStringExtra("userid")
        token = intent.getStringExtra("token")
        name = intent.getStringExtra("name")
        if (name !=null)
            title.setTitle(name)
        progressBar = findViewById(R.id.dashboardProgressBar)

        // Fetch report status when the activity is created
        fetchReportStatus()
        viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = ImageAdapter(images)
        viewPager.adapter = adapter
        // Set up auto-slide
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager.currentItem
                val nextItem = if (currentItem == images.size - 1) 0 else currentItem + 1

                viewPager.setCurrentItem(nextItem, true)
                Log.d("next", nextItem.toString())
                // Post the runnable again to create a loop
                startAutoSlide()
//                handler.postDelayed(this, 3000) // Adjust time as needed
            }
        }

        // Existing code...
        val overflowMenu: ImageButton = findViewById(R.id.overflow_menu)
        overflowMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.menu_buttons, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_exit_app -> {
                        finishAffinity()
                        true
                    }

                    R.id.action_engineer_profile -> {
                        val intent = Intent(this, Profile::class.java)
                        startActivity(intent)
                        true
                    }

                    R.id.action_logout -> {
                        logout()
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }
        glsearchInput = findViewById(R.id.glsearchinput)
        var previousText: String = ""
        glsearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s.toString()
               }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Call your method here
//                navigateToSearchEngineerJob(glsearchInput.text.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
//                glsearchInput.setText("")
                if (s.toString().length > previousText.length) {
                    navigateToSearchEngineerJob(glsearchInput.text.toString())
                }
            }
        })
    

//        glsearchInput.setOnKeyListener { _, _, _ ->
//            navigateToSearchEngineerJob(glsearchInput.text.toString())
////            filterJobs(searchInput.text.toString())
//            false
//        }
//        glsearchInput.setOnKeyListener { _, keyCode, event ->
//            if (event.action == KeyEvent.ACTION_UP || keyCode == KeyEvent.ACTION_DOWN) {
//                navigateToSearchEngineerJob(glsearchInput.text.toString())
//                true
//            } else {
//                false
//            }
//        }
//      var  searchEditText : MaterialCardView = findViewById(R.id.searchGlobal)
//
//        // Setting up the click listener for the TextInputEditText
//        searchEditText.setOnClickListener {
//          navigateToSearchEngineerJob()
//        }

        val cardEngineerJob = findViewById<CardView>(R.id.card_engineer_job)
        cardEngineerJob.setOnClickListener {
            navigateToEngineerJob()
        }

        val inProgressJobs = findViewById<CardView>(R.id.inProgress)
        inProgressJobs.setOnClickListener {
            navigateToInProgressEngineerJob()
        }

        val onHoldJobs = findViewById<CardView>(R.id.holdJobs)
        onHoldJobs.setOnClickListener {
            navigateToHoldEngineerJob()
        }

        val completedEngineerJobs = findViewById<MaterialCardView>(R.id.completedJobCard)
        completedEngineerJobs.setOnClickListener {
            val intent = Intent(this, CompletedJobs::class.java)
            startActivity(intent)
        }

        val pendingEngineerJobs = findViewById<MaterialCardView>(R.id.pendingReports)
        pendingEngineerJobs.setOnClickListener {
           navigateToPendingEngineerJob()
        }
    }
    private fun startAutoSlide() {
        handler.postDelayed(runnable, 3000) // Change images every 3 seconds
    }


    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable) // Stop auto-slide when paused
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Clean up callbacks
    }

    override fun onResume() {
        super.onResume()
        startAutoSlide()

        // Fetch the report status again when the activity resumes
        fetchReportStatus()

    }



    private fun fetchReportStatus() {
        // Show ProgressBar before starting the API call
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val result = callReportStatusAPI()
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after the API call completes
                progressBar.visibility = View.GONE
                handleReportStatusResult(result)
            }
        }
    }

    private suspend fun callReportStatusAPI(): String? {
        return try {
            val url = URL(Constants.getUrl("api/engineer/$userid/engreportstatus/"))
//            val url = URL("http://10.0.2.2:8000/api/engineer/$userid/engreportstatus/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleReportStatusResult(result: String?) {
        if (result != null) {
            try {
                val jsonObject = JSONObject(result)
                val totalReports = jsonObject.getInt("tot")
                val completedReports = jsonObject.getInt("com")
                val onHoldReports = jsonObject.getInt("hold")
                val pendingReports = jsonObject.getInt("pend")
                val inProgressReports = jsonObject.getInt("inprog")

                // Update the TextViews with the fetched data
                findViewById<TextView>(R.id.total_reports).text = totalReports.toString()
                findViewById<TextView>(R.id.completed_reports).text = completedReports.toString()
                findViewById<TextView>(R.id.on_hold_reports).text = onHoldReports.toString()
                findViewById<TextView>(R.id.pending_reports).text = pendingReports.toString()
                findViewById<TextView>(R.id.in_progressCount).text = inProgressReports.toString()

                // Print the result for debugging
                println("Total Reports: $totalReports, Completed Reports: $completedReports, On Hold Reports: $onHoldReports, Pending Reports: $pendingReports, In Progress Reports: $inProgressReports")
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to parse report status", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to fetch report status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToEngineerJob() {
        val intent = Intent(this, EngineerJob::class.java).apply {
            putExtra("userid", userid)
            putExtra("token", token)
        }
        startActivity(intent)
    }



    private fun navigateToInProgressEngineerJob() {
        val intent = Intent(this, EngineerJob::class.java).apply {
            putExtra("userid", userid)
            putExtra("token", token)
            putExtra("jobStatus", "InProgress")
        }
        startActivity(intent)
    }

// add one button inprogress in total jobs
    private fun navigateToPendingEngineerJob() {
        val intent = Intent(this, EngineerJob::class.java).apply {
            putExtra("userid", userid)
            putExtra("token", token)
            putExtra("jobStatus", "null")
            putExtra("jobStatusExtra", "Pending")
        }
        startActivity(intent)
    }

    private fun navigateToSearchEngineerJob(txt: String) {
        Log.d("txt",txt)
        val intent = Intent(this, EngineerJob::class.java).apply {
            putExtra("userid", userid)
            putExtra("token", token)
            putExtra("IsSearch", "Search")
            putExtra("searchTxt", txt)
        }
        startActivity(intent)
    }


    private fun navigateToHoldEngineerJob() {
        val intent = Intent(this, EngineerJob::class.java).apply {
            putExtra("userid", userid)
            putExtra("token", token)
            putExtra("jobStatus", "Hold")
        }
        startActivity(intent)
    }

    private fun logout() {
        progressBar.visibility = View.VISIBLE // Show ProgressBar before logout

        CoroutineScope(Dispatchers.IO).launch {
            val result = callLogoutAPI()
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE // Hide ProgressBar after logout
                handleLogoutResult(result)
            }
        }
    }

    private suspend fun callLogoutAPI(): Boolean {
        return try {
            val url = URL(Constants.getUrl("api/logoutapi/"))
//            val url = URL("http://10.0.2.2:8000/api/logoutapi/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun handleLogoutResult(success: Boolean) {
        if (success) {
            // Clear the login token from SharedPreferences
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                remove("token")
                remove("userid")
                apply()
            }

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show()
        }
    }
}
