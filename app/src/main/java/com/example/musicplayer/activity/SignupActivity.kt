package com.example.musicplayer.activity


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.databinding.ActivitySignupBinding
import com.example.musicplayer.model.User
import com.example.musicplayer.vm.AuthViewModel

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var viewModel: AuthViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //init viewmodel here
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        // Initialising auth object
        binding.btnSignup.setOnClickListener {

//            signUpuser()
            viewModel.register(
                binding.edtEmail.text.toString(),
                binding.edtPassword.text.toString()
            )

        }
        viewModel.isSuccessful.observe(this, Observer {
            //handle
            var message = ""
            if (it == true) {
                viewModel.insertUser(
                    User(
                        null,
                        binding.edtEmail.text.toString(),
                        binding.edtPassword.text.toString()
                    )
                )
                message = "Sign Up Success!"
                startActivity(Intent(this, SigninActivity::class.java))
                finish()
            } else {
                message = "Sign Up Failed!"
            }

            Toast.makeText(application, message, Toast.LENGTH_LONG).show()
        })
    }

    private fun signUpuser() {

        //check pass
        if (binding.edtName.text.toString().isBlank() || binding.edtEmail.text.toString()
                .isBlank() || binding.edtPassword.text.toString()
                .isBlank() || binding.edtPasswordConfim.text.toString().isBlank()
        ) {
            Toast.makeText(this, "Email and Password can't be bank", Toast.LENGTH_LONG).show()
            return
        }
        if (binding.edtPassword.text.toString() != binding.edtPasswordConfim.text.toString()) {
            Toast.makeText(this, "Password and Confirm Password do not match", Toast.LENGTH_LONG)
                .show()
            return
        }


    }

}