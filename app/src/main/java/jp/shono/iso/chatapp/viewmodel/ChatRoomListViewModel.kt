package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.shono.iso.chatapp.model.chatRoom

class ChatRoomListViewModel(application: Application) : AndroidViewModel(application) {
    // mapにすると順序の概念を持ってくれないのでListを二つ用意
    val chatRoomList = MutableLiveData<MutableList<chatRoom>>()
    val idList = MutableLiveData<MutableList<String>>()
    lateinit var db:FirebaseFirestore
    var isFirstLoad = true
    var isFullloaded = false

    fun initChatRoomList() {
        db = FirebaseFirestore.getInstance()
        db.collection("chatRoom")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val newRoomList = mutableListOf<chatRoom>()
                val newIdList = mutableListOf<String>()
                snapshot.documents.forEach { document ->
                    val chatRoom = chatRoom(document.get("uid").toString(), document.get("title").toString(), document.get("datetime").toString().toLong())
                    newRoomList.add(chatRoom)
                    newIdList.add(document.id)
                }
                chatRoomList.postValue(newRoomList)
                idList.postValue(newIdList)
                isFirstLoad = false
            }
    }

    fun loadChatRoom(datetime: Long) {
        if (isFirstLoad || isFullloaded) {
            return
        }
        db.collection("chatRoom")
            .whereLessThan("datetime", datetime)
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.size.equals(0)) {
                    isFullloaded = true
                    return@addOnSuccessListener
                }
                val newRoomList = mutableListOf<chatRoom>()
                val newIdList = mutableListOf<String>()
                chatRoomList.value?.also {
                    newRoomList.addAll(it)
                }
                idList.value?.also {
                    newIdList.addAll(it)
                }
                snapshot.documents.forEach { document ->
                    val chatRoom = chatRoom(
                        document.get("uid").toString(),
                        document.get("title").toString(),
                        document.get("datetime").toString().toLong()
                    )
                    newRoomList.add(chatRoom)
                    newIdList.add(document.id)
                }
                chatRoomList.postValue(newRoomList)
                idList.postValue(newIdList)
            }
    }
}