package jp.shono.iso.chatapp.view.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import jp.shono.iso.chatapp.R
import jp.shono.iso.chatapp.databinding.ActivityProfileBinding
import jp.shono.iso.chatapp.viewmodel.ProfileViewModel
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initialize()
    }

    private fun initialize() {
        initBinding()
        initViewModel()
        initClick()
    }

    private fun initBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.lifecycleOwner = this
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java).apply {
            targetUid = intent.getStringExtra("uid") ?: ""
            buttonText.observe(this@ProfileActivity, Observer {
                binding.buttonText = it
            })
            isStranger.observe(this@ProfileActivity, Observer {
                binding.isStranger = it
            })
            isSend.observe(this@ProfileActivity, Observer {
                binding.isSend = it
            })
            name.observe(this@ProfileActivity, Observer {
                binding.name = it
            })
            initProfile()
        }
    }

    private fun initClick() {
        followButton.setOnClickListener {
            viewModel.sendFriendRequest()
        }
    }


}
