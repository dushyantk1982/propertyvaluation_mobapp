package com.propval.propertyvaluation

import Adapter.EngineerJobAdapter
import Class.EngineerJobData
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL

class EngineerJob : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EngineerJobAdapter
    private val dataList = mutableListOf<EngineerJobData>()
    private lateinit var searchInput: TextInputEditText
    private var jobStatus: String? = null
    private var jobStatusExtra: String? = null
    private var IsSearch: String? = null
    private lateinit var progressBar: View

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engineer_job_recycler_view)

        val userid = intent.getStringExtra("userid")
        val token = intent.getStringExtra("token")
        jobStatus = intent.getStringExtra("jobStatus") // Get job status from intent
        jobStatusExtra = intent.getStringExtra("jobStatusExtra")
        IsSearch = intent.getStringExtra("IsSearch")
        val searchTxt = intent.getStringExtra("searchTxt")
        searchInput = findViewById(R.id.search_input)
        Log.d("txt",searchTxt.toString())
        if(IsSearch =="Search"){
        val engineerJobsTitle : TextView = findViewById(R.id.engineerJobsTitle)
           engineerJobsTitle.setText("Search Jobs")
            searchInput.setText(searchTxt.toString())
            searchInput.requestFocus()
        }

        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        progressBar = findViewById(R.id.engineerJobProgressBar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = EngineerJobAdapter(dataList,IsSearch)
        recyclerView.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Call your method here
                filterJobs(searchInput.text.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
//                searchInput.setText("")
            }
        })
//        searchInput.setOnKeyListener { _, _, _ ->
//            filterJobs(searchInput.text.toString())
//            false
//        }

        // Show ProgressBar before fetching data
        progressBar.visibility = View.VISIBLE


        CoroutineScope(Dispatchers.IO).launch {
            // Hide ProgressBar after data fetch is complete
            progressBar.visibility = View.GONE
            val result = fetchData(userid, token,IsSearch)
            withContext(Dispatchers.Main) {
                handleFetchResult(result)
            }
        }
    }

    private suspend fun fetchData(userid: String?, token: String?, IsSearch: String?): String {
        var url: URL? = null
        return try {
            if(IsSearch =="Search") {
                 url = URL(Constants.getUrl("api/reception/$userid/totalengineerjob/"))
            }else{
                 url = URL(Constants.getUrl("api/reception/$userid/engineerjob/"))
            }
//            val url = URL("http://10.0.2.2:8000/api/reception/$userid/engineerjob/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Accept", "application/json")

            val result = connection.inputStream.bufferedReader().use { it.readText() }
            println(result)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    private fun handleFetchResult(result: String) {
        try {
            val jsonArray = JSONArray(result)
            updateRecyclerView(jsonArray)
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
        }
    }

    // visiting person wali id
    private fun updateRecyclerView(data: JSONArray) {
        dataList.clear()
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val engstatus = item.getString("engineer")
            val engStatusUpdate = if (engstatus=="Submitted") "Completed" else if (engstatus=="InProgress") "Pending" else engstatus
            Log.d("stst",engStatusUpdate)
            val npacase = if(item.getBoolean("npa")) "NPA" else "normal"
            val prioritycase = if(item.getBoolean("priority")) "High" else "normal"
            val engineerJob = EngineerJobData(
                url = item.getString("url"),
                applicationDate = item.getString("applicationdate"),
                applicationNumber = item.getString("applicationnumber"),
                name = item.getString("name"),
                bankName = item.getString("bankname"),
                bankId = item.getString("bankid"),
                bankVertical = item.getString("bankvertical"),
                address1 = item.getString("add1"),
                address2 = item.getString("add2"),
                city = item.getString("city"),
                region = item.getString("region"),
                zip = item.getString("zip"),
                country = item.getString("country"),
                npa = item.getBoolean("npa"),
                partcase = item.getBoolean("partcase"),
                phoneNumber = item.getString("phonenumber"),
                visitingPerson = item.getInt("visitingperson"),
                reportPerson = item.getInt("reportperson"),
                visitingPersonName = item.getString("visitingpersonname"),
                reportPersonName = item.getString("reportpersonname"),
                engineer = engStatusUpdate,
                reporter = item.optString("reporter", null),
                priority = item.getBoolean("priority"),
                dateCreated = item.getString("datecreated"),
                updatedAt = item.getString("updated_at"),
                userId = item.getString("visitingperson")
            )
            Log.d("stat",engineerJob.engineer+"00")
            // Only add jobs that match the desired status
            if (jobStatus == null || engineerJob.engineer.equals(jobStatus, ignoreCase = true ) || engineerJob.engineer.equals(jobStatusExtra,ignoreCase = true)) {
                dataList.add(engineerJob)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun filterJobs(query: String) {
        val filteredList = dataList.filter { job ->
            job.name.contains(query, ignoreCase = true) ||
                    job.applicationNumber.contains(query, ignoreCase = true) ||
                    job.bankName.contains(query, ignoreCase = true) ||
                    job.phoneNumber.contains(query, ignoreCase = true) ||
                    job.engineer.contains(query, ignoreCase = true) ||
                    job.address2.contains(query, ignoreCase = true) ||
                    job.region.contains(query, ignoreCase = true) ||
                    job.applicationDate.contains(query, ignoreCase = true)
        }
        adapter = EngineerJobAdapter(filteredList, IsSearch)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }
}
