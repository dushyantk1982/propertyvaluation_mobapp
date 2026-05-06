package com.propval.propertyvaluation

import Class.EngineerJobData
import Constants
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.propval.propertyvaluation.databinding.ActivitySubmitReportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.Spinner
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import java.io.DataOutputStream
import java.lang.Integer.parseInt
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay

class SubmitReport : AppCompatActivity() {
    private lateinit var scrollView: ScrollView
    private lateinit var linearLayout: LinearLayout
    private lateinit var floorlinearLayout :LinearLayout
    private lateinit var occupantlinearLayout :LinearLayout
    private lateinit var binding: ActivitySubmitReportBinding
    private var files = mutableListOf<Uri>()
    private val selectedFiles = mutableListOf<Uri>()
    private val siteFormFiles = mutableListOf<Uri>()
    private val electricMeterFiles = mutableListOf<Uri>()
    private val electricBillFiles = mutableListOf<Uri>()
    private val khasraFiles = mutableListOf<Uri>()
    private val sitePhotosFiles = mutableListOf<Uri>()
    private val siteReferenceFiles = mutableListOf<Uri>()
    private var token: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var progressBar: View
    private val bankNames = mutableListOf<String>()
    private val occupantsname = mutableListOf<TextInputEditText>()
    private val floornames = mutableListOf<TextInputEditText>()
    private val floordetails = mutableListOf<TextInputEditText>()
    private val floorareas = mutableListOf<TextInputEditText>()
    private val dynamicTextSet = mutableSetOf<Pair<String,TextInputEditText>>()
    private val dynamicCheckbox = mutableListOf<CheckBox>()
    private val dynamicCheckboxSet = mutableSetOf<Pair<String, CheckBox>>()
    //    private val mapCkeckbox = mutableMapOf<String, MutableList<String>>()
//    private val mapCkeckbox: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val dynamicSelect = mutableListOf<Pair<Spinner, Spinner?>>()
//    private val dynamicSelectSet = mutableSetOf<Pair<String, String>>()
//    private val dynamicSubSelectSet = mutableSetOf<Pair<String, String>>()
//    private val selectedValues = mutableListOf<String>()
    private val dynamicSubSelect = mutableListOf<Spinner?>()
    private var spinnerValue: String? = null
    private var subspinnerValue: String=""
    var attendanceaddress = "Unknown"
    var attendanceCity  = "Unknown"
    var attendancezip = "Unknown"
    var attendanceregion ="Unknown"
    var attendancecountry = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scrollView = findViewById(R.id.engScroll) // Make sure to set the correct ID
        linearLayout = findViewById(R.id.dynamicFields) // Make sure to set the correct ID
        progressBar = findViewById(R.id.editReportProgressBar) // Initialize the ProgressBar
        floorlinearLayout = findViewById(R.id.floorlayout)
        occupantlinearLayout = findViewById(R.id.occupantLayout)

        CoroutineScope(Dispatchers.IO).launch {
            // Hide ProgressBar after data fetch is complete
//            progressBar.visibility = View.GONE
            val dynResult = fetchDynamicFields()
            withContext(Dispatchers.Main) {
                Log.d("result = " , dynResult)
                handleDynFetchResult(dynResult)
            }
        }


        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
        val ocAddButton: Button = findViewById(R.id.addOccupant)
        ocAddButton.setOnClickListener {
            addOccupants()
        }
        val ocspinner: Spinner = findViewById(R.id.occupancy)
        val spitems = arrayOf("Single Occupancy", "Multiple Occupancy")
        val spadapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spitems)
        ocspinner.adapter = spadapter
        ocspinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                val ocselectedItem = parent.getItemAtPosition(position).toString()
                  ocAddButton.visibility = if (ocAddButton.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
//                Toast.makeText(this@SubmitReport, "Selected: $ocselectedItem", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        val addfloorbtn:Button= findViewById(R.id.addfloor)
        addfloorbtn.setOnClickListener {
            addFloorfields()
        }
        binding.bankName.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                fetchBankNamesAndShowDialog()
            }
            false // Return false to allow normal behavior (e.g., showing the keyboard).
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get data from intent
        val engineerJobData = intent.getSerializableExtra("engineerJobData") as? EngineerJobData
        token = intent.getStringExtra("token")
        // Prefill the form with received data
//        val casetypetextbox = findViewById<TextInputEditText>(R.id.caseType)
        engineerJobData?.let {
            binding.applicationNumber.setText(it.applicationNumber)
            binding.name.setText(it.name)
            binding.bankName.setText(it.bankName)
            binding.bankId.setText(it.bankId)
            binding.add1.setText(it.address1+"," + it.address2+"," + it.city+"," + it.region+"," + it.zip)
            binding.add2.setText(it.address2)
            binding.city.setText(it.city)
            binding.region.setText(it.region)
            binding.zip.setText(it.zip)
            binding.country.setText(it.country)
            binding.caseType.setText(if (it.npa) "NPA" else "Normal")
            binding.phoneNumber.setText(it.phoneNumber)
            binding.visitingPersonName.setText(it.visitingPersonName)
            binding.reportPersonName.setText(it.reportPersonName)
            binding.engineer.setText(it.engineer)
            binding.reporter.setText(it.reporter)
            binding.priority.isChecked = it.priority
            binding.receptionId.setText(it.url)
            binding.userId.setText(it.userId)
        }

        // Add fields for ReportData
        binding.visitInPresence.setText("") // Prefill if available
//        binding.caseType.setText("") // Prefill if available
        binding.east.setText("") // Prefill if available
        binding.west.setText("") // Prefill if available
        binding.north.setText("") // Prefill if available
        binding.south.setText("") // Prefill if available
        binding.gfArea.setText("") // Prefill if available
        binding.ffArea.setText("") // Prefill if available
        binding.sfArea.setText("") // Prefill if available
        binding.tfArea.setText("") // Prefill if available
        binding.propertyAge.setText("") // Prefill if available
        binding.landRate.setText("") // Prefill if available
        binding.occupant.setText("") // Prefill if available
        binding.rented.setText("") // Prefill if available
        binding.landmark.setText("") // Prefill if available
        binding.roadWidth.setText("") // Prefill if available
        binding.highTensionLine.isChecked = false // Prefill if available
        binding.railwayLine.isChecked = false // Prefill if available
        binding.nala.isChecked = false // Prefill if available
        binding.river.isChecked = false // Prefill if available
        binding.pahad.isChecked = false // Prefill if available
        binding.roadComesUnderRoadBinding.isChecked = false // Prefill if available
        binding.propertyAccessIssue.isChecked = false // Prefill if available
        binding.otherCheck.isChecked = false // Prefill if available
        binding.others.setText("") // Prefill if available
        binding.remark.setText("") // Prefill if available

        CoroutineScope(Dispatchers.IO).launch {
            val location = getCurrentLocation()
            if (location != null) {
                currentLocation = location
                val applicationNo = findViewById<TextView>(R.id.applicationNumber).text.toString()
                val receptionUrl = findViewById<TextView>(R.id.receptionId).text.toString()
                val userId = findViewById<TextView>(R.id.userId).text.toString()
                val userDetailsUrl = Constants.getUrl("api/user/${userId}/")
                submitAttendanceData(applicationNo, receptionUrl, userDetailsUrl)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubmitReport, "Failed to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }


//
//        CoroutineScope(Dispatchers.IO).launch {
//        val applicationNo = findViewById<TextView>(R.id.applicationNumber).text.toString()
//        val receptionUrl = findViewById<TextView>(R.id.receptionId).text.toString()
//        val userId = findViewById<TextView>(R.id.userId).text.toString()
//        var userDetailsUrl =Constants.getUrl("api/user/${userId}/")
//
//            submitAttendanceData(applicationNo,receptionUrl, userDetailsUrl)
//        }


        val submitReportButton: Button = findViewById(R.id.submitReportButton)
        submitReportButton.setOnClickListener {
            if (validateFields()) {
                progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    val result = submitReport()
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE // Hide ProgressBar after submission
                        handleReportSubmissionResult(result)
                    }
                }
            }
        }

        val uploadButton: Button = findViewById(R.id.button_select_file)
        uploadButton.setOnClickListener {
            openFileChooser()
        }
        val siteFormUploadButton: Button = findViewById(R.id.btnUploadSiteForm)
        siteFormUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_SITE_FORM)
        }
        val electricityMeterUploadButton: Button = findViewById(R.id.btnUploadelectricityMeter)
        electricityMeterUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_ELEC_METER)
        }
        val electricityBillUploadButton: Button = findViewById(R.id.btnUploadelectricityBill)
        electricityBillUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_ELEC_BILL)
        }
        val khasraUploadButton: Button = findViewById(R.id.btnUploadKhasra)
        khasraUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_KHASRA)
        }
        val sitePhotosUploadButton: Button = findViewById(R.id.btnUploadSitephotos)
        sitePhotosUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_SITE_PHOTO)
        }
        val siteReferenceUploadButton: Button = findViewById(R.id.btnUploadReference)
        siteReferenceUploadButton.setOnClickListener {
            openFileChooserA(REQUEST_CODE_SITE_REFERENCE)
        }
    }

    private fun addOccupants() {
        val textInputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

            }
            hint = "Occupant Name"
            setHintEnabled(true)
            boxBackgroundColor = ContextCompat.getColor(
                this@SubmitReport,
                R.color.editbox
            )

            boxStrokeColor = ContextCompat.getColor(this@SubmitReport, R.color.border_color)
            boxStrokeWidthFocused = 4
            boxStrokeWidth = 4
//            elevation = 4f
            setPadding(0, 0, 0, 0) // Optional: setting padding
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE // Set the outlined box mode

        }
        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

            }
            setPadding(32,32,32,32)
            setBackground(null)
            inputType = InputType.TYPE_CLASS_TEXT
            gravity = Gravity.TOP or Gravity.START
            id = View.generateViewId()
        }
        textInputLayout.addView(editText)
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(40, 0, 0, 8)

            }
            addView(textInputLayout, 0, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        }
        val removeButton = ImageButton(this).apply {
            // Set an appropriate drawable or create a bitmap with "-"
            setImageResource(android.R.drawable.ic_delete) // Replace with your minus icon resource
            background = null  // Remove default button background
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 32, 0, 0) // Optional: add some margin to the left of the button
            }
            // Optionally, set minimum width and height to 0
            minimumWidth = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                occupantsname.remove(editText) //remove refrences
                // Remove the row from the parent layout
                (inputContainer.parent as? LinearLayout)?.removeView(inputContainer)
            }
        }
        inputContainer.addView(removeButton)
//        textInputLayout.addView(removeButton)
        occupantlinearLayout.addView(inputContainer)
        occupantsname.add(editText)
    }

    private fun addFloorfields() {
        val sublinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(40, 0, 0, 4)

            }
            setPadding(8, 8, 0, 8)
        }
        val currentFloorNames = mutableListOf<TextInputEditText>()
        val currentFloorDetails = mutableListOf<TextInputEditText>()
        val currentFloorAreas = mutableListOf<TextInputEditText>()
        // Creating a TextInputLayout
        for (i in 1..3) {
            val hnt = when (i) {
                1 -> "Floor Name"
                2 -> "Details"
                3 -> "Area"
                else -> ""
            }

            var wt = 1f
            var mg = 0
            if (i == 2) {
                wt =2f
                mg = 8
            }
            if (i == 3) {
                mg = 8
            }
            val textInputLayout = TextInputLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,wt
                ).apply {
                    setMargins(mg, 0, 8, 0)

                }
                hint = hnt
                setHintEnabled(true)
                boxBackgroundColor = ContextCompat.getColor(
                    this@SubmitReport,
                    R.color.editbox
                )

                boxStrokeColor = ContextCompat.getColor(this@SubmitReport, R.color.border_color)
                boxStrokeWidthFocused = 4
                boxStrokeWidth = 4
                setPadding(0, 0, 0, 0) // Optional: setting padding
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE // Set the outlined box mode

            }

            // Creating an EditText

            val editText = TextInputEditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(32,32,32,32)
                setBackground(null)
                inputType = InputType.TYPE_CLASS_TEXT
                gravity = Gravity.TOP or Gravity.START
                id = View.generateViewId()
            }
            // Adding EditText to TextInputLayout
            textInputLayout.addView(editText)
            sublinearLayout.addView(textInputLayout)
            if (i == 1 ){
                floornames.add(editText)
            }else if( i ==2){
                floordetails.add(editText)
            }else {
                floorareas.add(editText)
            }
            when (i) {
                1 -> currentFloorNames.add(editText)
                2 -> currentFloorDetails.add(editText)
                3 -> currentFloorAreas.add(editText)
            }
        }
        val removeButton = ImageButton(this).apply {
            // Set an appropriate drawable or create a bitmap with "-"
            setImageResource(android.R.drawable.ic_delete) // Replace with your minus icon resource
            background = null  // Remove default button background
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 4, 0) // Optional: add some margin to the left of the button
            }
            // Optionally, set minimum width and height to 0
            minimumWidth = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                // Remove the row from the parent layout
                floornames.removeAll(currentFloorNames)
                floordetails.removeAll(currentFloorDetails)
                floorareas.removeAll(currentFloorAreas)
                (sublinearLayout.parent as? LinearLayout)?.removeView(sublinearLayout)
            }
        }

        sublinearLayout.addView(removeButton)
        // Adding TextInputLayout to the LinearLayout
//        floorlinearLayout.addView(textInputLayout)
        floorlinearLayout.addView(sublinearLayout)
    }


    private fun fetchBankNamesAndShowDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(Constants.getUrl("/api/bank/"))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseAndShowBankNames(response)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SubmitReport,
                            "Failed to fetch bank names. Error code: $responseCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SubmitReport,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun parseAndShowBankNames(response: String) {
        try {
            val jsonArray = JSONArray(response)
            bankNames.clear()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.getString("name")
                bankNames.add(name)
            }
            withContext(Dispatchers.Main) {
                showBankSelectionDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubmitReport, "Error parsing data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBankSelectionDialog() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bankNames)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Bank")
            .setAdapter(adapter) { _, which ->
                val selectedBank = bankNames[which]
                binding.bankName.setText(selectedBank)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private suspend fun getCurrentLocation(): Location? = suspendCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            continuation.resume(null) // Permission not granted, return null
            return@suspendCoroutine
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                continuation.resume(location)
            } else {
                continuation.resume(null) // No location available
            }
        }.addOnFailureListener {
            continuation.resume(null) // Handle failure to get location
        }
    }

//
//    private fun getCurrentLocation() {
//        // Check location permissions
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
//            return
//        }
//
//        // Fetch last known location
//        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            currentLocation = location
//            if (location != null) {
//                // Display location or use it in the report data
//                Toast.makeText(this, "Location: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Unable to get last location", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    fun fetchAddressFromLatLng(lat: Double, lng: Double): Map<String, String> {
        val addressMap = mutableMapOf<String, String>()
        val apiKey = "YOUR_API_KEY" // Replace with your Google Maps API key
        val urlString = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey"

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                val resultsArray = jsonResponse.getJSONArray("results")
                if (resultsArray.length() > 0) {
                    val addressComponents = resultsArray.getJSONObject(0).getJSONArray("address_components")

                    for (i in 0 until addressComponents.length()) {
                        val component = addressComponents.getJSONObject(i)
                        val types = component.getJSONArray("types")

                        when {
                            types.toString().contains("route") -> addressMap["address"] = component.getString("long_name")
                            types.toString().contains("locality") -> addressMap["city"] = component.getString("long_name")
                            types.toString().contains("postal_code") -> addressMap["zip"] = component.getString("long_name")
                            types.toString().contains("administrative_area_level_1") -> addressMap["region"] = component.getString("long_name")
                            types.toString().contains("country") -> addressMap["country"] = component.getString("long_name")
                        }
                    }
                }
            } else {
                println("Failed to fetch address. Response Code: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Exception: ${e.message}")
        }

        return addressMap
    }


    private suspend fun submitAttendanceData(applicationNo: String,receptionUrl: String,userDetailsUrl: String) {
        // Ensure you have the current location
        if (currentLocation == null) {
            withContext(Dispatchers.Main) {
                //Toast.makeText(this@SubmitReport, "Location not available", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Step 1: Fetch address details using current latitude and longitude
        val lat = currentLocation!!.latitude
        val lng = currentLocation!!.longitude

//        val addressDetails = fetchAddressFromLatLng(lat, lng)
//        val attendanceAddress = addressDetails["address"] ?: "Unknown"
//        val attendanceCity = addressDetails["city"] ?: "Unknown"
//        val attendanceZip = addressDetails["zip"] ?: "Unknown"
//        val attendanceRegion = addressDetails["region"] ?: "Unknown"
//        val attendanceCountry = addressDetails["country"] ?: "Unknown"

        // Step 2: Prepare API request data
        val apiUrl = URL(Constants.getUrl("api/engattendance/"))
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubmitReport, "Authorization token missing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val jsonInput = JSONObject().apply {
            put("applicationnumber", applicationNo)
          //  put("address", attendanceAddress)
           // put("city", attendanceCity)
           // put("zip", attendanceZip)
          //  put("region", attendanceRegion)
          //  put("country", attendanceCountry)
            put("lat", lat.toString())
            put("lng", lng.toString())
            put("receptionid", receptionUrl)
            put("userdetailsid", userDetailsUrl)
        }

        // Step 3: Send data via HTTP POST
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "token $token")
                connection.doOutput = true

                // Write JSON payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonInput.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
//                    println("Attendance submitted successfully: $response")
                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@SubmitReport, "Attendance submitted successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    println("Error Response: $error")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SubmitReport, "Failed to Fetch Location: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubmitReport, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun updateOutAttendance(applicationNumber: String, receptionId: Int) {
        // Ensure you have the current location (if required for this functionality)
        if (currentLocation == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubmitReport, "Location not available", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Step 1: Get the latitude and longitude from the current location
       // val lat = currentLocation!!.latitude
      // val lng = currentLocation!!.longitude

        // Step 2: Prepare API request data
        val apiUrl = URL(Constants.getUrl("api/engattendance/updateout/"))

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubmitReport, "Authorization token missing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val jsonInput = JSONObject().apply {
            put("applicationnumber", applicationNumber)
            put("receptionid", receptionId)
//            put("lat", lat.toString())
//            put("lng", lng.toString())
        }

        // Step 3: Send data via HTTP PUT
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "token $token")
                connection.doOutput = true

                // Write JSON payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonInput.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    println("Attendance updated successfully: $response")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SubmitReport, "Attendance updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    println("Error Response: $error")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SubmitReport, "Error updating attendance: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubmitReport, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Launch a coroutine to call the suspend function
                CoroutineScope(Dispatchers.Main).launch {
                    val location = getCurrentLocation()
                    if (location != null) {
                        Toast.makeText(
                            this@SubmitReport,
                            "Location: ${location.latitude}, ${location.longitude}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this@SubmitReport, "Failed to fetch location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun validateFields(): Boolean {
        return when {
            binding.visitInPresence.text.isNullOrEmpty() -> {
                showToast("Please fill Visit In Presence field")
                false
            }
//            binding.caseType.text.isNullOrEmpty() -> {
//                showToast("Please fill Case Type field")
//                false
//            }
//            binding.east.text.isNullOrEmpty() -> {
//                showToast("Please fill East field")
//                false
//            }
//            binding.west.text.isNullOrEmpty() -> {
//                showToast("Please fill West field")
//                false
//            }
//            binding.north.text.isNullOrEmpty() -> {
//                showToast("Please fill North field")
//                false
//            }
//            binding.south.text.isNullOrEmpty() -> {
//                showToast("Please fill South field")
//                false
//            }
//            binding.gfArea.text.isNullOrEmpty() -> {
//                showToast("Please fill Ground Floor Area field")
//                false
//            }
//            binding.ffArea.text.isNullOrEmpty() -> {
//                showToast("Please fill First Floor Area field")
//                false
//            }
//            binding.sfArea.text.isNullOrEmpty() -> {
//                showToast("Please fill Second Floor Area field")
//                false
//            }
//            binding.tfArea.text.isNullOrEmpty() -> {
//                showToast("Please fill Third Floor Area field")
//                false
//            }
            binding.propertyAge.text.isNullOrEmpty() -> {
                showToast("Please fill Property Age field")
                false
            }
            binding.landRate.text.isNullOrEmpty() -> {
                showToast("Please fill Land Rate field")
                false
            }
//            binding.occupant.text.isNullOrEmpty() -> {
//                showToast("Please fill Occupant field")
//                false
//            }
//            binding.rented.text.isNullOrEmpty() -> {
//                showToast("Please fill Rented field")
//                false
//            }
            binding.landmark.text.isNullOrEmpty() -> {
                showToast("Please fill Landmark field")
                false
            }
            binding.roadWidth.text.isNullOrEmpty() -> {
                showToast("Please fill Road Width field")
                false
            }
//            binding.others.text.isNullOrEmpty() -> {
//                showToast("Please fill Others field")
//                false
//            }
//            binding.remark.text.isNullOrEmpty() -> {
//                showToast("Please fill Remark field")
//                false
//            }
            else -> true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
//    private suspend fun submitReport2(): Any {
//        val occupants = getOccupantsValues()
//        Log.d("occupants",occupants.toString())
//        val floorN = getFloorN()
//        val floorD = getFloorD()
//        val floorA = getFloorA()
//        Log.d("occupant",binding.occupancy.selectedItem.toString())
////        val checkboxVal = getCheckboxVal()
////        val dynamicValues = getDynamicValues()
////        Log.d("floor",floorN.toString()+": "+floorD.toString()+": "+floorA.toString())
////        Log.d("values",dynamicValues.toString())
//        return false
//    }
    data class Item(val id: Int, val value: String?, val subvalue: String?)
    private fun getDynamicValues(): String {
        val values = mutableListOf<String>()
//        values.addAll(dynamicCheckbox.map { it.text.toString() })
//        values.addAll(dynamicSelect.map { it.selectedItem.toString() })
//        Log.d("selItem",(dynamicSelect.map { it.selectedItem.toString() }).toString())
//        Log.d("selItem",dynamicSelectSet.toString())
//        mapCkeckbox["4"] = values.toMutableList()
//        val formattedValues = values.joinToString(prefix = "{", postfix = "}", separator = ", ")
        var result = ""
//        val result = mapOf(key to values.toSet()) // Convert to a Set to ensure uniqueness
        dynamicCheckboxSet.forEach { pair ->
            val inputId = pair.first // The input ID
            val checkBoxText = pair.second.text // The CheckBox text
            values.add("{${inputId},${checkBoxText},null}")   // Format as required, e.g., "4, check1"
            result = "[${values.joinToString(", ")}]"
        }
        dynamicTextSet.forEach { pair ->
            val inputId = pair.first // The input ID
            val checkBoxText = pair.second.text // The CheckBox text
            values.add("{${inputId},${checkBoxText},null}")   // Format as required, e.g., "4, check1"
            result = "[${values.joinToString(", ")}]"
        }
     //            val mainSpinner = spinnerPair.first
//            val subSpinner = spinnerPair.second
//
//            val selectedValue = mainSpinner.selectedItem.toString()
//            val optionId = optionIds[index] // Retrieve the optionId from the list
//
//            // Check if the sub-spinner exists
//            val subValue = subSpinner?.selectedItem.toString() ?: "No Sub Value" // Use a default value if there's no sub-spinner
//
//            Pair(optionId, Pair(selectedValue, subValue)) // Create a pair of optionId and selected values
//        }
//
//        val resultList = dynamicSelect.map { spinner ->
//            val selectedValue = spinner.selectedItem.toString()
//            val optionId = spinner.tag as String // Retrieve the optionId using the tag
//
//            Pair(optionId, selectedValue) // Create a pair of optionId and selected spinner value
//        }
        dynamicSelect.map { spinnerPair ->
            val mainSpinner = spinnerPair.first
            val subSpinner = spinnerPair.second

            val selectedValue = mainSpinner.selectedItem.toString()
            val optionId = mainSpinner.tag as String // Retrieve the optionId from the tag

            // Check if the sub-spinner exists
            val subValue = subSpinner?.selectedItem.toString() ?: "null" // Use a default value if there's no sub-spinner

            Pair(optionId, Pair(selectedValue, subValue)) // Create a pair of optionId and selected values
            values.add("{${optionId},${selectedValue},${subValue}}")
        }


        val cleanData = values.toString()
            .removeSurrounding("[", "]") // remove the square brackets
            .split("},") // split items
            .map {
                // Remove leading '{' and replace '}' at the end
                val itemString = it.trim().removePrefix("{").removeSuffix("}")
                val parts = itemString.split(",") // split by ','

                // Parse the id, name, and additional values
                val id = parts[0].toInt() // assuming all parts are well formatted as per your example
                val value = if (parts[1].isNotEmpty() or parts[1].isNotBlank()) parts[1] else null // handle empty names
//                val subvalue = if (parts.size > 2 && parts[2] != "null") parts[2] else null // handle nulls
                val subvalue = if (parts[2].isNotBlank() && parts[2] != "null") parts[2] else null // handle nulls
                Item(id, value, subvalue)
//                Log.d("floornamee",id.toString()+ value+ subvalue)
            }
        val gson: Gson = GsonBuilder().serializeNulls().create()
        val jsonDynamicValues = gson.toJson(cleanData)
//        val gson = Gson()
//        val jsonDynamicValues = gson.toJson(cleanData)
// resultList will now contain tuples of (optionId, (mainValue, subValue))
        return jsonDynamicValues
    }
    private fun getFloorN(): List<String> {
        val values = mutableListOf<String>()
        val existingFloorN = findViewById<TextInputEditText>(R.id.flName)
        existingFloorN?.let {
            values.add(it.text.toString())
        }
        values.addAll(floornames.map { it.text.toString() })

        return values
    }
    private fun getFloorD(): List<String> {
        val values = mutableListOf<String>()
        val existingFloorD = findViewById<TextInputEditText>(R.id.flDetails)
        existingFloorD?.let {
            values.add(it.text.toString())
        }
        values.addAll(floordetails.map { it.text.toString() })

        return values
    }
    private fun getFloorA(): List<String> {
        val values = mutableListOf<String>()
        val existingFloorA = findViewById<TextInputEditText>(R.id.flArea)
        existingFloorA?.let {
            values.add(it.text.toString())
        }
        values.addAll(floorareas.map { it.text.toString() })

        return values
    }

    private suspend fun submitReport(): Boolean {
        return try {
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            val userid = sharedPreferences.getString("userid", null)
            val urlField = findViewById<TextView>(R.id.receptionId)
            val userIdFiled = findViewById<TextView>(R.id.userId)
            var jobId: String? = null
            val urlText = urlField.text.toString()
            val userIdText = userIdFiled.text.toString()
            val pattern = Pattern.compile(".*/(\\d+)/$")
            val matcher = pattern.matcher(urlText)

            val occupantsArray = getOccupantsValues()
            val occupantjsonArray = JSONArray(occupantsArray)
            val occupants = occupantjsonArray.toString()

            val floorN = getFloorN()

            val floorNjsonArray = JSONArray(floorN)

            val floorNamesArray = floorNjsonArray.toString()
            val floorD = getFloorD()
            val floorDjsonArray = JSONArray(floorD)
            val floorDetailsArray = floorDjsonArray.toString()
            val floorA = getFloorA()
            val floorAjsonArray = JSONArray(floorA)
            val floorAreasArray = floorAjsonArray.toString()
            val dynamicValues = getDynamicValues()
            Log.d("floorname",dynamicValues)
//            val dynamicValuesjsonArray = JSONArray(dynamicValues)
//            Log.d("floornamea",dynamicValuesjsonArray.toString())
//            val dynamicValuesArray = dynamicValuesjsonArray.toString()
//            Log.d("floornameb",dynamicValuesArray.toString())
            if (matcher.find()) {
                jobId = matcher.group(1)
            }


            var userdetail = URL(Constants.getUrl("api/user/${userIdText}/"))
//            "http://10.0.2.2:8000/api/user/${userIdText}/"
            val url = URL(Constants.getUrl("api/engineer/createengreport/"))
//            val url = URL("http://10.0.2.2:8000/api/engineer/createengreport/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonInput = JSONObject().apply {
                // id no of saved record
                // applicaiton no
                // reception id
                put("applicationnumber", binding.applicationNumber.text.toString())
                put("name", binding.name.text.toString())
                put("visitinpresence", binding.visitInPresence.text.toString())
                put("bankname", binding.bankName.text.toString())
                put("bankid", binding.bankId.text.toString())
                put("casetype", binding.caseType.text.toString())
                put("add1", binding.add1.text.toString())
                put("add2", binding.add2.text.toString())
                put("city", binding.city.text.toString())
                put("region", binding.region.text.toString())
                put("zip", binding.zip.text.toString())
                put("country", binding.country.text.toString())
                put("east", binding.east.text.toString())
                put("west", binding.west.text.toString())
                put("north", binding.north.text.toString())
                put("south", binding.south.text.toString())
//                put("gfarea", binding.gfArea.text.toString())
//                put("ffarea", binding.ffArea.text.toString())
//                put("sfarea", binding.sfArea.text.toString())
//                put("tfarea", binding.tfArea.text.toString())
                put("occupant", binding.occupancy.selectedItem.toString())
                put("occupants", occupants)
                put("floors", floorNamesArray)
                put("floor_details", floorDetailsArray)
                put("floor_areas", floorAreasArray)
                put("dynamicValues", dynamicValues)
                put("propertyage", binding.propertyAge.text.toString())
                put("landrate", binding.landRate.text.toString())
//                put("Occupant", binding.occupant.text.toString())
//                put("rented", binding.rented.text.toString())
                put("landmark", binding.landmark.text.toString())
                put("roadwidth", binding.roadWidth.text.toString())
//                put("hightensionline", binding.highTensionLine.isChecked)
//                put("railwayline", binding.railwayLine.isChecked)
//                put("nala", binding.nala.isChecked)
//                put("river", binding.river.isChecked)
//                put("pahad", binding.pahad.isChecked)
//                put("roadcomesunderroadbinding", binding.roadComesUnderRoadBinding.isChecked)
//                put("propertyaccessissue", binding.propertyAccessIssue.isChecked)
//                put("othercheck", binding.otherCheck.isChecked)
//                put("others", binding.others.text.toString())
                put("remark", binding.remark.text.toString())
                put("reporter", binding.reporter.text.toString())
                put("priority", binding.priority.isChecked)
                put("receptionid", binding.receptionId.text.toString())
                put("userdetailsid", userdetail )
                put("lat", currentLocation?.longitude.toString())
                put("lng", currentLocation?.latitude.toString())
            }

            println("JSON Payload: $jsonInput")

            OutputStreamWriter(connection.outputStream).use { it.write(jsonInput.toString()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                println("Full Response: $response")

                // Parse JSON response
                val jsonResponse = JSONObject(response)
                val data = jsonResponse.getJSONObject("data")

                // Define regex pattern to extract the ID from the URL
                val pattern = Pattern.compile(".*/(\\d+)/$")

                //we need applicatoin no here ..
                val Url = data.getString("url")
                val Matcher = pattern.matcher(Url)
                val resId = if (Matcher.find()) Matcher.group(1) else null

                val applicationNo = findViewById<TextView>(R.id.applicationNumber).text.toString()
                // Extract `receptionid`
                val receptionUrl = data.getString("receptionid")
                val receptionMatcher = pattern.matcher(receptionUrl)
                val receptionId = if (receptionMatcher.find()) receptionMatcher.group(1) else null

                // Extract `userdetailsid`
                val userUrl = data.getString("userdetailsid")
                val userMatcher = pattern.matcher(userUrl)
                val userId = if (userMatcher.find()) userMatcher.group(1) else null

                if (receptionId != null && userId != null && resId != null) {
                    // Call uploadFiles and check its success
                    val uploadSuccess = uploadFiles(1,receptionId, userId, resId, applicationNo)
                    if (uploadSuccess) {
                        uploadFiles(101,receptionId, userId, resId, applicationNo)
                        uploadFiles(102,receptionId, userId, resId, applicationNo)
                        uploadFiles(103,receptionId, userId, resId, applicationNo)
                        uploadFiles(104,receptionId, userId, resId, applicationNo)
                        uploadFiles(105,receptionId, userId, resId, applicationNo)
                        uploadFiles(106,receptionId, userId, resId, applicationNo)
                        withContext(Dispatchers.Main) {
                            CoroutineScope(Dispatchers.IO).launch {
                                updateOutAttendance(applicationNo,receptionId.toInt())
                            }

                            Toast.makeText(this@SubmitReport, "Report submitted and files uploaded successfully", Toast.LENGTH_LONG).show()
                        }
                        true
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SubmitReport, "Report submitted, but file upload failed", Toast.LENGTH_LONG).show()
                        }
                        false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SubmitReport, "Failed to extract IDs", Toast.LENGTH_LONG).show()
                    }
                    false
                }

                //id
                //application no
                //reception id
                // multi
                //
                true
            } else {
                val errorStream = connection.errorStream
                val errorMessage = errorStream?.bufferedReader()?.use { it.readText() }
                println("------------------- $errorMessage")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SubmitReport, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubmitReport, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }

    private fun getOccupantsValues(): List<String> {
        val values = mutableListOf<String>()
        val existingOccupant = findViewById<TextInputEditText>(R.id.occupantName)
        existingOccupant?.let {
            values.add(it.text.toString())
        }
        values.addAll(occupantsname.map { it.text.toString() })

        return values
    }

    private fun handleReportSubmissionResult(success: Boolean) {
        if (success) {
            Toast.makeText(this, "Report created successfully", Toast.LENGTH_SHORT).show()

            // Create an Intent to navigate to the Dashboard activity
            val intent = Intent(this, Dashboard::class.java)

            // Pass the `userid` and `token` to the Dashboard activity if needed
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            val userid = sharedPreferences.getString("userid", null)
            val name = sharedPreferences.getString("name", null)

            intent.putExtra("userid", userid)
            intent.putExtra("token", token)
            intent.putExtra("name", name)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Failed to create report", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
    }
    private fun openFileChooserA(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        selectedFiles.add(fileUri)
//                        when (requestCode) {
//                            REQUEST_CODE_SELECT_FILE_A -> selectedFilesA.add(fileUri)
//                            REQUEST_CODE_SELECT_FILE_B -> selectedFilesB.add(FileUpload(fileUri, customName))
//                            REQUEST_CODE_SELECT_FILE_C -> selectedFilesC.add(FileUpload(fileUri, customName))
//                        }
                        Log.d("fileuri", fileUri.toString())
                        println(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    selectedFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        } else if (requestCode == REQUEST_CODE_SITE_FORM && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        siteFormFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    siteFormFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_ELEC_METER && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        electricMeterFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    electricMeterFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_ELEC_BILL && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        electricBillFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    electricBillFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_SITE_PHOTO && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        sitePhotosFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    sitePhotosFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_KHASRA && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        khasraFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    khasraFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_SITE_REFERENCE && resultCode == Activity.RESULT_OK){
            data?.let {
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        val fileUri = it.clipData!!.getItemAt(i).uri
                        siteReferenceFiles.add(fileUri)
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    siteReferenceFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }
    }

    private fun displaySelectedFile(fileUri: Uri,requestCode:Int) {
        var selectedFilesContainer: LinearLayout=findViewById(R.id.selected_files_container)
        if (requestCode==REQUEST_CODE_SELECT_FILE) {
            selectedFilesContainer = findViewById(R.id.selected_files_container)
        }else if (requestCode== REQUEST_CODE_SITE_FORM){
            selectedFilesContainer = findViewById(R.id.siteFormcontainer)
        }else if (requestCode== REQUEST_CODE_ELEC_METER){
            selectedFilesContainer = findViewById(R.id.electricityMetercontainer)
        }else if (requestCode== REQUEST_CODE_ELEC_BILL){
            selectedFilesContainer = findViewById(R.id.electricityBillcontainer)
        }else if (requestCode== REQUEST_CODE_KHASRA){
            selectedFilesContainer = findViewById(R.id.khasracontainer)
        }else if (requestCode== REQUEST_CODE_SITE_PHOTO){
            selectedFilesContainer = findViewById(R.id.sitePhotosContainer)
        }else if (requestCode== REQUEST_CODE_SITE_REFERENCE){
            selectedFilesContainer = findViewById(R.id.siteReferenceContainer)
        }
        val fileName = fileUri.lastPathSegment ?: "Unknown"
        val fileView = layoutInflater.inflate(R.layout.item_selected_file, selectedFilesContainer, false)
        val textViewFileName: TextView = fileView.findViewById(R.id.text_view_file_name)
        val buttonRemoveFile: ImageButton = fileView.findViewById(R.id.button_remove_file)

        textViewFileName.text = fileName
        buttonRemoveFile.setOnClickListener {
//            selectedFiles.remove(fileUri)
            when (requestCode) {
                REQUEST_CODE_SELECT_FILE -> selectedFiles.remove(fileUri)
                REQUEST_CODE_SITE_FORM -> siteFormFiles.remove(fileUri)
                REQUEST_CODE_ELEC_METER -> electricMeterFiles.remove(fileUri)
                REQUEST_CODE_ELEC_BILL -> electricBillFiles.remove(fileUri)
                REQUEST_CODE_KHASRA -> khasraFiles.remove(fileUri)
                REQUEST_CODE_SITE_PHOTO -> sitePhotosFiles.remove(fileUri)
                REQUEST_CODE_SITE_REFERENCE -> siteReferenceFiles.remove(fileUri)
            }
            selectedFilesContainer.removeView(fileView)
        }

        selectedFilesContainer.addView(fileView)
    }

    private fun uploadFiles(
//        selectedFilesA: List<Uri>,
        request: Int,
        receptionId: String,
        userId: String,
        resId: String,
        applicationNo: String
    ): Boolean {
        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
        val role = "Engineer"
        val platform = "engineer"

        return try {
            // Retrieve the token from SharedPreferences
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)

            if (token.isNullOrEmpty()) {
                println("Token is missing.")
                return false
            }

            // Construct the URL for the API endpoint
            val url = URL(Constants.getUrl("api/engfilesupload/$resId/"))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                doInput = true
                useCaches = false
                requestMethod = "POST"
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Authorization", "token $token")
            }

            val outputStream = DataOutputStream(connection.outputStream)

            // Add form data (non-file parameters)
            fun writeFormField(name: String, value: String) {
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                outputStream.writeBytes("$value\r\n")
            }

            writeFormField("application_number", applicationNo)
//            writeFormField("role", role)
            writeFormField("platform", platform)
            writeFormField("reception_idno", receptionId)
            writeFormField("userdetailsid", userId)
            if (request==REQUEST_CODE_SELECT_FILE) {
                writeFormField("role", "other")
                files = selectedFiles
            }
            if (request==REQUEST_CODE_SITE_FORM) {
                writeFormField("role", "siteForm")
                files = siteFormFiles
            }
            if (request==REQUEST_CODE_ELEC_METER) {
                writeFormField("role", "electricityMeter")
                files = electricMeterFiles
            }
            if (request==REQUEST_CODE_ELEC_BILL) {
                writeFormField("role", "electricityBill")
                files = electricBillFiles
            }
            if (request==REQUEST_CODE_KHASRA) {
                writeFormField("role", "khasra")
                files = khasraFiles
            }
            if (request==REQUEST_CODE_SITE_PHOTO) {
                writeFormField("role", "sitePhoto")
                files = sitePhotosFiles
            }
            if (request==REQUEST_CODE_SITE_REFERENCE) {
                writeFormField("role", "siteReference")
                files = siteReferenceFiles
            }
            // Attach files

            files.forEach { fileUri ->
                    val file = File(cacheDir, fileUri.lastPathSegment ?: "tempFile")
                    contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"


                    outputStream.writeBytes("--$boundary\r\n")
                    outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"files\"; filename=\"${file.name}\"\r\n"
                    )
                    outputStream.writeBytes("Content-Type: $mimeType\r\n\r\n")

                    file.inputStream().use { input ->
                        input.copyTo(outputStream)
                    }
                    outputStream.writeBytes("\r\n")
                }

                // End of multipart/form-data
                outputStream.writeBytes("--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

            // Read the server response
            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

//            println("Response Code: $responseCode")
//            println("Response Message: $responseMessage")

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                println("Files uploaded successfully.")
                true
            } else {
                println("File upload failed: $responseCode $responseMessage")
                false
            }
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun fetchDynamicFields(): String {
        return try {
            val url = URL(Constants.getUrl("api/dynamicfields/"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
//            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Accept", "application/json")

            val result = connection.inputStream.bufferedReader().use { it.readText() }
            println(result)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }
    private fun handleDynFetchResult(result: String) {
        try {
            val jsonArray = JSONArray(result)
            generateDynamicView(jsonArray)

        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateDynamicView(data: JSONArray) {
        var selectedId: String? = null
        for (i in 0 until data.length()) {
            Log.d("datal",data.length().toString())
            val item = data.getJSONObject(i)
            val label = item.getString("label")
            val inpType = item.getString("input_type")
            //inputid
            val optionid = item.getString("id")
            val subOption = item.getBoolean("suboption")

            if (inpType == "select") {
                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(40, 8, 40, 8)
                    }
                }

                // Create a TextView for the label
                val labelView = TextView(this).apply {
                    text = "Select " + label
                    setPadding(0, 0, 0, 8)
                }

                // Create a CardView to wrap the Spinner
                val cardView = MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    radius = 8f
                    setContentPadding(16, 32, 16, 32)
                    setCardBackgroundColor(Color.WHITE)
                    strokeColor = Color.GRAY
                    strokeWidth = 2
                }

                // Create the Spinner
                val spinner = Spinner(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT

                    )

                    id = View.generateViewId() // Generate a unique ID for this Spinner
                }
                spinner.tag = optionid

                val subspinner: Spinner? = if (subOption) {
                    val sub = Spinner(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        id = View.generateViewId() // Generate a unique ID for this Spinner
                    }

                    sub

                } else {
                    null // No sub-spinner for this optionId
                }

                // Coroutine to fetch options
                CoroutineScope(Dispatchers.IO).launch {
                    val result = getOptions(optionid)
//                    delay(5000)
                    val jsonObject = JSONObject(result)
                    val dataArray = jsonObject.getJSONArray("data")
                    Log.d("darry", dataArray.toString())
                    val optValues = mutableListOf<String>()

                    val optIds = mutableListOf<String>()
                    for (j in 0 until dataArray.length()) {
                        val optionItem = dataArray.getJSONObject(j)
                        Log.d("opitem",optionItem.get("sub_options").toString())
                        optValues.add(optionItem.getString("opt_value"))
                        optIds.add(optionItem.getString("id"))
                    }
                    withContext(Dispatchers.Main) {
                        // Set up the adapter for the Spinner on the main thread
                        val adapter = ArrayAdapter(
                            this@SubmitReport,
                            android.R.layout.simple_spinner_item,
                            optValues
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                        //-----
                        val subresult = dataArray.getJSONObject(0).getString("sub_options")
                        val suboptValues = mutableListOf<String>()
                        val subjsonArray = JSONArray(subresult)
                        for (k in 0 until subjsonArray.length()) {
                            val optionItem = subjsonArray.getJSONObject(k)
                            suboptValues.add(optionItem.getString("name"))
                        }
                        val subadapter = ArrayAdapter(
                            this@SubmitReport,
                            android.R.layout.simple_spinner_item,
                            suboptValues
                        )
                        subadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        subspinner?.adapter = subadapter
                        delay(10000)
                        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View,
                                position: Int,
                                id: Long
                            ) {
                                val selectedValue = parent.getItemAtPosition(position) as String
                                spinnerValue = parent.getItemAtPosition(position) as String
                                selectedId = optIds[position]

//                                dynamicSelectSet.add(Pair(optionid, selectedValue))
                                if (subOption) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val subresult = getSubOptions(selectedId!!)
                                        val subjsonObject = JSONObject(subresult)
                                        val subdataArray = subjsonObject.getJSONArray("data")
                                        val suboptValues = mutableListOf<String>()

                                        for (k in 0 until subdataArray.length()) {
                                            val optionItem = subdataArray.getJSONObject(k)
                                            suboptValues.add(optionItem.getString("name"))
                                        }

                                        withContext(Dispatchers.Main) {
                                            // Set up the adapter for the Spinner on the main thread
                                            val subadapter = ArrayAdapter(
                                                this@SubmitReport,
                                                android.R.layout.simple_spinner_item,
                                                suboptValues
                                            )
                                            subadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                            subspinner?.adapter = subadapter
                                        }
                                    }
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {
                                // Handle case when nothing is selected
                            }
                        }
                        subspinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View,
                                position: Int,
                                id: Long
                            ) {
                                subspinnerValue = parent.getItemAtPosition(position) as String
//                                selectedValues.add("{${optionid},${spinnerValue},${subspinnerValue}}")
                                Log.d("spinS", subspinnerValue.toString())
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {

                            }
                        }
                    }
                }

                // Add the Spinner to the CardView
                cardView.addView(spinner)

                // Add the label and CardView to the container
                container.addView(labelView)
                container.addView(cardView)
                if (!subOption)
                dynamicSelect.add(Pair(spinner,subspinner))
                if (subOption){
                    val sublabelView = TextView(this).apply {
                        text = "Select sub" + label
                        setPadding(0, 0, 0, 8)
                    }

                    // Create a CardView to wrap the Spinner
                    val subcardView = MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        radius = 8f
                        setContentPadding(16, 32, 16, 32)
                        setCardBackgroundColor(Color.WHITE)
                        strokeColor = Color.GRAY
                        strokeWidth = 2
                    }

                     // Add the Spinner to the CardView
                    subcardView.addView(subspinner)

                    // Add the label and CardView to the container
                    container.addView(sublabelView)
                    container.addView(subcardView)
                    dynamicSubSelect.add(subspinner)
                    dynamicSelect.add(Pair(spinner,subspinner))
                }
                // Finally, add the container to the parent layout
                linearLayout.addView(container)

            }
            else if (inpType == "checkbox"){
                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(40, 8, 40, 8)
                    }
                }
                val labelView = TextView(this).apply {
                    text =  label
                    setPadding(0, 0, 0, 8)
                }
                container.addView(labelView)
                // Coroutine to fetch checkboxes text
                CoroutineScope(Dispatchers.IO).launch {
                    val chkresult = getOptions(optionid)
                    val chkjsonObject = JSONObject(chkresult)
                    val chkdataArray = chkjsonObject.getJSONArray("data")
                        Log.d("chkbox",chkresult)
                    val optValues = mutableListOf<String>()
                    val optIds = mutableListOf<String>()
                    for (j in 0 until chkdataArray.length()) {
                        val optionItem = chkdataArray.getJSONObject(j)
                        optValues.add(optionItem.getString("opt_value"))
                        optIds.add(optionItem.getString("id"))
                        // Create a new CheckBox for each option
                        val checkBox = CheckBox(this@SubmitReport).apply {
                            text = optionItem.getString("opt_value")
//                            id = optId ?: View.generateViewId() // Safely handle ID assignment
                            id = View.generateViewId()
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }

                        // Switch to Main thread to update the UI
                        withContext(Dispatchers.Main) {
                            container.addView(checkBox)


                        }
                        checkBox.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                dynamicCheckbox.add(checkBox)
                                dynamicCheckboxSet.add(Pair(optionid, checkBox))
                            } else {
                                dynamicCheckbox.remove(checkBox)
                                dynamicCheckboxSet.remove(Pair(optionid, checkBox))
                            }
                        }
                    }
                }
                linearLayout.addView(container)
            }
            else {

                val textInputLayout = TextInputLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(40, 8, 40, 8)
                    }
                    hint = label
                    setHintEnabled(true)
                    boxBackgroundColor = ContextCompat.getColor(
                        this@SubmitReport,
                        R.color.editbox
                    )
                    boxStrokeColor = ContextCompat.getColor(this@SubmitReport, R.color.border_color)
                    boxStrokeWidthFocused = 2
                    boxStrokeWidth = 2
                    setPadding(16, 16, 16, 16) // Optional: setting padding
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE // Set the outlined box mode

                }

                // Create a TextInputEditText
                val textInputEditText = TextInputEditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setBackground(null)
                    inputType = InputType.TYPE_CLASS_TEXT
                    gravity = Gravity.TOP or Gravity.START
//                id = View.generateViewId() // Generate a unique ID for this EditText
                    id = createViewId(label) // Generate a unique ID for this EditText
                }

                // Add the EditText to the TextInputLayout
                textInputLayout.addView(textInputEditText)
                dynamicTextSet.add(Pair(optionid,textInputEditText))
                // Finally, add the TextInputLayout to the LinearLayout
                linearLayout.addView(textInputLayout)
            }

        }
    }


    suspend fun getOptions(id: String): String {

        return withContext(Dispatchers.IO) {
//        try {
            val url = URL(Constants.getUrl("api/optionvalues/$id/dynoptionvalues/"))
            Log.d("options", "URL: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            // Check the response code
            val responseCode = connection.responseCode
            Log.d("optionsa", "response: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val result = connection.inputStream.bufferedReader().use { it.readText() }
                println(result)
                Log.d("options", "Response: $result")
                result

            } else {
                // Handle error response
                val errorResponse = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("options", "Error: $errorResponse")
                "[]" // Or handle however you want
            }

//        } catch (e: Exception) {
//            e.printStackTrace()
//            return "[]"
//        }
        }

    }
    private fun getSubOptions(id: String): String {

        return try {
            val url = URL(Constants.getUrl("api/suboptions/$id/dynsuboptionvalues/"))
            Log.d("suburl", "URL: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")

            // Check the response code
            val responseCode = connection.responseCode
            Log.d("optionsa", "response: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val result = connection.inputStream.bufferedReader().use { it.readText() }
                println(result)
                Log.d("options", "Response: $result")
                result
            } else {
                // Handle error response
                val errorResponse = connection.errorStream.bufferedReader().use { it.readText() }
                Log.e("options", "Error: $errorResponse")
                "[]" // Or handle however you want
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "[]"
        }
    }
    private fun createViewId(label: String): Int {
        val sanitizedLabel = label
            .replace(" ", "_")
            .replace(Regex("[^A-Za-z0-9_]"), "")
            .lowercase()
        return View.generateViewId()
    }

    companion object {
        private const val REQUEST_CODE_SELECT_FILE = 1
        private const val REQUEST_CODE_SITE_FORM = 101
        private const val REQUEST_CODE_ELEC_METER = 102
        private const val REQUEST_CODE_ELEC_BILL = 103
        private const val REQUEST_CODE_KHASRA = 104
        private const val REQUEST_CODE_SITE_PHOTO = 105
        private const val REQUEST_CODE_SITE_REFERENCE = 106
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}