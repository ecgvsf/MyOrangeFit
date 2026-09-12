package com.myorangefit.app.activity.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.myorangefit.app.activity.ActivityManager
import com.myorangefit.app.activity.MainActivity
import com.myorangefit.app.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.myorangefit.app.R

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var options: Bundle
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        ActivityManager.add(this)

        // Inizializza FirebaseAuth
        auth = FirebaseAuth.getInstance()

        options = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.fade_in,
            R.anim.fade_out
        ).toBundle()

        auth.currentUser?.let {
            goMain()
            return
        }

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.login.setOnClickListener { logInWithEmail() }
        binding.loginGoogle.setOnClickListener { logInWithGoogle() }
        binding.signUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java), options)
            ActivityManager.finish(this)
        }
        binding.recuperoPw.setOnClickListener {
            startActivity(Intent(this, RecoverPwActivity::class.java), options)
            ActivityManager.finish(this)
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = enableButton()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.emailText.addTextChangedListener(textWatcher)
        binding.passwordText.addTextChangedListener(textWatcher)
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java), options)
        ActivityManager.finishAll()
    }

    private fun logInWithEmail() {
        val email = binding.emailText.text.toString().trim()
        val pwd = binding.passwordText.text.toString()

        auth.signInWithEmailAndPassword(email, pwd)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goMain()
                } else {
                    val e = task.exception
                    if (e is FirebaseAuthException) {
                        // è una eccezione di Auth: controlliamo il codice
                        when (e.errorCode) {
                            "ERROR_USER_NOT_FOUND" ->
                                showToast("No account found with this email.")

                            "ERROR_INVALID_EMAIL" ->
                                showToast("Email address is badly formatted.")

                            "ERROR_WRONG_PASSWORD" ->
                                showToast("Incorrect password.")

                            "ERROR_USER_DISABLED" ->
                                showToast("Your account has been disabled.")

                            else ->
                                showToast("Authentication failed: ${e.localizedMessage}")
                        }
                    } else {
                        // non è una FirebaseAuthException
                        showToast("Unexpected error: ${e?.localizedMessage}")
                    }

                }
            }
    }
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun logInWithGoogle() {
        val signInIntent = googleClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val acct = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            goMain()
                        } else {
                            val e = authTask.exception
                            val msg = when (e) {
                                is FirebaseAuthUserCollisionException ->
                                    "This email is already registered with a different sign-in method."
                                is FirebaseAuthInvalidCredentialsException ->
                                    "Google credentials are invalid."
                                is FirebaseNetworkException ->
                                    "Network error during Google sign-in."
                                else ->
                                    "Google authentication failed. Please try again."
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                // Qui usiamo i codici giusti:
                val msg = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                        "Sign-in was cancelled by the user."        // codice 12501 :contentReference[oaicite:0]{index=0}
                    CommonStatusCodes.DEVELOPER_ERROR ->
                        "Configuration error. Please check your SHA-1 and client ID."  // codice 10 :contentReference[oaicite:1]{index=1}
                    else ->
                        "Google sign-in error (code ${e.statusCode}). Please try again."
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableButton() {
        val email = binding.emailText.text.toString()
        val pwd = binding.passwordText.text.toString()
        val enabled = email.contains('@') && pwd.length >= 8
        binding.login.isEnabled = enabled
        binding.login.isClickable = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityManager.finish(this)
    }
}
