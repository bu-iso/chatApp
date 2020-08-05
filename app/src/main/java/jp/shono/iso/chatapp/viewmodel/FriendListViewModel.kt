package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.shono.iso.chatapp.model.friend

class FriendListViewModel(application: Application) : AndroidViewModel(application) {
    val friendList = MutableLiveData<MutableList<friend>>()
    val uidList = mutableListOf<String>()
    val nameList = MutableLiveData<MutableList<String>>()
    lateinit var db: FirebaseFirestore
    var isFirstLoad = true
    var isFullloaded = false
    var uid = ""

    fun initFriendList() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.also {
            uid = it.uid
        }
        db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("friends")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val newUidList = mutableListOf<String>()
                val newFriendList = mutableListOf<friend>()
                snapshot.documents.forEach { document ->
                    val friend = friend(document.get("datetime").toString().toLong())
                    newUidList.add(document.id)
                    newFriendList.add(friend)
                }
                isFirstLoad = false
                uidList.addAll(newUidList)
                friendList.postValue(newFriendList)
            }
    }

    fun loadName() {
        val newNameList = mutableListOf<String>()
        nameList.value?.also {
            newNameList.addAll(it)
        }
        uidList.forEach {
            db.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener {
                    newNameList.add(it.data?.get("name").toString())
                    nameList.postValue(newNameList)
                }
        }
    }

    fun loadFriendList(datetime: Long) {
        if (isFirstLoad || isFullloaded) {
            return
        }
        db.collection("users")
            .document(uid)
            .collection("friends")
            .whereLessThan("datetime", datetime)
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.size.equals(0)) {
                    isFullloaded = true
                    return@addOnSuccessListener
                }
                val newFriendList = mutableListOf<friend>()
                val newUidList = mutableListOf<String>()
                friendList.value?.also {
                    newFriendList.addAll(it)
                }
                snapshot.documents.forEach { document ->
                    val friend = friend(document.get("datetime").toString().toLong())
                    newUidList.add(document.id)
                    newFriendList.add(friend)
                }
                friendList.postValue(newFriendList)
            }
    }
}