package jp.shono.iso.chatapp.model

import com.google.firebase.firestore.PropertyName

class friendRequest (
    var uid:String,
    @set:PropertyName("isChecked")
    @get:PropertyName("isChecked")
    var isChecked:Boolean = false,
    @set:PropertyName("isOk")
    @get:PropertyName("isOk")
    var isOk:Boolean = false,
    var datetime:Long
)