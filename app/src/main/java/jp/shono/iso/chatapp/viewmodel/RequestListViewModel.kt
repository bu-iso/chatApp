package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.shono.iso.chatapp.model.friend
import jp.shono.iso.chatapp.model.friendRequest
import java.util.*

class RequestListViewModel(application: Application) : AndroidViewModel(application) {
    val requestList = MutableLiveData<MutableList<friendRequest>>()
    val pathList = MutableLiveData<MutableList<String>>()
    lateinit var db:FirebaseFirestore
    var isFirstLoad = true
    var isFullloaded = false
    var uid = ""

    fun initRequestList() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.also {
            uid = it.uid
        }
        db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("friendRequest")
            .whereEqualTo("isChecked", false)
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val newRequestList = mutableListOf<friendRequest>()
                val newPathList = mutableListOf<String>()
                snapshot.documents.forEach { document ->
                    val friendRequest = friendRequest(document.get("uid").toString(), document.get("isChecked").toString().toBoolean(), document.get("isOk").toString().toBoolean(), document.get("datetime").toString().toLong())
                    newRequestList.add(friendRequest)
                    newPathList.add(document.id)
                }
                isFirstLoad = false
                pathList.postValue(newPathList)
                requestList.postValue(newRequestList)
            }
    }

    fun loadRequestList(datetime: Long) {
        if (isFirstLoad || isFullloaded) {
            return
        }
        db.collection("users")
            .document(uid)
            .collection("friendRequest")
            .whereEqualTo("isChecked", false)
            .whereLessThan("datetime", datetime)
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.size.equals(0)) {
                    isFullloaded = true
                    return@addOnSuccessListener
                }
                val newPathList = mutableListOf<String>()
                val newRequestList = mutableListOf<friendRequest>()
                requestList.value?.also {
                    newRequestList.addAll(it)
                }
                pathList.value?.also {
                    newPathList.addAll(it)
                }
                snapshot.documents.forEach { document ->
                    val friendRequest = friendRequest(document.get("uid").toString(), document.get("isChecked").toString().toBoolean(), document.get("isOk").toString().toBoolean(), document.get("datetime").toString().toLong())
                    newRequestList.add(friendRequest)
                    newPathList.add(document.id)
                }
                pathList.postValue(newPathList)
                requestList.postValue(newRequestList)
            }
    }

    fun pushButton(targetUid: String, targetPath: String, isOk:Boolean) {
        db.collection("users")
            .document(uid)
            .collection("friendRequest")
            .document(targetPath)
            .update(mapOf(
                friendRequest::isChecked.name to true,
                friendRequest::isOk.name to isOk
            ))
        if (!isOk)
            return
        val friend = friend(Date().time)
        db.collection("users")
            .document(uid)
            .collection("friends")
            .document(targetUid)
            .set(friend)
        db.collection("users")
            .document(targetUid)
            .collection("friends")
            .document(uid)
            .set(friend)
    }
}