package jp.shono.iso.chatapp.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.databinding.ActivityChatRoomBinding
import jp.shono.iso.chatapp.model.chatMessage
import jp.shono.iso.chatapp.model.userData
import jp.shono.iso.chatapp.viewmodel.ChatRoomViewModel
import kotlinx.android.synthetic.main.activity_chat_room.*
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var viewModel:ChatRoomViewModel
    private val customAdapter by lazy {ChatMessageRecyclerViewAdapter(this)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        initialize()
    }

    fun initialize() {
        initBinding()
        initViewModel()
        initLayout()
    }

    private fun initBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_room)
        binding.lifecycleOwner = this
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ChatRoomViewModel::class.java).apply {
            roomId = intent.getStringExtra("roomId") ?: ""
            initSetting()
            binding.editMessage = editMessage
            chatMessageList.observe(this@ChatRoomActivity, androidx.lifecycle.Observer {
                customAdapter.refresh(it)
            })
        }
    }

    private fun initLayout() {
        initButton()
        initRecyclerView()
    }

    private fun initButton() {
        imageButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    showChooser()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
                    return@setOnClickListener
                }
            } else {
                showChooser()
            }
        }

        doneButton.setOnClickListener {
            viewModel.sendMessage()
        }
    }

    private fun initRecyclerView() {
        chatMessageRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatRoomActivity)
            adapter = customAdapter
        }
    }

    private fun showChooser() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        viewModel.pictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.pictureUri)

        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_REQUEST_CODE) {
            viewModel.saveImageCloud(resultCode, data, contentResolver)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }
}

class ChatMessageRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<ChatMessageViewHolder>() {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)
    var chatMessageList = mutableListOf<chatMessage>()
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun refresh(newChatMessageList: MutableList<chatMessage>?) {
        newChatMessageList?.also {
            chatMessageList.apply {
                clear()
                addAll(it)
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.chat_message_cell, parent, false)

        return ChatMessageViewHolder(inflate)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val item = chatMessageList.get(position)
        holder.apply {
            if (item.isImage) {
                val storageReference = FirebaseStorage.getInstance().reference.child(item.text)
                GlideApp.with(context)
                    .load(storageReference)
                    .into(imageView)
                textView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
            } else {
                textView.apply {
                    setText(item.text)
                    visibility = View.VISIBLE
                }
                imageView.visibility = View.GONE
            }
            dateView.setText(simpleDateFormat.format(item.datetime))
            db.collection("users")
                .whereEqualTo(userData::uid.name, item.uid)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    userNameView.setText("${documents.first().data?.get("name")}")
                }
                .addOnFailureListener {
                    userNameView.setText("不明")
                }
            if (FirebaseAuth.getInstance().currentUser?.uid.equals(item.uid)) {
                itemView.setBackgroundColor(Color.CYAN)
            } else {
                itemView.setBackgroundColor(Color.WHITE)
            }
        }
    }

    override fun getItemCount(): Int {
        return chatMessageList.size
    }
}

class ChatMessageViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    var textView: TextView
    var dateView: TextView
    var userNameView: TextView
    var imageView: ImageView

    init {
        textView = itemView.findViewById(R.id.text)
        dateView = itemView.findViewById(R.id.date)
        userNameView = itemView.findViewById(R.id.userName)
        imageView = itemView.findViewById(R.id.image)
    }
}

@GlideModule
class MyAppGlideModule: AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(StorageReference::class.java,
            InputStream::class.java,
            FirebaseImageLoader.Factory())
    }
}