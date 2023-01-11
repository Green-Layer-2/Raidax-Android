package com.cloudcoin2.wallet.Utils

import android.content.Context
import android.content.SharedPreferences



class SharedPref {

    companion object {
        private const val PREF_INTRO_COMPLETED = " intro_completed"
        const val APP_NAME = "Cloudcoin"


        private fun getSharedPreferences(mContext: Context): SharedPreferences {
            return mContext.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        }



        fun getIntroStatus(context: Context): Boolean {
            return getSharedPreferences(
                context
            )
                .getBoolean(PREF_INTRO_COMPLETED, false)
        }


        fun setIntroStatus(context: Context, value: Boolean) {
            val editor = getSharedPreferences(
                context
            ).edit()
            editor.putBoolean(PREF_INTRO_COMPLETED, value)
            editor.apply()
        }

        fun clearPreferences(context: Context) {
            val editor = getSharedPreferences(
                context
            ).edit()
            editor.clear()
            editor.apply()
        }

    }
}