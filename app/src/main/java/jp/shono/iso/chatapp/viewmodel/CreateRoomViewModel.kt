package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.shono.iso.chatapp.model.chatRoom
import java.util.*

class CreateRoomViewModel(application: Application) : AndroidViewModel(application) {
    val roomTitle = MutableLiveData<String>()
    val isSuccess = MutableLiveData<Boolean>().apply { value = false }
    lateinit var db: FirebaseFirestore

    fun createRoom() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.also {
            val room = chatRoom(it.uid, roomTitle.value.toString(), Date().time)
            db = FirebaseFirestore.getInstance()
            db.collection("chatRoom")
                .document()
                .set(room)
                .addOnSuccessListener {
                    isSuccess.value = true
                }
        }
    }
}