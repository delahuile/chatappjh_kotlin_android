package com.example.chatappjh

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.chatappjh.models.UsernameUID
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    val RC_SIGN_IN: Int = 1
    lateinit var signInClient: GoogleSignInClient
    lateinit var signInOptions: GoogleSignInOptions
    private lateinit var auth: FirebaseAuth

    private lateinit var githubAuthProvider: OAuthProvider.Builder

    private lateinit var constraintlayout: ConstraintLayout
    private lateinit var animationDrawable: AnimationDrawable

    var userInDatabase = false

    companion object {
        val TAG = "LoginActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Starts animation of the login layout background
        constraintlayout = findViewById((R.id.login_layout))
        animationDrawable = constraintlayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        button_login.setOnClickListener {
            val email = edit_login_email.text.toString()
            val password = edit_login_password.text.toString()

            Log.d("LoginActivity", "email is " + email)
            Log.d( "LoginActivity", "password is $password")
            Log.d("LoginActivity", "Shows login activity")

            emailLogin(email, password)
        }

        button_google_login.setOnClickListener {
            Log.d("LoginActivity", "redirect to googlelogin")
            Log.d("SignupActivity", "redirect to googlesignup")
            val loginIntent: Intent = signInClient.signInIntent
            startActivityForResult(loginIntent, RC_SIGN_IN)
        }

        auth = FirebaseAuth.getInstance()
        setupGoogleLogin()

        githubAuthProvider = OAuthProvider.newBuilder("github.com")
        githubAuthProvider.addCustomParameter("login", "your-email@gmail.com")

        button_github_login.setOnClickListener {
            Log.d("Loginactivity", "redirect to githublogin")
            Log.d("SignupActivity", "redirect to githubsignup")
            checkPendingGithubLogin()
        }

        button_redirect_to_signup.setOnClickListener {
            Log.d("LoginActivity", "Redirects to login activity")
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

    }

    private fun emailLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please, enter mail and password", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener

                Log.d("LoginActivity", "Logged in successfully")

                // redirecting  to chat after successful login
                val intent = Intent(this, ChatActivity::class.java)

                //clears off the activity stack
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show()
            }



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    googleFirebaseAuth(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun googleFirebaseAuth(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                checkIfUserIsAlreadyInDatabase()
            } else {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupGoogleLogin() {
        signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        signInClient = GoogleSignIn.getClient(this, signInOptions)
    }

    private fun checkIfUserIsAlreadyInDatabase(){

        FirebaseFirestore.getInstance().collection("userID_Names")
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                if (task.isSuccessful) {
                    Log.d(SignupActivity.TAG, "tag result successful")
                    for (document in task.result!!) {
                        val user = document.toObject(UsernameUID::class.java)
                        Log.d(TAG, "user.uid is " + user.uid)
                        Log.d(TAG, "FirebaseAuth UID is " + FirebaseAuth.getInstance().uid)
                        if (user?.uid == FirebaseAuth.getInstance().uid) {
                            userInDatabase = true
                            Log.d(
                                SignupActivity.TAG,
                                "User with uid ${FirebaseAuth.getInstance().uid} is already in database"
                            )
                        }
                    }
                    pushUserToUsernameUID()
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            })
    }

    private fun pushUserToUsernameUID() {
        val name = ""
        val fromID = FirebaseAuth.getInstance().uid

        if (!userInDatabase){
            if (fromID == null) return

            val message = UsernameUID(name, fromID)

            FirebaseFirestore.getInstance().collection("userID_Names").add(message)
                .addOnCompleteListener {
                    Log.d(SignupActivity.TAG, "usernameUID sent into the database successfully")
                    val intent = Intent(this, SetUsernameFromSignupActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Log.d(SignupActivity.TAG, "failed to send usernameUID into the database")
                }

        } else {
            val intent = Intent(this, ChatActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun checkPendingGithubLogin(){


        if (auth.getPendingAuthResult() == null) {
            // There's no pending result so you need to start the sign-in flow.
            // See below.
            startGithubSigninFlow()
        } else {
            val pendingResultTask: Task<AuthResult> = auth.getPendingAuthResult()
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                .addOnSuccessListener {
                    // User is signed in.
                    // IdP data available in
                    Log.d(
                        SignupActivity.TAG,
                        "it.getAdditionalUserInfo().getProfile() is ${
                            it.getAdditionalUserInfo().getProfile()
                        }"
                    )
                    // The OAuth access token can also be retrieved:
                    // authResult.getCredential().getAccessToken().
                    checkIfUserIsAlreadyInDatabase()
                }
                .addOnFailureListener {
                    Log.d(SignupActivity.TAG, "Failed to complete pending github sign-up")
                    // Handle failure.
                }
        }
    }



    private fun startGithubSigninFlow(){
        auth
            .startActivityForSignInWithProvider( /* activity= */this, githubAuthProvider.build())
            .addOnSuccessListener(
                OnSuccessListener<AuthResult?> {
                    // User is signed in.
                    // IdP data available in
                    // authResult.getAdditionalUserInfo().getProfile().
                    // The OAuth access token can also be retrieved:
                    // authResult.getCredential().getAccessToken().
                    checkIfUserIsAlreadyInDatabase()
                })
            .addOnFailureListener(
                OnFailureListener {
                    // Handle failure.
                    Log.d(SignupActivity.TAG, "Failed to complete github sign-up")
                })
    }
}