package jp.shono.iso.chatapp.view.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import jp.shono.iso.chatapp.R
import kotlinx.android.synthetic.main.activity_login.loginButton
import kotlinx.android.synthetic.main.activity_login_menu.*

class LoginMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_menu)

        val intent = Intent(applicationContext, LoginActivity::class.java)
        loginButton.setOnClickListener {
            intent.putExtra("isCreate", false)
            startActivity(intent)
        }
        createButton.setOnClickListener {
            intent.putExtra("isCreate", true)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            onBackPressed()
        }
    }
}
