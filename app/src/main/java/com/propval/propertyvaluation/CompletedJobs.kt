package com.propval.propertyvaluation

import Class.CompletedJobData
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.propval.propertyvaluation.Adapter.CompletedJobAdapter
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CompletedJobs : AppCompatActivity() {

    private lateinit var completedJobs: List<CompletedJobData>
    private lateinit var adapter: CompletedJobAdapter
    private lateinit var searchInput: TextInputEditText
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_jobs)

        progressBar = findViewById(R.id.completedJobsProgressBar)
        searchInput = findViewById(R.id.search_input)

        fetchCompletedJobs()

        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Set up the search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterJobs(s.toString())
            }
        })
    }

    private fun fetchCompletedJobs() {
        // Show ProgressBar before starting the data fetch
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            completedJobs = getCompletedJobs() ?: emptyList()
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after data is fetched
                progressBar.visibility = View.GONE
                setupRecyclerView(completedJobs)
            }
        }
    }

    private fun setupRecyclerView(jobs: List<CompletedJobData>) {
        val recyclerView: RecyclerView = findViewById(R.id.completedJobsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CompletedJobAdapter(jobs)
        recyclerView.adapter = adapter
    }

    private fun filterJobs(query: String) {
        val filteredJobs = completedJobs.filter { job ->
            job.name.contains(query, ignoreCase = true) ||
                    job.applicationNumber.contains(query, ignoreCase = true) ||
                    job.bankName.contains(query, ignoreCase = true) ||
                    job.city.contains(query, ignoreCase = true)
        }
        adapter.updateData(filteredJobs)
    }

    private suspend fun getCompletedJobs(): List<CompletedJobData>? {
        return try {
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            val userId = sharedPreferences.getString("userid", null)
            val url = URL(Constants.getUrl("api/engineer/$userId/engineercomjobapi/"))
//            val url = URL("http://10.0.2.2:8000/api/engineer/$userId/engineercomjobapi")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $token")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().use { it.readText() }

                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("data")
                val completedJobs = mutableListOf<CompletedJobData>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val job = CompletedJobData(
                        jsonObject.getString("applicationnumber"),
                        jsonObject.getString("name"),
                        jsonObject.getString("bankname"),
                        jsonObject.getString("city"),
                        jsonObject.getString("url")
                    )
                    completedJobs.add(job)
                }
                completedJobs
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
