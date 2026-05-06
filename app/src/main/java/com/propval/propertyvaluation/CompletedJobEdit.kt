package com.propval.propertyvaluation

import Constants
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.propval.propertyvaluation.SubmitReport.Companion
import com.propval.propertyvaluation.databinding.ActivityCompletedJobEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class CompletedJobEdit : AppCompatActivity() {

    private lateinit var binding: ActivityCompletedJobEditBinding
    private var jobId: String? = null
    private var isView: String? = null
    private lateinit var progressBar: View
    private var files = mutableListOf<Uri>()
    private val selectedFiles = mutableListOf<Uri>()
    private val siteFormFiles = mutableListOf<Uri>()
    private val electricMeterFiles = mutableListOf<Uri>()
    private val electricBillFiles = mutableListOf<Uri>()
    private val khasraFiles = mutableListOf<Uri>()
    private val sitePhotosFiles = mutableListOf<Uri>()
    private val siteReferenceFiles = mutableListOf<Uri>()
    private var receptionId: String? = null
    private var resId: String? = null
    private var applicationNo: String? = null
    private var userId: String? = null
    private val bankNames = mutableListOf<String>()
    private lateinit var floorlinearLayout :LinearLayout
    private lateinit var occupantlinearLayout :LinearLayout
    private val occupantsname = mutableListOf<TextInputEditText>()
    private val floornames = mutableListOf<TextInputEditText>()
    private val floordetails = mutableListOf<TextInputEditText>()
    private val floorareas = mutableListOf<TextInputEditText>()
    private var spinnerValue: String? = null
    private var subspinnerValue: String=""
    private val dynamicTextSet = mutableSetOf<Pair<String,TextInputEditText>>()
    private val dynamicCheckbox = mutableListOf<CheckBox>()
    private val dynamicCheckboxSet = mutableSetOf<Pair<String, CheckBox>>()
    private val dynamicSelect = mutableListOf<Pair<Spinner, Spinner?>>()
    private val dynamicSubSelect = mutableListOf<Spinner?>()
    private lateinit var linearLayout: LinearLayout

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompletedJobEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        floorlinearLayout = findViewById(R.id.floorlayout)
        occupantlinearLayout = findViewById(R.id.occupantLayout)
        // Initialize the ProgressBar
        progressBar = findViewById(R.id.completedJobEditProgressBar)
        linearLayout = findViewById(R.id.dynamicFields)
        // Set listener on the Bank Name field
        binding.bankName.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                fetchBankNamesAndShowDialog()
            }
            false // Return false to allow normal behavior (e.g., showing the keyboard).
        }
        val ocAddButton: Button = findViewById(R.id.addOccupant)
        ocAddButton.setOnClickListener {
            addOccupants("")
        }
        val ocspinner: Spinner = findViewById(R.id.occupancy)
        val spitems = arrayOf("Single Occupancy", "Multiple Occupancy")
        val spadapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spitems)
        ocspinner.adapter = spadapter
        ocspinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                ocAddButton.visibility = if (ocAddButton.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        val addfloorbtn:Button= findViewById(R.id.addfloor)
        addfloorbtn.setOnClickListener {
            addFloorfields("", "", "")
        }

        jobId = intent.getStringExtra("job_id")
        isView = intent.getStringExtra("isView")

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

        if (jobId != null) {
            fetchJobData(jobId!!)
            fetchOccupants(jobId!!,ocspinner,spitems,ocAddButton)
            fetchFloorData(jobId!!)
            fetchDynamicsData(jobId!!)
        } else {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener {
            if (jobId != null) {
                saveJobData(jobId!!)
            } else {
                Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchDynamicsData(jobId: String) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val dynamicData = getDynamicData(jobId)
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after data fetch
                progressBar.visibility = View.GONE
                if (dynamicData != null) {
                    generateDynamicFields(dynamicData)
                } else {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch Dynamic data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun fetchFloorData(jobId: String) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val floorData = getfloorData(jobId)
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after data fetch
                progressBar.visibility = View.GONE
                if (floorData != null) {
                    generateFloors(floorData)
                } else {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch floor data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun generateFloors(floorData: JSONObject) {
            val floorsArray = floorData.getJSONArray("data")
        Log.d("flr",floorsArray.toString())
        for (i in 0 until floorsArray.length()) {
            val floor = floorsArray.getJSONObject(i)
            val floorName = floor.getString("floorname")
            val floorDetail = floor.getString("floordetail")
            val floorArea = floor.getString("floorarea")

            addFloorfields(floorName,floorDetail,floorArea)
        }
        if (floorsArray.length()==0){
            addFloorfields("","","")
        }
    }

    private suspend fun getfloorData(jobId: String): JSONObject? {
        return try {
//            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//            val token = sharedPreferences.getString("token", null)
//
//            if (token.isNullOrEmpty()) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@CompletedJobEdit,
//                        "Token is missing. Please log in again.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//                return null
//            }
//            api/floor/38/getfloor/
            val url = URL(Constants.getUrl("api/floor/$jobId/getfloor/"))
            val connection = withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection
            connection.requestMethod = "GET"
//            connection.setRequestProperty("Authorization", "token $token")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch data. Response code: ${connection.responseCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(response)
            return data

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CompletedJobEdit,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            return null
        }
    }

    private fun fetchOccupants(
        jobId: String,
        ocspinner: Spinner,
        spitems: Array<String>,
        ocAddButton: Button
    ) {

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val occupantData = getOccupantData(jobId)
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after data fetch
                progressBar.visibility = View.GONE
                if (occupantData != null) {
                    generateOccupants(occupantData,ocspinner,spitems,ocAddButton)
                } else {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch Occupant data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

//        progressBar.visibility = View.VISIBLE
//        var occupantdata: JSONObject
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val url = URL(Constants.getUrl("api/occupant/$jobId/getoccupant/"))
//                val connection = url.openConnection() as HttpURLConnection
//                connection.requestMethod = "GET"
////            connection.setRequestProperty("Authorization", "token $token")
//                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@CompletedJobEdit,
//                            "Failed to fetch occupant data. Response code: ${connection.responseCode}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//
//                }
//
//                val response = connection.inputStream.bufferedReader().use { it.readText() }
//                occupantdata = JSONObject(response)
//                Log.d("occupant",occupantdata.getJSONArray("data").toString())
//                val occupantArray = occupantdata.getJSONArray("data")
//
//
//                for (i in 0 until occupantArray.length()) {
//                    val occupant = occupantArray.getJSONObject(i)
//                    val occupantName = occupant.getString("occupantname")
//                    val occupantId = occupant.getString("id")
//                    Log.d("siz",occupantArray.length().toString())
//                    if(occupantArray.length()>1){
//                        addOccupants(occupantName)
//                    }else {
//                        binding.occupantName.setText(occupantName)
//                    }
//                }
//                if(occupantArray.length()>1){
//                    ocAddButton.visibility = if (ocAddButton.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
//
//                    ocspinner.setSelection(spitems.indexOf("Multiple Occupancy"))
//                }
//            }catch (e: Exception) {
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@CompletedJobEdit,
//                        "An error occurred: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//
//            }
//            withContext(Dispatchers.Main) {
//                // Hide ProgressBar after data fetch
//                progressBar.visibility = View.GONE
//
//            }
//        }


    }

    private suspend fun getOccupantData(jobId: String): JSONObject? {
        return try {
            val url = URL(Constants.getUrl("api/occupant/$jobId/getoccupant/"))
            val connection = withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection
            connection.requestMethod = "GET"
//            connection.setRequestProperty("Authorization", "token $token")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch occupant data. Response code: ${connection.responseCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }


                return null
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(response)
            return data


        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CompletedJobEdit,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            return null
        }
    }
    private fun generateOccupants(
        occupantData: JSONObject,
        ocspinner: Spinner,
        spitems: Array<String>,
        ocAddButton: Button
    ) {
        val occupantArray = occupantData.getJSONArray("data")
        Log.d("ocArr","len"+occupantArray.length().toString())
        for (i in 0 until occupantArray.length()) {
            val occupant = occupantArray.getJSONObject(i)
            val occupantName = occupant.getString("occupantname")
            addOccupants(occupantName)

        }
        if(occupantArray.length()==0){
            addOccupants("")
        }
        if(occupantArray.length()>1){
            ocAddButton.visibility = if (ocAddButton.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE

            ocspinner.setSelection(spitems.indexOf("Multiple Occupancy"))
        }
    }

    private fun addFloorfields(flName: String, flDetail: String, flArea: String) {

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
            val txtValue = when (i) {
                1 -> flName
                2 -> flDetail
                3 -> flArea
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
                    this@CompletedJobEdit,
                    R.color.editbox
                ) // Replace with your color

                boxStrokeColor = ContextCompat.getColor(this@CompletedJobEdit, R.color.border_color)
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
//                if(ik !=0)
                setText(txtValue)
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
                floornames.removeAll(currentFloorNames)
                floordetails.removeAll(currentFloorDetails)
                floorareas.removeAll(currentFloorAreas)
                // Remove the row from the parent layout
                (sublinearLayout.parent as? LinearLayout)?.removeView(sublinearLayout)
            }
        }
        sublinearLayout.addView(removeButton)
        // Adding TextInputLayout to the LinearLayout
//        floorlinearLayout.addView(textInputLayout)
        floorlinearLayout.addView(sublinearLayout)
    }

    private fun addOccupants(occupantName: String) {
        val textInputLayout = TextInputLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

            }
            hint = "Occupant Name"
            setHintEnabled(true)
            boxBackgroundColor = ContextCompat.getColor(
                this@CompletedJobEdit,
                R.color.editbox
            )

            boxStrokeColor = ContextCompat.getColor(this@CompletedJobEdit, R.color.border_color)
            boxStrokeWidthFocused = 4
            boxStrokeWidth = 4
//            elevation = 4f
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE // Set the outlined box mode

        }
        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

            }
            setPadding(32,32,32,32)
            setText(occupantName)
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
                setMargins(16, 32, 0, 0) // Optional: add some margin to the left of the button
            }
            // Optionally, set minimum width and height to 0
            minimumWidth = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                // Remove the row from the parent layout
                occupantsname.remove(editText) //remove refrences
                (inputContainer.parent as? LinearLayout)?.removeView(inputContainer)
            }
        }

        inputContainer.addView(removeButton)
//        textInputLayout.addView(removeButton)
        occupantlinearLayout.addView(inputContainer)
        occupantsname.add(editText)
    }

    private suspend fun getDynamicData(jobId: String): String {
        return try {
            val url = URL(Constants.getUrl("api/dynamicfields/$jobId/getdynamicvalues/"))
//            val url = URL("http://10.0.2.2:8000/api/reception/$userid/engineerjob/")
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
    private fun generateDynamicFields(result: String) {
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
            Log.d("item",item.toString())
            var dynVal = ""
            var dynSubVal = ""
            if (item.getJSONArray("dynamic_values").length()>0) {
                dynVal = item.getJSONArray("dynamic_values").getJSONObject(0).getString("value")
                dynSubVal = item.getJSONArray("dynamic_values").getJSONObject(0).getString("subvalue")
            }
//            Log.d("dynArray", dyn.getString("value"))
            val label = item.getString("label")
            val inpType = item.getString("input_type")
            //inputid
            val optionid = item.getString("id")
            val subOption = item.getBoolean("suboption")
            Log.d("item1",inpType)
            if (inpType == "select") {
                Log.d("item",item.toString())
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
                val optValues = mutableListOf<String>()
//                optValues.add("Please select")
                val optIds = mutableListOf<String>()
//                optIds.add("Please select")
                val suboptValues = mutableListOf<String>()
//                suboptValues.add("Please select")
                // Coroutine to fetch options
                CoroutineScope(Dispatchers.IO).launch {
                    val result = getOptions(optionid)
//                    delay(5000)
                    val jsonObject = JSONObject(result)
                    val dataArray = jsonObject.getJSONArray("data")
                    Log.d("darry", dataArray.toString())



                    for (j in 0 until dataArray.length()) {
                        val optionItem = dataArray.getJSONObject(j)
                        Log.d("opitem",optionItem.get("sub_options").toString())
                        optValues.add(optionItem.getString("opt_value"))
                        optIds.add(optionItem.getString("id"))
                    }
                    withContext(Dispatchers.Main) {
                        // Set up the adapter for the Spinner on the main thread
                        val adapter = ArrayAdapter(
                            this@CompletedJobEdit,
                            android.R.layout.simple_spinner_item,
                            optValues
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinner.adapter = adapter
                        //-----
                        var subresult: String = ""
                        if (dynVal!="") {
                            subresult = dataArray.getJSONObject(optValues.indexOf(dynVal)).getString("sub_options")
                        }else{
                            subresult = dataArray.getJSONObject(0)
                                .getString("sub_options")
                        }
                        val subjsonArray = JSONArray(subresult)
                        for (k in 0 until subjsonArray.length()) {
                            val optionItem = subjsonArray.getJSONObject(k)
                            suboptValues.add(optionItem.getString("name"))
                        }
                        val subadapter = ArrayAdapter(
                            this@CompletedJobEdit,
                            android.R.layout.simple_spinner_item,
                            suboptValues
                        )
                        subadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        subspinner?.adapter = subadapter
                        if(dynVal!="")
                        spinner.setSelection( optValues.indexOf(dynVal))
                        if (subOption && dynSubVal!="") {
                            Log.d("subop", "a-->" + dynSubVal + suboptValues.toString()+suboptValues.indexOf(dynSubVal))
                            subspinner?.setSelection(suboptValues.indexOf(dynSubVal))
//                            delay(1000000000000000000)
//                            subadapter.notifyDataSetChanged()
                            delay(10000)
                        }else{
                            delay(10000)
                        }


                        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View,
                                position: Int,
                                id: Long
                            ) {
//                                val selectedValue = parent.getItemAtPosition(position) as String
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
                                                this@CompletedJobEdit,
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
                    // Add the Spinner to the CardView
//                    spinner.setSelection( optValues.indexOf(dynVal))

                }

                cardView.addView(spinner)
                Log.d("dynval",dynVal)

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

//                    subspinner?.setSelection(suboptValues.indexOf(dynSubVal))
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
                    val optValues = mutableListOf<String>()
                    val optIds = mutableListOf<String>()
                    for (j in 0 until chkdataArray.length()) {
                        val optionItem = chkdataArray.getJSONObject(j)
                        optValues.add(optionItem.getString("opt_value"))
                        optIds.add(optionItem.getString("id"))
                        val checktext = optionItem.getString("opt_value")
                        // Create a new CheckBox for each option
                        val checkBox = CheckBox(this@CompletedJobEdit).apply {
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
                            val jsonar = item.getJSONArray("dynamic_values")
                            Log.d("jsar","zdv  "+jsonar.toString())
                            for (k in 0 until jsonar.length()) {
                                if (checktext == jsonar.getJSONObject(k).getString("value"))
                                    checkBox.isChecked = true
                            }
//                            delay(1000)
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
                        this@CompletedJobEdit,
                        R.color.editbox
                    )
                    boxStrokeColor = ContextCompat.getColor(this@CompletedJobEdit, R.color.border_color)
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
                id = View.generateViewId() // Generate a unique ID for this EditText
//                    id = createViewId(label) // Generate a unique ID for this EditText
                }

                // Add the EditText to the TextInputLayout
                textInputEditText.setText(dynVal)
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

    private fun fetchJobData(jobId: String) {
        // Show ProgressBar before starting data fetch
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val jobData = getJobData(jobId)
            withContext(Dispatchers.Main) {
                // Hide ProgressBar after data fetch
                progressBar.visibility = View.GONE
                if (jobData != null) {
                    displayJobData(jobData)
                } else {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch job data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun getJobData(jobId: String): JSONObject? {
        return try {
            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)

            if (token.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Token is missing. Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return null
            }
//            api/floor/38/getfloor/
            val url = URL(Constants.getUrl("api/engineer/$jobId/"))
            Log.d("urll",url.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $token")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to fetch data. Response code: ${connection.responseCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(response)
            Log.d("phone1",data.toString())
            // Extract IDs
            val pattern = Pattern.compile(".*/(\\d+)/$")
            resId = extractIdFromUrl(data.optString("url"), pattern)
            val receptionJson = JSONObject(data.optString("receptionid")) // Parse it as JSONObject
            receptionId = receptionJson.optString("id") // Extract the nested "id" field
            val phonenumber = receptionJson.optString("phonenumber")
            userId = extractIdFromUrl(data.optString("userdetailsid"), pattern)
            applicationNo = data.optString("applicationnumber")

            if (resId == null || receptionId == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Failed to extract IDs from response.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return null
            }

            // Fetch additional data
            loadAndDisplayFiles(receptionId!!, applicationNo!!)
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"siteForm")
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"electricityMeter")
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"electricityBill")
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"khasra")
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"sitePhoto")
            loadAndDisplaySiteFormFiles(receptionId!!, applicationNo!!,"siteReference")

            // Combine all data into the final JSONObject
            return JSONObject().apply {
                put("resId", resId)
                put("receptionId", receptionId)
                put("userId", userId)
//                put("fetchedData", fetchedData)

                // Add all the extracted fields
                put("applicationnumber", data.optString("applicationnumber"))
                put("name", data.optString("name"))
                put("bankname", data.optString("bankname"))
                put("add1", data.optString("add1"))
//                put("add2", data.optString("add2"))
                put("city", data.optString("city"))
//                put("region", data.optString("region"))
//                put("zip", data.optString("zip"))
//                put("country", data.optString("country"))
                put("phoneNumber",phonenumber)
                put("visitinpresence", data.optString("visitinpresence"))
//                put("reporter", data.optString("reporter"))
//                put("priority", data.optBoolean("priority"))
                put("casetype", data.optString("casetype"))
                put("east", data.optString("east"))
                put("west", data.optString("west"))
                put("north", data.optString("north"))
                put("south", data.optString("south"))
//                put("gfarea", data.optString("gfarea"))
//                put("ffarea", data.optString("ffarea"))
//                put("sfarea", data.optString("sfarea"))
//                put("tfarea", data.optString("tfarea"))
                put("propertyage", data.optString("propertyage"))
                put("landrate", data.optString("landrate"))
                put("occupant", data.optString("occupant"))
                put("landmark", data.optString("landmark"))
                put("roadwidth", data.optString("roadwidth"))
//                put("hightensionline", data.optBoolean("hightensionline"))
//                put("railwayline", data.optBoolean("railwayline"))
//                put("nala", data.optBoolean("nala"))
//                put("river", data.optBoolean("river"))
//                put("pahad", data.optBoolean("pahad"))
//                put("roadcomesunderroadbinding", data.optBoolean("roadcomesunderroadbinding"))
//                put("propertyaccessissue", data.optBoolean("propertyaccessissue"))
//                put("othercheck", data.optBoolean("othercheck"))
//                put("others", data.optString("others"))
                put("remark", data.optString("remark"))
//                put("reporterholdcause", data.optString("reporterholdcause"))

            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CompletedJobEdit,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            return null
        }
    }

    private fun displaySelectedFile(fileUri: Uri,requestCode: Int) {
        var selectedFilesContainer: LinearLayout = findViewById(R.id.selected_files_container)
        if (requestCode== REQUEST_CODE_SELECT_FILE) {
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

    private fun disableFields() {
        binding.applicationNumber.isEnabled = false
        binding.name.isEnabled = false
        binding.bankName.isEnabled = false
        binding.city.isEnabled = false
        binding.phoneNumber.isEnabled = false
        binding.visitInPresence.isEnabled = false
        binding.caseType.isEnabled = false
        binding.add1.isEnabled = false
        binding.add2.isEnabled = false
        binding.region.isEnabled = false
        binding.zip.isEnabled = false
        binding.country.isEnabled = false
        binding.east.isEnabled = false
        binding.west.isEnabled = false
        binding.north.isEnabled = false
        binding.south.isEnabled = false
        binding.gfArea.isEnabled = false
        binding.ffArea.isEnabled = false
        binding.sfArea.isEnabled = false
        binding.tfArea.isEnabled = false
        binding.propertyAge.isEnabled = false
        binding.landRate.isEnabled = false
        binding.occupant.isEnabled = false
        binding.rented.isEnabled = false
        binding.landmark.isEnabled = false
        binding.roadWidth.isEnabled = false
        binding.highTensionLine.isEnabled = false
        binding.railwayLine.isEnabled = false
        binding.nala.isEnabled = false
        binding.river.isEnabled = false
        binding.pahad.isEnabled = false
        binding.roadComesUnderRoadBinding.isEnabled = false
        binding.propertyAccessIssue.isEnabled = false
        binding.otherCheck.isEnabled = false
        binding.others.isEnabled = false
        binding.remark.isEnabled = false
        binding.reporter.isEnabled = false
        binding.priority.isEnabled = false
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
                            this@CompletedJobEdit,
                            "Failed to fetch bank names. Error code: $responseCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompletedJobEdit,
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
                Toast.makeText(this@CompletedJobEdit, "Error parsing data", Toast.LENGTH_SHORT).show()
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


    // Helper function to extract ID from URL
    private fun extractIdFromUrl(url: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun displayJobData(jobData: JSONObject) {
        if (isView == "yes") {
            val saveButton = findViewById<Button>(R.id.saveButton)
            val editViewHeader = findViewById<TextView>(R.id.editViewHeader)
            editViewHeader.setText("Engineer Job View")
            disableFields()
            saveButton.visibility = View.GONE
            findViewById<Button>(R.id.button_select_file).visibility = View.GONE
        }

//        val address=jobData.getString("add1")
        Log.d("phone",jobData.toString())
        binding.applicationNumber.setText(jobData.getString("applicationnumber"))
        binding.name.setText(jobData.getString("name"))
        binding.bankName.setText(jobData.getString("bankname"))
        binding.add1.setText(jobData.getString("add1"))
//        binding.add2.setText(jobData.getString("add2"))
        binding.city.setText(jobData.getString("city"))
//        binding.region.setText(jobData.getString("region"))
//        binding.zip.setText(jobData.getString("zip"))
//        binding.country.setText(jobData.getString("country"))
//        binding.visitingPersonName.setText(jobData.getString("visitinpresence"))
//        binding.reportPersonName.setText(jobData.getString("reporter"))
//        binding.reporter.setText(jobData.getString("reporter"))
//        binding.priority.isChecked = jobData.getBoolean("priority")
        binding.phoneNumber.setText(jobData.getString("phoneNumber"))
        binding.visitInPresence.setText(jobData.getString("visitinpresence"))
        binding.caseType.setText(jobData.getString("casetype"))
        binding.east.setText(jobData.getString("east"))
        binding.west.setText(jobData.getString("west"))
        binding.north.setText(jobData.getString("north"))
        binding.south.setText(jobData.getString("south"))
//        binding.gfArea.setText(jobData.getString("gfarea"))
//        binding.ffArea.setText(jobData.getString("ffarea"))
//        binding.sfArea.setText(jobData.getString("sfarea"))
//        binding.tfArea.setText(jobData.getString("tfarea"))
        binding.propertyAge.setText(jobData.getString("propertyage"))
        binding.landRate.setText(jobData.getString("landrate"))
//        binding.occupant.setText(jobData.getString("occupant"))
        binding.landmark.setText(jobData.getString("landmark"))
        binding.roadWidth.setText(jobData.getString("roadwidth"))
//        binding.highTensionLine.isChecked = jobData.getBoolean("hightensionline")
//        binding.railwayLine.isChecked = jobData.getBoolean("railwayline")
//        binding.nala.isChecked = jobData.getBoolean("nala")
//        binding.river.isChecked = jobData.getBoolean("river")
//        binding.pahad.isChecked = jobData.getBoolean("pahad")
//        binding.roadComesUnderRoadBinding.isChecked = jobData.getBoolean("roadcomesunderroadbinding")
//        binding.propertyAccessIssue.isChecked = jobData.getBoolean("propertyaccessissue")
//        binding.otherCheck.isChecked = jobData.getBoolean("othercheck")
//        binding.others.setText(jobData.getString("others"))
        binding.remark.setText(jobData.getString("remark"))
//        binding.reporterHoldCause.setText(jobData.getString("reporterholdcause"))
    }

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
//        val existingFloorN = findViewById<TextInputEditText>(R.id.flName)
//        existingFloorN?.let {
//            values.add(it.text.toString())
//        }
        values.addAll(floornames.map { it.text.toString() })

        return values
    }
    private fun getFloorD(): List<String> {
        val values = mutableListOf<String>()
//        val existingFloorD = findViewById<TextInputEditText>(R.id.flDetails)
//        existingFloorD?.let {
//            values.add(it.text.toString())
//        }
        values.addAll(floordetails.map { it.text.toString() })

        return values
    }
    private fun getFloorA(): List<String> {
        val values = mutableListOf<String>()
//        val existingFloorA = findViewById<TextInputEditText>(R.id.flArea)
//        existingFloorA?.let {
//            values.add(it.text.toString())
//        }
        values.addAll(floorareas.map { it.text.toString() })

        return values
    }
    private fun getOccupantsValues(): List<String> {
        val values = mutableListOf<String>()
//        val existingOccupant = findViewById<TextInputEditText>(R.id.occupantName)
//        existingOccupant?.let {
//            values.add(it.text.toString())
//        }
        values.addAll(occupantsname.map { it.text.toString() })

        return values
    }

    private fun saveJobData(jobId: String) {
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
        val jobData = JSONObject().apply {
            put("applicationnumber", binding.applicationNumber.text.toString())
            put("name", binding.name.text.toString())
            put("bankname", binding.bankName.text.toString())
            put("add1", binding.add1.text.toString())
//            put("add2", binding.add2.text.toString())
            put("city", binding.city.text.toString())
//            put("region", binding.region.text.toString())
//            put("zip", binding.zip.text.toString())
//            put("country", binding.country.text.toString())
//            put("visitinpresence", binding.visitingPersonName.text.toString())
//            put("reporter", binding.reportPersonName.text.toString())
//            put("priority", binding.priority.isChecked)
            put("occupant", binding.occupancy.selectedItem.toString())
            put("visitinpresence", binding.visitInPresence.text.toString())
            put("casetype", binding.caseType.text.toString())
            put("east", binding.east.text.toString())
            put("west", binding.west.text.toString())
            put("north", binding.north.text.toString())
            put("south", binding.south.text.toString())
            put("occupants", occupants)
            put("floors", floorNamesArray)
            put("floor_details", floorDetailsArray)
            put("floor_areas", floorAreasArray)
            put("dynamicValues", dynamicValues)
//            put("gfarea", binding.gfArea.text.toString())
//            put("ffarea", binding.ffArea.text.toString())
//            put("sfarea", binding.sfArea.text.toString())
//            put("tfarea", binding.tfArea.text.toString())
            put("propertyage", binding.propertyAge.text.toString())
            put("landrate", binding.landRate.text.toString())
//            put("Occupant", binding.occupant.text.toString())
            put("landmark", binding.landmark.text.toString())
            put("roadwidth", binding.roadWidth.text.toString())
            put("edited",true)
//            put("hightensionline", binding.highTensionLine.isChecked)
//            put("railwayline", binding.railwayLine.isChecked)
//            put("nala", binding.nala.isChecked)
//            put("river", binding.river.isChecked)
//            put("pahad", binding.pahad.isChecked)
//            put("roadcomesunderroadbinding", binding.roadComesUnderRoadBinding.isChecked)
//            put("propertyaccessissue", binding.propertyAccessIssue.isChecked)
//            put("othercheck", binding.otherCheck.isChecked)
//            put("others", binding.others.text.toString())
            put("remark", binding.remark.text.toString())
//            put("reporterholdcause", binding.reporterHoldCause.text.toString())
        }

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val result = updateJobData(jobId, jobData)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                if (result) {
                    val uploadSuccess = if (selectedFiles.isNotEmpty()) {
                        uploadFiles(1,receptionId!!, userId!!, resId!!, applicationNo!!)
                    } else true

                    if (uploadSuccess) {
                        uploadFiles(101,receptionId!!, userId!!, resId!!, applicationNo!!)
                        uploadFiles(102,receptionId!!, userId!!, resId!!, applicationNo!!)
                        uploadFiles(103,receptionId!!, userId!!, resId!!, applicationNo!!)
                        uploadFiles(104,receptionId!!, userId!!, resId!!, applicationNo!!)
                        uploadFiles(105,receptionId!!, userId!!, resId!!, applicationNo!!)
                        uploadFiles(106,receptionId!!, userId!!, resId!!, applicationNo!!)
                        Toast.makeText(
                            this@CompletedJobEdit,
                            "Job data updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToDashboard()
                    } else {
                        Toast.makeText(
                            this@CompletedJobEdit,
                            "Job data updated successfully, but files not updated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@CompletedJobEdit,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateJobData(jobId: String, jobData: JSONObject): Boolean {
        return try {
            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)

            if (token.isNullOrEmpty()) {
                println("Token is missing.")
                return false
            }

            val url = URL(Constants.getUrl("api/engineer/$jobId/updateengreport/"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Content-Type", "application/json")

            // Send job data
            connection.outputStream.use { outputStream ->
                outputStream.write(jobData.toString().toByteArray())
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                println("Success! Response Code: $responseCode")
                println("Response Message: $responseMessage")
                true
            } else {
                val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                println("Error! Response Code: $responseCode")
                println("Error Message: $errorMessage")
                Log.d("err","Error Message: $errorMessage")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Exception occurred: ${e.message}")
            false
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
                        displaySelectedFile(fileUri,requestCode)
                    }
                } else if (it.data != null) {
                    val fileUri = it.data!!
                    selectedFiles.add(fileUri)
                    displaySelectedFile(fileUri,requestCode)
                }
            }
        }else if (requestCode == REQUEST_CODE_SITE_FORM && resultCode == Activity.RESULT_OK){
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

    private fun loadAndDisplayFiles(receptionId: String, applicationNumber: String) {
        lifecycleScope.launch {
            val fileList = fetchFilesData(receptionId, applicationNumber,"other")
            Log.d("filelist",fileList.toString())
            if (fileList != null && fileList.isNotEmpty()) {
                displayFiles(fileList,"other")
            } else {
                withContext(Dispatchers.Main) {
                   // Toast.makeText(this@CompletedJobEdit, "No files found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun loadAndDisplaySiteFormFiles(receptionId: String, applicationNumber: String,fileType:String) {
        lifecycleScope.launch {
            val fileList = fetchFilesData(receptionId, applicationNumber,fileType)
            if (fileList != null && fileList.isNotEmpty()) {
                displayFiles(fileList,fileType)
            } else {
                withContext(Dispatchers.Main) {
                    // Toast.makeText(this@CompletedJobEdit, "No files found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    private suspend fun uploadFiles(
        request: Int,
        receptionId: String,
        userId: String,
        resId: String,
        applicationNo: String
    ): Boolean {
        if (selectedFiles.isEmpty()) {
            println("No files selected for upload.")
         //   return true
        }

        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
        val role = "Engineer"
        val platform = "engineer"

        return withContext(Dispatchers.IO) { // Perform network operation in background
            try {
                // Retrieve the token from SharedPreferences
                val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token.isNullOrEmpty()) {
                    println("Token is missing.")
                    return@withContext false
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
                //writeFormField("role", role)
                writeFormField("platform", platform)
                writeFormField("reception_idno", receptionId)
                writeFormField("userdetailsid", userId)
                if (request== REQUEST_CODE_SELECT_FILE) {
                    writeFormField("role", "other")
                    files = selectedFiles
                }
                if (request== REQUEST_CODE_SITE_FORM) {
                    writeFormField("role", "siteForm")
                    files = siteFormFiles
                }
                if (request== REQUEST_CODE_ELEC_METER) {
                    writeFormField("role", "electricityMeter")
                    files = electricMeterFiles
                }
                if (request== REQUEST_CODE_ELEC_BILL) {
                    writeFormField("role", "electricityBill")
                    files = electricBillFiles
                }
                if (request== REQUEST_CODE_KHASRA) {
                    writeFormField("role", "khasra")
                    files = khasraFiles
                }
                if (request== REQUEST_CODE_SITE_PHOTO) {
                    writeFormField("role", "sitePhoto")
                    files = sitePhotosFiles
                }
                if (request== REQUEST_CODE_SITE_REFERENCE) {
                    writeFormField("role", "siteReference")
                    files = siteReferenceFiles
                }
                // Attach files
                files.forEach { fileUri ->
                    try {
                        // Create a temporary file from the URI
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
                    } catch (e: Exception) {
                        println("Error processing file URI: $fileUri - ${e.message}")
                    }
                }

                // End of multipart/form-data
                outputStream.writeBytes("--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                // Read the server response
                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    println("Files updated successfully.")
                    true
                } else {
                    println("File upload failed: $responseCode $responseMessage")
                    false
                }
            } catch (e: Exception) {
                println("Exception occurred while uploading files: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

//    private fun fetchFilesData(receptionId: String, applicationNumber: String): List<Pair<String, String>>? {
//        return try {
//            val url = URL(Constants.getUrl("api/getengfiles/${applicationNumber}/${receptionId}"))
//            val connection = (url.openConnection() as HttpURLConnection).apply {
//                requestMethod = "GET"
//            }
//
//            val responseCode = connection.responseCode
//            val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }
//
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                println("Data fetched successfully: $responseMessage")
//                val jsonResponse = JSONObject(responseMessage)
//                val dataArray = jsonResponse.getJSONArray("data")
//                val fileList = mutableListOf<Pair<String, String>>()
//                for (i in 0 until dataArray.length()) {
//                    val fileObject = dataArray.getJSONObject(i)
//                    val fileName = fileObject.getString("file_name") // Adjust key as per API
//                    val fileId = fileObject.getString("id") // Assuming the ID key is "id"
//                    fileList.add(fileName to fileId)
//                }
//                fileList
//            } else {
//                println("Failed to fetch data: $responseCode $responseMessage")
//                null
//            }
//        } catch (e: Exception) {
//            println("Exception occurred while fetching data: ${e.message}")
//            e.printStackTrace()
//            null
//        }
//    }


    private suspend fun fetchFilesData(receptionId: String, applicationNumber: String,fileType: String): List<Pair<String, String>>? {
        return withContext(Dispatchers.IO) { // Ensures the operation runs on a background thread
            try {
                val url = URL(Constants.getUrl("api/getengfiles/$applicationNumber/$receptionId"))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                }

                val responseCode = connection.responseCode
                val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("Data fetched successfully: $responseMessage")
                    val jsonResponse = JSONObject(responseMessage)
                    val dataArray = jsonResponse.getJSONArray("data")
                    val fileList = mutableListOf<Pair<String, String>>()
                    for (i in 0 until dataArray.length()) {
                        val fileObject = dataArray.getJSONObject(i)
                        val fileName = fileObject.getString("file_name") // Adjust key as per API
                        val fileId = fileObject.getString("id") // Assuming the ID key is "id"
                        if (fileObject.getString("role")==fileType)
                        fileList.add(fileName to fileId)
                    }
                    fileList
                } else {
                    println("Failed to fetch data: $responseCode $responseMessage")
                    null
                }
            } catch (e: Exception) {
                println("Exception occurred while fetching data: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, Dashboard::class.java)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        val userid = sharedPreferences.getString("userid", null)
        val name = sharedPreferences.getString("name", null)

        intent.putExtra("userid", userid)
        intent.putExtra("token", token)
        intent.putExtra("name", name)

        // Start the Dashboard activity
        startActivity(intent)

        finish()
    }


    private fun displayFiles(fileList: List<Pair<String, String>>,fileType: String) {
        var filesContainer: LinearLayout = findViewById(R.id.selected_files_container)
        if (fileType=="siteForm")
            filesContainer = findViewById(R.id.siteFormcontainer)
        if (fileType=="electricityMeter")
            filesContainer = findViewById(R.id.electricityMetercontainer)
        if (fileType=="electricityBill")
            filesContainer = findViewById(R.id.electricityBillcontainer)
        if (fileType=="khasra")
            filesContainer = findViewById(R.id.khasracontainer)
        if (fileType=="sitePhoto")
            filesContainer = findViewById(R.id.sitePhotosContainer)
        if (fileType=="siteReference")
            filesContainer = findViewById(R.id.siteReferenceContainer)
        filesContainer.removeAllViews() // Clear existing views

        fileList.forEach { (fileName, fileId) ->
            val fileView = layoutInflater.inflate(R.layout.item_selected_file, filesContainer, false)
            val textViewFileName: TextView = fileView.findViewById(R.id.text_view_file_name)
            val buttonRemoveFile: ImageButton = fileView.findViewById(R.id.button_remove_file)

            textViewFileName.text = fileName
            buttonRemoveFile.setOnClickListener {
                lifecycleScope.launch {
                    removeFile(fileId) // Call the suspend function within the coroutine
                    filesContainer.removeView(fileView) // Remove the file view from UI
                }
            }

            filesContainer.addView(fileView)
        }
    }


    suspend fun removeFile(fileId: String) {
        val apiUrl = Constants.getUrl("engineer/delete_file/$fileId/")
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CompletedJobEdit, "Authorization token is missing", Toast.LENGTH_SHORT).show()
            }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "token $token")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CompletedJobEdit, "Removed file successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CompletedJobEdit, "Failed to remove file: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CompletedJobEdit, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_FILE = 1
        private const val REQUEST_CODE_SITE_FORM = 101
        private const val REQUEST_CODE_ELEC_METER = 102
        private const val REQUEST_CODE_ELEC_BILL = 103
        private const val REQUEST_CODE_KHASRA = 104
        private const val REQUEST_CODE_SITE_PHOTO = 105
        private const val REQUEST_CODE_SITE_REFERENCE = 106
    }


}
