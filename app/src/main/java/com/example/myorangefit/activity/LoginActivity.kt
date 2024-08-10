package com.example.myorangefit.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myorangefit.R
import com.example.myorangefit.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityManager.add(this)

        setContentView(R.layout.activity_login)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.setOnClickListener {
            logIn()
        }

        binding.signUp.setOnClickListener {

        }
        binding.recuperoPw.setOnClickListener {

        }

        binding.emailText.addTextChangedListener(object : TextWatcher {
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
        })

        binding.passwordText.addTextChangedListener(object : TextWatcher {
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
        })
    }

    private fun logIn(){
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun enableButton(){
        if(
            binding.emailText.text.toString() != "" &&
            binding.passwordText.text.toString() != "")
        {
            binding.login.isEnabled = true
            binding.login.isClickable = true
        } else {
            binding.login.isEnabled = false
            binding.login.isClickable = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.remove(this)
    }
}