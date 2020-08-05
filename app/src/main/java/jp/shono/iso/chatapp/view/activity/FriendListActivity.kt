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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.databinding.ActivityChatRoomBinding
import jp.shono.iso.chatapp.model.chatMessage
import jp.shono.iso.chatapp.model.userData
import jp.shono.iso.chatapp.viewmodel.ChatRoomViewModel
import jp.shono.iso.chatapp.viewmodel.FriendListViewModel
import kotlinx.android.synthetic.main.activity_chat_room.*
import java.text.SimpleDateFormat
import java.util.*

class FriendListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var viewModel: FriendListViewModel
    private val customAdapter by lazy {FriendListRecyclerViewAdapter(this)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)
        initialize()
    }

    fun initialize() {
        initBinding()
        initViewModel()
        initLayout()
    }

    private fun initBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friend_list)
        binding.lifecycleOwner = this
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(FriendListViewModel::class.java).apply {
            /*
            initSetting()
            binding.editMessage = editMessage
            chatMessageList.observe(this@FriendListActivity, androidx.lifecycle.Observer {
                customAdapter.refresh(it)
            })

             */
        }
    }

    private fun initLayout() {
        initRecyclerView()
    }

    private fun initRecyclerView() {
        chatMessageRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@FriendListActivity)
            adapter = customAdapter

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        /*
                        viewModel.chatRoomList.value?.also {
                            viewModel.loadChatRoom(it.last().datetime)
                        }
                        
                         */
                    }
                }
            })
        }
    }
}

class FriendListRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<FriendListViewHolder>() {
    var friendList = mutableListOf<userData>()

    fun refresh(newFriendList: MutableList<userData>?) {
        newFriendList?.also {
            friendList.apply {
                clear()
                addAll(it)
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendListViewHolder {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.user_cell, parent, false)

        return FriendListViewHolder(inflate)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: FriendListViewHolder, position: Int) {
        val item = friendList.get(position)
        holder.apply {
            userNameView.setText(item.name)
        }
    }

    override fun getItemCount(): Int {
        return friendList.size
    }
}

class FriendListViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    var userNameView: TextView

    init {
        userNameView = itemView.findViewById(R.id.userName)
    }
}