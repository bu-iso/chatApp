package jp.shono.iso.chatapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import jp.shono.iso.chatapp.model.chatRoom

class ChatRoomListViewModel(application: Application) : AndroidViewModel(application) {
    // mapにすると順序の概念を持ってくれないのでListを二つ用意
    val chatRoomList = MutableLiveData<MutableList<chatRoom>>()
    val idList = MutableLiveData<MutableList<String>>()
    lateinit var db:FirebaseFirestore

    fun initChatRoomList() {
        db = FirebaseFirestore.getInstance()
        db.collection("chatRoom")
            .addSnapshotListener { snapshot,e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    chatRoomList.value?.clear()
                    idList.value?.clear()
                    val newRoomList = mutableListOf<chatRoom>()
                    val newIdList = mutableListOf<String>()
                    snapshot.documents.forEach { document ->
                        val chatRoom = chatRoom(document.get("uid").toString(), document.get("title").toString(), document.get("datetime").toString().toLong())
                        newRoomList.add(chatRoom)
                        newIdList.add(document.id)
                    }
                    chatRoomList.postValue(newRoomList)
                    idList.postValue(newIdList)
                }
            }
    }
}