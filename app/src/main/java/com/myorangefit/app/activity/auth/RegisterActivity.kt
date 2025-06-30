package com.myorangefit.app.activity.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.myorangefit.app.R
import com.myorangefit.app.activity.ActivityManager
import com.myorangefit.app.activity.MainActivity
import com.myorangefit.app.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var options: Bundle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityManager.add(this)

        auth = FirebaseAuth.getInstance()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        options = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.fade_in,
            R.anim.fade_out
        ).toBundle()

        binding.register.setOnClickListener {
            register()
        }

        binding.logIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java), options)
            ActivityManager.finish(this)
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                enableButton()
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {

            }
        }

        val textWatcherPW = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val e = enableButton()
                ShowDialog(e)
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {

            }
        }

        binding.usernameText.addTextChangedListener(textWatcher)
        binding.emailText.addTextChangedListener(textWatcher)
        binding.passwordText.addTextChangedListener(textWatcherPW)
        binding.confirmPasswordText.addTextChangedListener(textWatcherPW)
    }

    private fun register() {
        val username = binding.usernameText.text.toString().trim()
        val email    = binding.emailText.text.toString().trim()
        val password = binding.passwordText.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Aggiorna displayName e vai alla Main
                    auth.currentUser?.updateProfile(
                        userProfileChangeRequest { displayName = username }
                    )?.addOnCompleteListener {
                        goMain()
                    }
                } else {
                    // Gestione degli errori specifici
                    val e = task.exception
                    val msg = when (e) {
                        is FirebaseAuthInvalidCredentialsException ->
                            "Email address is invalid. Please enter a valid email."
                        is FirebaseAuthUserCollisionException ->
                            "This email is already registered. Please log in or use another email."
                        is FirebaseNetworkException ->
                            "Network error. Please check your internet connection."
                        else ->
                            "Registration failed. Please try again."
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java), options)
        ActivityManager.finishAll()
    }

    private fun ShowDialog(e: String) {
        if (
            binding.confirmPasswordText.text.toString() !=
            binding.passwordText.text.toString() ||
            binding.passwordText.text.toString().length < 8
            ) {
            binding.dialogPw.text = e
            binding.dialogPw.visibility = View.VISIBLE
        } else {
            binding.dialogPw.visibility = View.GONE
        }
    }

    private fun enableButton() : String {
        val username = binding.usernameText.text.toString()
        val email    = binding.emailText.text.toString()
        val pwd      = binding.passwordText.text.toString()
        val confirm  = binding.confirmPasswordText.text.toString()

        val valid = username.isNotBlank() &&
                email.isNotBlank() &&
                email.contains('@') &&
                pwd.length >= 8 &&
                confirm == pwd

        binding.register.isEnabled   = valid
        binding.register.isClickable = valid

        return when {
            !valid && confirm != pwd                -> "Password doesn't match"
            !valid && pwd.length < 8                -> "Password too short"
            !valid && !email.contains('@')     -> "Email not valid"
            else                                    -> ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.finish(this)
    }
}