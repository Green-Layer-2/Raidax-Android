package com.cloudcoin2.wallet.base

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.cloudcoin.droid.ui.LoaderFragment

abstract class BaseActivity: AppCompatActivity() {

    protected abstract fun defineLayoutResource(): Int

    protected abstract fun initializeBinding(binding: ViewDataBinding)

    protected abstract fun initializeViewModels()

    protected abstract fun initializeBehavior()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var binding: ViewDataBinding = DataBindingUtil.setContentView(this, defineLayoutResource())
        initializeBinding(binding)
        initializeViewModels()
        initializeBehavior()
    }

    fun showLoader(clickable: Boolean = true) {
        val fragment = LoaderFragment(clickable)
        supportFragmentManager
            ?.beginTransaction()
            ?.add(android.R.id.content, fragment, "Startup Task in Progress,Please Wait..")
            ?.commitAllowingStateLoss()
    }

    fun hideLoader() {
        val fragment = supportFragmentManager?.findFragmentByTag("LOAD")
        if (fragment != null) {
            supportFragmentManager?.beginTransaction()
                ?.remove(fragment)
                ?.commitAllowingStateLoss()
        }
    }

    fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}