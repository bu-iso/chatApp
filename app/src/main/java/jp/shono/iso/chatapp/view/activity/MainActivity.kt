package jp.shono.iso.chatapp.view.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.model.chatRoom
import jp.shono.iso.chatapp.model.userData
import jp.shono.iso.chatapp.viewmodel.ChatRoomListViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: ChatRoomListViewModel
    private val customAdapter by lazy { ChatRoomListRecyclerViewAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()

        onCheckLogin()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(applicationContext, LoginMenuActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        onCheckLogin()
    }

    fun onCheckLogin() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val intent = Intent(applicationContext, LoginMenuActivity::class.java)
            startActivity(intent)
        }
    }

    fun initialize() {
        initViewModel()
        initLayout()
    }

    fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ChatRoomListViewModel::class.java).apply {
            initChatRoomList()
            idList.observe(this@MainActivity, Observer {
                customAdapter.refresh(chatRoomList.value, it)
            })
        }
    }

    fun initLayout() {
        setSupportActionBar(toolbar)
        initButton()
        initRecyclerView()
    }

    fun initButton() {
        fab.setOnClickListener{
            val intent = Intent(this@MainActivity, CreateRoomActivity::class.java)
            startActivity(intent)
        }
    }

    fun initRecyclerView() {
        chatListRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = customAdapter

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        viewModel.chatRoomList.value?.also {
                            viewModel.loadChatRoom(it.last().datetime)
                        }
                    }
                }
            })
        }

        customAdapter.setOnItemClickListener(object : ChatRoomListRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClickListener(id: String) {
                val intent = Intent(this@MainActivity, ChatRoomActivity::class.java)
                intent.putExtra("roomId", id)
                startActivity(intent)
            }
        })
    }
}

class ChatRoomListRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<ChatRoomListViewHolder>() {
    lateinit var listener: OnItemClickListener
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)
    private val chatRoomList = mutableListOf<chatRoom>()
    private val idList = mutableListOf<String>()
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun refresh(newChatRoomList: MutableList<chatRoom>?, newIdList: MutableList<String>?) {
        newChatRoomList?.also {
            chatRoomList.apply {
                clear()
                addAll(it)
            }
        }
        newIdList?.also {
            idList.apply {
                clear()
                addAll(it)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomListViewHolder {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.chat_room_list_cell, parent, false)

        return ChatRoomListViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ChatRoomListViewHolder, position: Int) {
        val item = chatRoomList.get(position)
        holder.apply {
            titleView.setText(item.title)
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
            this.itemView.setOnClickListener{
                listener.onItemClickListener(idList.get(position))
            }
        }
    }

    override fun getItemCount(): Int {
        return chatRoomList.size
    }

    interface OnItemClickListener{
        fun onItemClickListener(id: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}

class ChatRoomListViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    var titleView: TextView
    var dateView: TextView
    var userNameView: TextView

    init {
        titleView = itemView.findViewById(R.id.title)
        dateView = itemView.findViewById(R.id.date)
        userNameView = itemView.findViewById(R.id.userName)
    }
}
