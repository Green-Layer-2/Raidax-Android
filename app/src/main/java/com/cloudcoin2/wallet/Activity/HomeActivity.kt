package com.cloudcoin2.wallet.Activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cloudcoin2.wallet.base.BaseActivity
import com.cloudcoin2.wallet.R
import com.cloudcoin2.wallet.Utils.SharedPref
import com.google.android.material.bottomnavigation.BottomNavigationView


class HomeActivity : BaseActivity() {


    companion object {
        private const val TAG = "HomeActivity"

        fun start(context: Context, clearPrev: Boolean) {
            val intent = Intent(context, HomeActivity::class.java)
            if (clearPrev) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }

    lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun defineLayoutResource(): Int {
        return R.layout.activity_home
    }

    override fun initializeBinding(binding: ViewDataBinding) {

    }


    override fun initializeViewModels() {

    }

    override fun initializeBehavior() {
        val navHostFragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        initializeBottomMenu()
        if (!SharedPref.getIntroStatus(this))
            showDialog()

    }

    public fun loadFragment(page: Int) {
        if (page == 1) {
            navController.navigate(R.id.homeFragment)
        }
        if (page == 2) {
            navController.navigate(R.id.transactionFragment)
        }
    }


    private fun initializeBottomMenu() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    //  replaceMyFragment(HomeFragment())
                    navController.navigate(R.id.homeFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_up_down -> {
                    //   replaceMyFragment(TransactionFragment())
                    navController.navigate(R.id.transactionFragment)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_card -> {
                    navController.navigate(R.id.SettingsFragment)
                    return@setOnNavigationItemSelectedListener true
                }

                else -> {
                    return@setOnNavigationItemSelectedListener false
                }
            }
        }
    }


    fun replaceMyFragment(fragment: Fragment) {
        val fManager = supportFragmentManager
        val fragmentByTag = fManager.findFragmentByTag(fragment.javaClass.simpleName)
        if (fragmentByTag == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment, fragment.javaClass.simpleName)
                .commitAllowingStateLoss()
        }
    }


    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(
            "This Software is provided as-is with all faults, defects and errors, and without warranty of any kind, " +
                    "free of cost from the CloudCoin Consortium."
        )

        builder.setPositiveButton("I agree") { dialog, which ->
            SharedPref.setIntroStatus(this, true)
        }

        builder.setCancelable(false)
        builder.show()

    }


}