package jp.shono.iso.chatapp.model

import com.google.firebase.firestore.PropertyName
import java.sql.Timestamp

class chatMessage (
    var uid:String,
    var text:String,
    // そのままisImageにするとisがエスケープされるため対策
    @set:PropertyName("isImage")
    @get:PropertyName("isImage")
    var isImage:Boolean,
    var datetime:Long
)