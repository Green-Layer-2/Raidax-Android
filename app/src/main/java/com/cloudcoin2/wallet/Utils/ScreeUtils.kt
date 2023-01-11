package com.cloudcoin2.wallet.Utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Created by Akhtar
 */
object ScreeUtils {
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }
}