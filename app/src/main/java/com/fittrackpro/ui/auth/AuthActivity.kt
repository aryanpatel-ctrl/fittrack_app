package com.fittrackpro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fittrackpro.databinding.ActivityAuthBinding
import com.fittrackpro.ui.main.MainActivity
import com.fittrackpro.data.local.preferences.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLoginMode = true

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userPreferences.isLoggedIn) {
            navigateToMain()
            return
        }

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(email, password)) performLogin(email, password)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            if (validateInput(email, password)) performRegister(name, email, password)
        }

        binding.tvToggleMode.setOnClickListener { toggleMode() }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) resetPassword(email)
            else binding.tilEmail.error = "Enter your email first"
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        if (isLoginMode) {
            binding.tvTitle.text = "Sign In"
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnRegister.visibility = View.GONE
            binding.tilName.visibility = View.GONE
            binding.tvToggleMode.text = "Don't have an account? Sign Up"
            binding.tvForgotPassword.visibility = View.VISIBLE
        } else {
            binding.tvTitle.text = "Create Account"
            binding.btnLogin.visibility = View.GONE
            binding.btnRegister.visibility = View.VISIBLE
            binding.tilName.visibility = View.VISIBLE
            binding.tvToggleMode.text = "Already have an account? Sign In"
            binding.tvForgotPassword.visibility = View.GONE
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var valid = true
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"; valid = false
        } else binding.tilEmail.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"; valid = false
        } else binding.tilPassword.error = null

        return valid
    }

    private fun performLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                result.user?.let { user ->
                    userPreferences.isLoggedIn = true
                    userPreferences.userId = user.uid
                    userPreferences.userEmail = email
                    userPreferences.userName = user.displayName ?: email.substringBefore("@")
                    navigateToMain()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = e.localizedMessage ?: "Login failed"
            }
    }

    private fun performRegister(name: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                result.user?.let { user ->
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    userPreferences.isLoggedIn = true
                    userPreferences.userId = user.uid
                    userPreferences.userEmail = email
                    userPreferences.userName = name
                    navigateToMain()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = e.localizedMessage ?: "Registration failed"
            }
    }

    private fun resetPassword(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener { Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e ->
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = e.localizedMessage ?: "Failed to send reset email"
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
