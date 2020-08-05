package jp.shono.iso.chatapp.view.activity

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.model.friendRequest
import jp.shono.iso.chatapp.viewmodel.RequestListViewModel
import kotlinx.android.synthetic.main.activity_request_list.*

class RequestListActivity : AppCompatActivity() {
    lateinit var viewModel: RequestListViewModel
    private val customAdapter by lazy { RequestListRecyclerViewAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_list)
        initialize()
    }

    private fun initialize() {
        initViewModel()
        initLayout()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(RequestListViewModel::class.java).apply {
            requestList.observe(this@RequestListActivity, Observer {
                customAdapter.refresh(it, pathList.value)
            })
            initRequestList()
            customAdapter.viewModel = this
        }
    }

    private fun initLayout() {
        initRecyclerView()
    }

    private fun initRecyclerView() {
        requestRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RequestListActivity)
            adapter = customAdapter

            addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        viewModel.requestList.value?.also {
                            viewModel.loadRequestList(it.last().datetime)
                        }
                    }
                }
            })
        }
    }
}

class RequestListRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<RequestListViewHolder>() {
    private val requestList = mutableListOf<friendRequest>()
    private val pathList = mutableListOf<String>()
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    lateinit var viewModel: RequestListViewModel

    fun refresh(newRequestList: MutableList<friendRequest>?, newPathList: MutableList<String>?) {
        newRequestList?.also {
            requestList.apply {
                clear()
                addAll(it)
            }
        }
        newPathList?.also {
            pathList.apply {
                clear()
                addAll(it)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestListViewHolder {
        val inflate: View =
            LayoutInflater.from(parent.context).inflate(R.layout.request_cell, parent, false)

        return RequestListViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: RequestListViewHolder, position: Int) {
        val item = requestList.get(position)
        val path = pathList.get(position)
        holder.apply {
            db.collection("users")
                .document(item.uid)
                .get()
                .addOnSuccessListener { documents ->
                    userNameView.setText("${documents.data?.get("name")}")
                }
                .addOnFailureListener {
                    userNameView.setText("不明")
                }
            yesButton.setOnClickListener {
                viewModel.pushButton(item.uid, path, true)
                falseClickable(this, true)
            }
            noButton.setOnClickListener {
                viewModel.pushButton(item.uid, path, false)
                falseClickable(this, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    fun falseClickable(holder: RequestListViewHolder, isOk:Boolean) {
        holder.apply {
            yesButton.isClickable = false
            noButton.isClickable = false
            itemView.setBackgroundColor(Color.LTGRAY)
        }
    }

}

class RequestListViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    var userNameView: TextView
    var yesButton: Button
    var noButton: Button

    init {
        userNameView = itemView.findViewById(R.id.userName)
        yesButton = itemView.findViewById(R.id.yes_button)
        noButton = itemView.findViewById(R.id.no_button)
    }
}
