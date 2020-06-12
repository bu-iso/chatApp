package jp.shono.iso.chatapp.view.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.databinding.ActivityCreateRoomBinding
import jp.shono.iso.chatapp.viewmodel.CreateRoomViewModel
import kotlinx.android.synthetic.main.activity_create_room.*

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var binding:ActivityCreateRoomBinding
    private lateinit var viewModel:CreateRoomViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)
        initialize()
    }

    fun initialize() {
        initBinding()
        initViewModel()
        initLayout()
    }

    fun initBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_room)
        binding.lifecycleOwner = this
    }

    fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(CreateRoomViewModel::class.java).apply {
            isSuccess.observe(this@CreateRoomActivity, Observer {
                if(it) {
                    finish()
                }
            })
            binding.roomTitle= roomTitle
        }
    }

    fun initLayout() {
        initButton()
    }

    fun  initButton() {
        doneButton.setOnClickListener {
            viewModel.createRoom()
        }
    }


}
