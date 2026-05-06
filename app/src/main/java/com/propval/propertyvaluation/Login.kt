package com.propval.propertyvaluation

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Login : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize the ProgressBar
        progressBar = findViewById(R.id.progressBar)

        // Check if user is already logged in
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        val userid = sharedPreferences.getString("userid", null)
        val name = sharedPreferences.getString("name", null)
//        val userName = sharedPreferences.getString("userid", null)
        if (token != null && userid != null) {
            // User is already logged in, navigate to Dashboard
            val intent = Intent(this, Dashboard::class.java).apply {
                putExtra("userid", userid)
                putExtra("token", token)
                putExtra("name", name)
            }
            startActivity(intent)
            finish()
        }

        val usernameEditText = findViewById<TextInputEditText>(R.id.username)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password)
        val loginButton = findViewById<MaterialButton>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val url = Constants.getUrl("api/loginapi/")
            // Show the loader before starting the login task
            progressBar.visibility = VISIBLE
            LoginTask(username, password).execute(url)
        }

        val forgotPasswordButton: TextInputLayout = findViewById(R.id.forgotPasswordButton)
        val resetPasswordButton = findViewById<MaterialButton>(R.id.reset_password)
        val forgotPasswordEmail = findViewById<TextInputLayout>(R.id.forgotPassowrdEmail)
        val backtologinButton = findViewById<MaterialButton>(R.id.backtoLogin)
        val forgotPasswordConfirmationText: TextView = findViewById(R.id.forgotPasswordConfirmationText)
        val forgotPassEmail: TextView = findViewById(R.id.forgotPassEmail)

        forgotPasswordButton.setOnClickListener{
            usernameEditText.visibility = GONE
            passwordEditText.visibility = GONE
            loginButton.visibility = GONE
            forgotPasswordButton.visibility = GONE
            resetPasswordButton.visibility = VISIBLE
            forgotPasswordEmail.visibility = VISIBLE
            forgotPassEmail.visibility = VISIBLE
            backtologinButton.visibility = VISIBLE



        }

        backtologinButton.setOnClickListener{
            usernameEditText.visibility = VISIBLE
            passwordEditText.visibility = VISIBLE
            loginButton.visibility = VISIBLE
            forgotPasswordButton.visibility = VISIBLE
            resetPasswordButton.visibility = GONE
            forgotPasswordEmail.visibility = GONE
            backtologinButton.visibility = GONE
            forgotPasswordConfirmationText.visibility = GONE
        }

        forgotPasswordConfirmationText.visibility = View.GONE

        resetPasswordButton.setOnClickListener {
            val email = forgotPassEmail.text.toString().trim()

            // Validate email format (optional)
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show the loader before starting the reset password task
            progressBar.visibility = VISIBLE

            // Send the POST request
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(Constants.getUrl("api/request-reset-password/"))
//                    val url = URL("http://10.0.2.2:8000/api/request-reset-password/")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")

                    // Create JSON body with email
                    val jsonObject = JSONObject().apply {
                        put("email", email)
                    }

                    // Write the JSON body to the request
                    connection.outputStream.write(jsonObject.toString().toByteArray())
                    connection.outputStream.flush()
                    connection.outputStream.close()

                    // Check if the response is successful
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = GONE // Hide loader on success
                            // Update confirmation text and make it visible on success
                            forgotPasswordConfirmationText.text = "Sent reset password link to your email."
                            forgotPasswordConfirmationText.visibility = View.VISIBLE
                            resetPasswordButton.visibility = View.GONE
                            forgotPassEmail.visibility = View.GONE
                        }
                    } else {
                        // Handle the failure case
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = GONE // Hide loader on failure
                            forgotPasswordConfirmationText.text = "Failed to send reset password link. Please try again."
                            forgotPasswordConfirmationText.visibility = View.VISIBLE
                            resetPasswordButton.visibility = View.GONE
                            forgotPassEmail.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle the exception case
                    withContext(Dispatchers.Main) {
                        forgotPasswordConfirmationText.text = "An error occurred. Please try again."
                        forgotPasswordConfirmationText.visibility = View.VISIBLE
                        resetPasswordButton.visibility = View.GONE
                    }
                }
            }
        }



    }

    private inner class LoginTask(val username: String, val password: String) : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String?): String {
            return try {
                val url = URL(urls[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val jsonInputString = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonInputString.toString())
                    writer.flush()
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                "{\"success\": false, \"message\": \"Error on server\"}"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progressBar.visibility = GONE // Hide loader after task completes
            try {

                val jsonResponse = JSONObject(result)
                val success = jsonResponse.getBoolean("success")
                val message = jsonResponse.getString("message")
                Log.d("user",jsonResponse.toString())
                if (success) {
                    val userid = jsonResponse.getInt("userid")
                    val token = jsonResponse.getString("token")
                    val name = jsonResponse.getString("name")
                    // Save token and userid in SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("token", token)
                        putString("userid", userid.toString())
                        putString("name",name.toString())
                        apply()
                    }

                    Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Login, Dashboard::class.java).apply {
                        putExtra("userid", userid.toString())
                        putExtra("token", token)
                        putExtra("name",name.toString())
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Login, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(this@Login, "Error parsing response", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
