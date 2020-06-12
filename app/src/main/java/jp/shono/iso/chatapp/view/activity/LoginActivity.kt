package jp.shono.iso.chatapp.view.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.model.userData
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private var isCreateAccount = false
    private  lateinit var db:FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        isCreateAccount = intent.getBooleanExtra("isCreate", false)
        if (isCreateAccount) {
            nameText.visibility = View.VISIBLE
            loginButton.text = "登録"
        }

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // アカウント作成処理のリスナー
        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser

                if (isCreateAccount) {
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()
                    user?.also {
                        val userdata = userData(it.uid, name)
                        db.collection("users")
                            .document()
                            .set(userdata)
                    }

                    // 表示名をPrefarenceに保存する
                    saveName(name)
                } else {
                    user?.also {
                        db.collection("users")
                            .whereEqualTo(userData::uid.name, user.uid)
                            .limit(1)
                            .get()
                            .addOnSuccessListener {documents ->
                                saveName("${documents.first().data?.get("name")}")
                                Log.d("firebasetest", "${documents.first().data?.get("name")}")
                            }
                            .addOnFailureListener {
                                Log.d("firebasetest", it.toString())
                            }
                    }
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // UIの準備
        title = "アカウント作成"

        loginButton.setOnClickListener { v ->
            if (isCreateAccount) {
                // キーボードが出てたら閉じる
                val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                val name = nameText.text.toString()

                if (email.length != 0 && password.length >= 6 && name.length != 0) {
                    createAccount(email, password)
                } else {
                    // エラーを表示する
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
                }
            } else {
                // キーボードが出てたら閉じる
                val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

                val email = emailText.text.toString()
                val password = passwordText.text.toString()

                if (email.length != 0 && password.length >= 6) {
                    login(email, password)
                } else {
                    // エラーを表示する
                    Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // アカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString("nameKey", name)
        editor.commit()
    }
}
