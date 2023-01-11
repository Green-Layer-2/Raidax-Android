package com.cloudcoin.droid.ui

import android.view.View
import android.widget.FrameLayout
import androidx.databinding.ViewDataBinding
import com.cloudcoin2.wallet.R
import com.cloudcoin2.wallet.base.BaseFragment

/**
 * Created by Akhtar
 */
class LoaderFragment(val clickable: Boolean) : BaseFragment(){

    private lateinit var flMain: FrameLayout

    override fun defineLayoutResource(): Int {
        return R.layout.fragment_loader
    }

    override fun initializeBindingComponent(binding: ViewDataBinding) {

    }

    override fun initializeViewModel() {

    }

    override fun initializeComponent(view: View) {
        flMain = view.findViewById(R.id.fragment_loader_flMain)
    }

    override fun initializeBehavior() {
        flMain.setOnClickListener {
            if (clickable) {
                hideLoader()
            }
        }
    }

}