package jp.shono.iso.chatapp.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import jp.shono.iso.chatapp.model.chatMessage
import java.io.ByteArrayOutputStream
import java.util.*

class ChatRoomViewModel(application: Application) : AndroidViewModel(application) {
    val editMessage = MutableLiveData<String>(null)
    var roomId = ""
    var pictureUri: Uri? = null
    lateinit var uid: String
    var isFirstLoad = true
    var isFullLoaded = false
    val chatMessageList = MutableLiveData<MutableList<chatMessage>>()
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun initSetting() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.also {
            uid = it.uid
        }
        db.collection("chatRoom")
            .document(roomId)
            .collection("messages")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && !isFirstLoad) {
                    val newMessageList = mutableListOf<chatMessage>()
                    chatMessageList.value?.also {
                        newMessageList.addAll(it)
                    }
                    // changesは新規メッセージのみであることが前提
                    snapshot.documentChanges.forEach {
                        if (it.type != DocumentChange.Type.ADDED) {
                            return@forEach
                        }
                        val document = it.document
                        val chatMessage = chatMessage(
                            document.get(chatMessage::uid.name).toString(),
                            document.get(chatMessage::text.name).toString(),
                            document.get(chatMessage::isImage.name).toString().toBoolean(),
                            document.get(chatMessage::datetime.name).toString().toLong()
                        )
                        newMessageList.add(chatMessage)
                    }
                    newMessageList.sortBy { it.datetime }
                    chatMessageList.postValue(newMessageList)
                }
            }
        db.collection("chatRoom")
            .document(roomId)
            .collection("messages")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener {
                val newMessageList = mutableListOf<chatMessage>()
                it.documents.forEach { document ->
                    val chatMessage = chatMessage(
                        document.get(chatMessage::uid.name).toString(),
                        document.get(chatMessage::text.name).toString(),
                        document.get(chatMessage::isImage.name).toString().toBoolean(),
                        document.get(chatMessage::datetime.name).toString().toLong()
                    )
                    newMessageList.add(chatMessage)
                }
                newMessageList.sortBy { it.datetime }
                chatMessageList.postValue(newMessageList)
                isFirstLoad = false
            }
    }

    fun loadMessages(datetime:Long) {
        if (isFirstLoad || isFullLoaded) {
            return
        }
        db.collection("chatRoom")
            .document(roomId)
            .collection("messages")
            .whereLessThan("datetime", datetime)
            .orderBy("datetime", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.size.equals(0)) {
                    isFullLoaded = true
                    return@addOnSuccessListener
                }
                val newMessageList = mutableListOf<chatMessage>()
                chatMessageList.value?.also {
                    newMessageList.addAll(it)
                }
                snapshot.forEach { document ->
                    val chatMessage = chatMessage(
                        document.get(chatMessage::uid.name).toString(),
                        document.get(chatMessage::text.name).toString(),
                        document.get(chatMessage::isImage.name).toString().toBoolean(),
                        document.get(chatMessage::datetime.name).toString().toLong()
                    )
                    newMessageList.add(chatMessage)
                }
                newMessageList.sortBy { it.datetime }
                chatMessageList.postValue(newMessageList)
            }
    }

    fun saveImageCloud(
        resultCode: Int,
        data: Intent?,
        contentResolver: ContentResolver
    ) {
        if (resultCode != Activity.RESULT_OK) {
            if (pictureUri != null) {
                contentResolver.delete(pictureUri!!, null, null)
                pictureUri = null
            }
            return
        }
        val uri = if (data == null || data.data == null) pictureUri else data.data
        val image: Bitmap
        try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri!!)
            image = BitmapFactory.decodeStream(inputStream)
            inputStream!!.close()
        } catch (e: Exception) {
            return
        }

        // 取得したBimapの長辺を500ピクセルにリサイズする
        val imageWidth = image.width
        val imageHeight = image.height
        val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

        val storageRef =
            FirebaseStorage.getInstance().getReference().child(uid).child(Date().time.toString())
                .child("image.png")

        val baos = ByteArrayOutputStream()
        resizedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        var uploadTask = storageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
            sendImage(it.storage.path)
        }
    }

    fun sendMessage() {
        val message = chatMessage(uid, editMessage.value.toString(), false, Date().time)
        db.collection("chatRoom")
            .document(roomId)
            .collection("messages")
            .document()
            .set(message)
            .addOnSuccessListener {
                editMessage.postValue("")
            }
    }

    fun sendImage(path: String) {
        val message = chatMessage(uid, path, true, Date().time)
        db.collection("chatRoom")
            .document(roomId)
            .collection("messages")
            .document()
            .set(message)
            .addOnSuccessListener {
                editMessage.postValue("")
            }
    }
}