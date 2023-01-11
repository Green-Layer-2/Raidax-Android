package com.cloudcoin2.wallet.Activity

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.databinding.ViewDataBinding
import com.cloudcoin2.wallet.Model.RaidaItems

import com.cloudcoin2.wallet.base.BaseActivity
import com.cloudcoin2.wallet.R
import com.cloudcoin2.wallet.Utils.Constants
import com.cloudcoin2.wallet.Utils.CoroutinesUtils
import com.cloudcoin2.wallet.Utils.PermissionUtils
import com.cloudcoin2.wallet.Utils.RAIDA

import kotlinx.coroutines.delay
import java.lang.Exception


class SplashActivity : BaseActivity() {
    override fun defineLayoutResource(): Int {
        return R.layout.activity_splash
    }

    override fun initializeBinding(binding: ViewDataBinding) {

    }

    override fun initializeViewModels() {

    }

    override fun initializeBehavior() {
        if (PermissionUtils.getStoragePermission(this)) {
            CoroutinesUtils.main {
                Constants.isStartupTask=true;
                delay(1000)
                HomeActivity.start(this, true)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Constants.REQUEST_CODE_STORAGE -> if (grantResults.size > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1]
                == PackageManager.PERMISSION_GRANTED
            ) {

                initializeBehavior()

            } else {
                // UiUtils.showToast(this, getString(R.string.alert_permission));
            }
        }
    }



}