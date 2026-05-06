//package com.propval.propertyvaluation
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import android.os.AsyncTask
//import android.os.Bundle
//import android.provider.MediaStore
//import android.util.Base64
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.bumptech.glide.Glide
//import org.json.JSONObject
//import java.io.OutputStreamWriter
//import java.net.HttpURLConnection
//import java.net.URL
//import java.util.regex.Pattern
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody
//import okhttp3.Response
//import java.io.File
//
//
//class Profile : AppCompatActivity() {
//
//    private lateinit var profileImage: ImageView
//    private lateinit var userName: TextView
//    private lateinit var userEmail: TextView
//    private lateinit var firstName: EditText
//    private lateinit var lastName: EditText
//    private lateinit var mobileNumber: EditText
//    private lateinit var address: EditText
//    private lateinit var city: EditText
//    private lateinit var state: EditText
//    private lateinit var postalCode: EditText
//    private lateinit var country: EditText
//    private lateinit var role: EditText
//    private lateinit var bankName: EditText
//    private lateinit var bankAccountNumber: EditText
//    private lateinit var ifscCode: EditText
//    private lateinit var updateButton: Button
//
//    private val PICK_IMAGE_REQUEST = 1
//    private var imageUri: Uri? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.engineer_profile)
//
//        profileImage = findViewById(R.id.profile_image)
//        userName = findViewById(R.id.user_name)
//        userEmail = findViewById(R.id.user_email)
//        firstName = findViewById(R.id.first_name)
//        lastName = findViewById(R.id.last_name)
//        mobileNumber = findViewById(R.id.mobile_number)
//        address = findViewById(R.id.address)
//        city = findViewById(R.id.city)
//        state = findViewById(R.id.state)
//        postalCode = findViewById(R.id.postal_code)
//        country = findViewById(R.id.country)
//        role = findViewById(R.id.role)
//        bankName = findViewById(R.id.bank_name)
//        bankAccountNumber = findViewById(R.id.bank_account_number)
//        ifscCode = findViewById(R.id.ifsc_code)
//        updateButton = findViewById(R.id.update_button)
//
//        // Handle back button click
//        val backButton: ImageButton = findViewById(R.id.back_button)
//        backButton.setOnClickListener {
//            finish()
//        }
//
//        // Fetch profile details
//        FetchProfileDetailsTask().execute()
//
//        // Handle profile image click
//        profileImage.setOnClickListener {
//            openImagePicker()
//        }
//
//        // Handle update button click
//        updateButton.setOnClickListener {
//            UpdateProfileDetailsTask().execute()
//        }
//    }
//
//    private fun openImagePicker() {
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
//            imageUri = data.data
//            profileImage.setImageURI(imageUri)
//        }
//    }
//
//    private inner class FetchProfileDetailsTask : AsyncTask<Void, Void, JSONObject?>() {
//        override fun doInBackground(vararg params: Void?): JSONObject? {
//            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//            val token = sharedPreferences.getString("token", null)
//            val userid = sharedPreferences.getString("userid", null)
//            val url = URL(Constants.getUrl("api/userdetail/$userid/getprofiledetail"))
////            val url = URL("http://10.0.2.2:8000/api/userdetail/$userid/getprofiledetail")
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"
//            connection.setRequestProperty("Authorization", "token $token")
//            connection.setRequestProperty("Accept", "application/json")
//
//            return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
//                val response = connection.inputStream.bufferedReader().use { it.readText() }
//                JSONObject(response).getJSONArray("data").getJSONObject(0)
//            } else {
//                null
//            }
//        }
//
//        override fun onPostExecute(result: JSONObject?) {
//            result?.let { data ->
//                userName.text = "${data.getString("first_name")} ${data.getString("last_name")}"
//                userEmail.text = data.getString("user_email")
//                firstName.setText(data.getString("first_name"))
//                lastName.setText(data.getString("last_name"))
//                mobileNumber.setText(data.getString("phone"))
//                address.setText(data.getString("add1"))
//                city.setText(data.getString("city"))
//                state.setText(data.getString("region"))
//                postalCode.setText(data.getString("zip"))
//                country.setText(data.getString("country"))
//                role.setText(data.getString("role"))
//                bankName.setText(data.getString("bankname"))
//                bankAccountNumber.setText(data.getString("bankacno"))
//                ifscCode.setText(data.getString("ifsccode"))
//
//                Glide.with(this@Profile).load(data.getString("profileimage")).into(profileImage)
//
//                // Extract profile ID from URL and save to shared preferences
//                val urlField = data.getString("url")
//                val pattern = Pattern.compile(".*/(\\d+)/$")
//                val matcher = pattern.matcher(urlField)
//                if (matcher.find()) {
//                    val profileId = matcher.group(1)
//                    val editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
//                    editor.putString("profile_id", profileId)
//                    editor.apply()
//                }
//            }
//        }
//    }
////
////    private inner class UpdateProfileDetailsTask : AsyncTask<Void, Void, Boolean>() {
////        override fun doInBackground(vararg params: Void?): Boolean {
////            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
////            val token = sharedPreferences.getString("token", null)
////            val profileId = sharedPreferences.getString("profile_id", null)
////
////            val url = URL("http://10.0.2.2:8000/api/profile/update/$profileId/")
////            val connection = url.openConnection() as HttpURLConnection
////            connection.requestMethod = "PUT"
////            connection.setRequestProperty("Authorization", "token $token")
////            connection.setRequestProperty("Content-Type", "application/json")
////            val userid = sharedPreferences.getString("userid", null)
////            val jsonRequest = JSONObject()
////            jsonRequest.put("user", userid)
////            jsonRequest.put("first_name", firstName.text.toString())
////            jsonRequest.put("last_name", lastName.text.toString())
////            jsonRequest.put("phone", mobileNumber.text.toString())
////            jsonRequest.put("add1", address.text.toString())
////            jsonRequest.put("city", city.text.toString())
////            jsonRequest.put("region", state.text.toString())
////            jsonRequest.put("zip", postalCode.text.toString())
////            jsonRequest.put("country", country.text.toString())
////            jsonRequest.put("role", role.text.toString())
////            jsonRequest.put("bankname", bankName.text.toString())
////            jsonRequest.put("bankacno", bankAccountNumber.text.toString())
////            jsonRequest.put("ifsccode", ifscCode.text.toString())
////
////            // Add profile image if selected
////            imageUri?.let {
////                val imageStream = contentResolver.openInputStream(it)
////                val imageBytes = imageStream?.readBytes()
////                jsonRequest.put("profileimage", imageBytes?.let { it1 -> Base64.encodeToString(it1, Base64.DEFAULT) })
////            }
////
////            connection.outputStream.bufferedWriter().use { it.write(jsonRequest.toString()) }
////
////            return connection.responseCode == HttpURLConnection.HTTP_OK
////        }
////
////        override fun onPostExecute(result: Boolean) {
////            if (result) {
////                Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
////            } else {
////                Toast.makeText(this@Profile, "Failed to update profile", Toast.LENGTH_SHORT).show()
////            }
////        }
////    }
//
//
//    private inner class UpdateProfileDetailsTask : AsyncTask<Void, Void, Boolean>() {
//        private var errorMessage: String? = null
//
//        override fun doInBackground(vararg params: Void?): Boolean {
//            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//            val token = sharedPreferences.getString("token", null)
//            val profileId = sharedPreferences.getString("profile_id", null)
//
//            val client = OkHttpClient()
//            val url = URL(Constants.getUrl("api/profile/update/$profileId/"))
////            val url = "http://10.0.2.2:8000/api/profile/update/$profileId/"
//
//            // Build the multipart body
//            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
//            builder.addFormDataPart("user", sharedPreferences.getString("userid", "") ?: "")
//            builder.addFormDataPart("first_name", firstName.text.toString())
//            builder.addFormDataPart("last_name", lastName.text.toString())
//            builder.addFormDataPart("phone", mobileNumber.text.toString())
//            builder.addFormDataPart("add1", address.text.toString())
//            builder.addFormDataPart("city", city.text.toString())
//            builder.addFormDataPart("region", state.text.toString())
//            builder.addFormDataPart("zip", postalCode.text.toString())
//            builder.addFormDataPart("country", country.text.toString())
//            builder.addFormDataPart("role", role.text.toString())
//            builder.addFormDataPart("bankname", bankName.text.toString())
//            builder.addFormDataPart("bankacno", bankAccountNumber.text.toString())
//            builder.addFormDataPart("ifsccode", ifscCode.text.toString())
//
//            // Add profile image if selected
//            imageUri?.let {
//                val imageFile = File(it.path)
//                val imageRequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)
//                builder.addFormDataPart("profileimage", imageFile.name, imageRequestBody)
//            }
//
//            val requestBody = builder.build()
//
//            val request = Request.Builder()
//                .url(url)
//                .put(requestBody)
//                .addHeader("Authorization", "token $token")
//                .build()
//
//            return try {
//                val response: Response = client.newCall(request).execute()
//                if (!response.isSuccessful) {
//                    errorMessage = "Server responded with code: ${response.code}, message: ${response.body?.string()}"
//                    false
//                } else {
//                    true
//                }
//            } catch (e: Exception) {
//                errorMessage = "Request failed: ${e.message}"
//                false
//            }
//        }
//
//        override fun onPostExecute(result: Boolean) {
//            if (result) {
//                Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this@Profile, "Failed to update profile: $errorMessage", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//
//}
package com.propval.propertyvaluation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.util.regex.Pattern

class Profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var profileFrame: FrameLayout
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var mobileNumber: TextInputEditText
    private lateinit var address: TextInputEditText
    private lateinit var city: TextInputEditText
    private lateinit var state: TextInputEditText
    private lateinit var postalCode: TextInputEditText
    private lateinit var country: TextInputEditText
    private lateinit var role: TextInputEditText
    private lateinit var bankName: TextInputEditText
    private lateinit var bankAccountNumber: TextInputEditText
    private lateinit var ifscCode: TextInputEditText
    private lateinit var updateButton: Button
    private lateinit var progressBar: ProgressBar


    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engineer_profile)

        progressBar = findViewById(R.id.profileProgressBar)
        // Initialize views
        profileImage = findViewById(R.id.profile_image)
        userName = findViewById(R.id.user_name)
        userEmail = findViewById(R.id.user_email)
        firstName = findViewById(R.id.first_name)
        lastName = findViewById(R.id.last_name)
        mobileNumber = findViewById(R.id.mobile_number)
        address = findViewById(R.id.address)
        city = findViewById(R.id.city)
        state = findViewById(R.id.state)
        postalCode = findViewById(R.id.postal_code)
        country = findViewById(R.id.country)
        role = findViewById(R.id.role)
        bankName = findViewById(R.id.bank_name)
        bankAccountNumber = findViewById(R.id.bank_account_number)
        ifscCode = findViewById(R.id.ifsc_code)
        updateButton = findViewById(R.id.update_button)
        profileFrame = findViewById(R.id.profileFrame)
//        profileFrame.visibility=View.GONE

        // Back button click handler
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Fetch profile details
        FetchProfileDetailsTask().execute()

        // Profile image click handler
        profileImage.setOnClickListener {
            openImagePicker()
        }

        // Update button click handler
        updateButton.setOnClickListener {
            UpdateProfileDetailsTask().execute()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            profileImage.load(imageUri) // Use Coil to load selected image into profileImage

        }
    }

    private inner class FetchProfileDetailsTask : AsyncTask<Void, Void, JSONObject?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE // Show progress bar before fetching data
        }

        override fun doInBackground(vararg params: Void?): JSONObject? {
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            val userid = sharedPreferences.getString("userid", null)
            val url = URL(Constants.getUrl("api/userdetail/$userid/getprofiledetail"))
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Accept", "application/json")

            return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response).getJSONArray("data").getJSONObject(0)
            } else {
                null
            }
        }

        override fun onPostExecute(result: JSONObject?) {
            progressBar.visibility = View.GONE // Hide progress bar after fetching data
//            profileFrame.visibility = View.VISIBLE
            result?.let { data ->
                userName.text = "${data.getString("first_name")} ${data.getString("last_name")}"
                userEmail.text = data.getString("user_email")
                firstName.setText(data.getString("first_name"))
                lastName.setText(data.getString("last_name"))
                mobileNumber.setText(data.getString("phone"))
                address.setText(data.getString("add1"))
                city.setText(data.getString("city"))
                state.setText(data.getString("region"))
                postalCode.setText(data.getString("zip"))
                country.setText(data.getString("country"))
                role.setText(data.getString("role"))
                bankName.setText(data.getString("bankname"))
                bankAccountNumber.setText(data.getString("bankacno"))
                ifscCode.setText(data.getString("ifsccode"))

                // Load profile image using Coil
                val imageUrl = data.getString("profileimage")
                if (imageUrl.isNotEmpty()) {
                    profileImage.load(imageUrl) // Coil loads the image from URL
                }

                // Extract and save profile ID
                val urlField = data.getString("url")
                val pattern = Pattern.compile(".*/(\\d+)/$")
                val matcher = pattern.matcher(urlField)
                if (matcher.find()) {
                    val profileId = matcher.group(1)
                    val editor = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                    editor.putString("profile_id", profileId)
                    editor.apply()
                }
            }
        }
    }

    private inner class UpdateProfileDetailsTask : AsyncTask<Void, Void, Boolean>() {
        private var errorMessage: String? = null
        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE // Show progress bar before updating data
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            val profileId = sharedPreferences.getString("profile_id", null)

            val client = OkHttpClient()
            val url = Constants.getUrl("api/profile/update/$profileId/")

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            builder.addFormDataPart("user", sharedPreferences.getString("userid", "") ?: "")
            builder.addFormDataPart("first_name", firstName.text.toString())
            builder.addFormDataPart("last_name", lastName.text.toString())
            builder.addFormDataPart("phone", mobileNumber.text.toString())
            builder.addFormDataPart("add1", address.text.toString())
            builder.addFormDataPart("city", city.text.toString())
            builder.addFormDataPart("region", state.text.toString())
            builder.addFormDataPart("zip", postalCode.text.toString())
            builder.addFormDataPart("country", country.text.toString())
            builder.addFormDataPart("role", role.text.toString())
            builder.addFormDataPart("bankname", bankName.text.toString())
            builder.addFormDataPart("bankacno", bankAccountNumber.text.toString())
            builder.addFormDataPart("ifsccode", ifscCode.text.toString())

            // Handle image upload
            imageUri?.let { uri ->
                try {
                    // Open InputStream from the Uri using ContentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() // Read image data into a byte array
                    inputStream?.close()

                    // Convert the byte array to RequestBody and add it to the form
                    bytes?.let {
                        val imageRequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), it)
                        builder.addFormDataPart("profileimage", "profile_image.jpg", imageRequestBody)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "Failed to read image: ${e.message}"
                    return false
                }
            }

            val requestBody = builder.build()
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .addHeader("Authorization", "token $token")
                .build()

            return try {
                val response: Response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    errorMessage = "Server error: ${response.code}, ${response.body?.string()}"
                    false
                } else {
                    true
                }
            } catch (e: Exception) {
                errorMessage = "Request failed: ${e.message}"
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            progressBar.visibility = View.GONE // Hide progress bar after updating data

            if (result) {
                Toast.makeText(this@Profile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@Profile, "Failed to update profile: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

}
