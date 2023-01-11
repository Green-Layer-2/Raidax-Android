package com.cloudcoin2.wallet.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.cloudcoin2.wallet.base.BaseActivity

/**
 * responsible for common operations performed inside any fragment.
 * 2nd June 2020 : Dheeraj : code changes to implement data binding
 */

abstract class BaseFragment : Fragment() {

    private lateinit var mActivity: BaseActivity

    protected abstract fun defineLayoutResource(): Int
    protected abstract fun initializeBindingComponent(binding: ViewDataBinding)
    protected abstract fun initializeViewModel()
    protected abstract fun initializeComponent(view: View)
    protected abstract fun initializeBehavior()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

       val bind = inflater.inflate(defineLayoutResource(), container, false)
        val binding: ViewDataBinding =
            DataBindingUtil.inflate(inflater, defineLayoutResource(), container, false)

        initializeBindingComponent(binding)
        initializeViewModel()

        return bind
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