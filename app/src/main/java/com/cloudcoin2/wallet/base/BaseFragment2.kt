package com.cloudcoin2.wallet.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cloudcoin2.wallet.base.BaseActivity

/**
 * Created by Akhtar
 */
abstract class BaseFragment2 : Fragment() {

    private lateinit var mActivity: BaseActivity

    protected abstract fun defineLayoutResource(): Int
    protected abstract fun initializeComponent(view: View)
    protected abstract fun initializeBehavior()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(defineLayoutResource(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponent(view)
        initializeBehavior()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mActivity = activity as BaseActivity
    }

    fun showLoader(clickable: Boolean = true) {
        mActivity.showLoader(clickable)
    }

    fun getBaseActivity(): BaseActivity {
        return mActivity
    }

    fun hideLoader() {
        mActivity.hideLoader()
    }

    /**
     * hide keyboard
     */
    fun hideKeyboard() {
        mActivity.hideKeyboard()
    }


}