package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.shono.iso.chatapp.model.chatMessage
import jp.shono.iso.chatapp.model.chatRoom
import jp.shono.iso.chatapp.model.friendRequest
import jp.shono.iso.chatapp.model.userData
import java.util.*

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    val buttonText = MutableLiveData<String>()
    val isStranger = MutableLiveData<Boolean>().apply { value = false}
    val isSend = MutableLiveData<Boolean>().apply { value = false}
    val name = MutableLiveData<String>()
    lateinit var db: FirebaseFirestore
    var targetUid = ""
    var uid = ""

    fun initProfile() {
        db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        user?.also {
            uid = it.uid
        }
        loadUserData()
    }

    fun loadUserData() {
        Log.d("testinfo", targetUid)
        db.collection("users")
            .document(targetUid)
            .get()
            .addOnSuccessListener { snapshot ->
                name.postValue(snapshot.data?.get(userData::name.name).toString())
            }
        if (targetUid.equals(uid)) {
            isStranger.postValue(false)
            buttonText.postValue("あなたです")
        }

        db.collection("users")
            .document(uid)
            .collection("friends")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.forEach {
                    if (it.data.get("uid") == targetUid) {
                        isStranger.postValue(false)
                        buttonText.postValue("友達です")
                        return@addOnSuccessListener
                    }
                }
                isStranger.postValue(true)
                buttonText.postValue("友達申請する")
            }.addOnFailureListener {
                isStranger.postValue(true)
                buttonText.postValue("友達申請する")
            }

        db.collection("users")
            .document(targetUid)
            .collection("friendRequest")
            .whereEqualTo("uid", uid)
            .whereEqualTo("isChecked", false)
            .limit(1)
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    isStranger.postValue(false)
                    buttonText.postValue("申請済み")
                }
            }
    }
    fun sendFriendRequest() {

        val request = friendRequest(uid, datetime = Date().time)
        db.collection("users")
            .document(targetUid)
            .collection("friendRequest")
            .document()
            .set(request)
            .addOnSuccessListener {
                isSend.postValue(true)
                buttonText.postValue("送りました")
            }
    }
}