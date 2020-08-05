package jp.shono.iso.chatapp.view.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.databinding.ActivityChatRoomBinding
import jp.shono.iso.chatapp.viewmodel.FriendListViewModel
import kotlinx.android.synthetic.main.activity_friend_list.*

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
        initViewModel()
        initLayout()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(FriendListViewModel::class.java).apply {
            friendList.observe(this@FriendListActivity, androidx.lifecycle.Observer {
                loadName()
            })
            nameList.observe(this@FriendListActivity, androidx.lifecycle.Observer {
                customAdapter.refresh(it, uidList)
            })
            initFriendList()
        }
    }

    private fun initLayout() {
        initRecyclerView()
    }

    private fun initRecyclerView() {
        friendRecyclerView.apply {
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
                        viewModel.friendList.value?.also {
                            viewModel.loadFriendList(it.last().datetime)
                        }
                    }
                }
            })
        }

        customAdapter.setOnItemClickListener(object : FriendListRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClickListener(uid: String) {
                val intent = Intent(this@FriendListActivity, ProfileActivity::class.java)
                intent.putExtra("uid", uid)
                startActivity(intent)
            }
        })
    }
}

class FriendListRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<FriendListViewHolder>() {
    var nameList = mutableListOf<String>()
    var uidList = mutableListOf<String>()
    lateinit var listener: OnItemClickListener
    fun refresh(newFriendList: MutableList<String>?, newUidList: MutableList<String>?) {
        newFriendList?.also {
            nameList.apply {
                clear()
                addAll(it)
            }
        }
        newUidList?.also {
            uidList.apply {
                clear()
                addAll(it)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendListViewHolder {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.user_cell, parent, false)

        return FriendListViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: FriendListViewHolder, position: Int) {
        val item = nameList.get(position)
        val uid = uidList.get(position)
        holder.apply {
            userNameView.setText(item)
            this.itemView.setOnClickListener{
                listener.onItemClickListener(uid)
            }
        }
    }

    override fun getItemCount(): Int {
        return nameList.size
    }

    interface OnItemClickListener{
        fun onItemClickListener(uid: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}

class FriendListViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    var userNameView: TextView

    init {
        userNameView = itemView.findViewById(R.id.userName)
    }
}