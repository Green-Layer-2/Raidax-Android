package com.cloudcoin2.wallet.Utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat


class PermissionUtils {

    companion object {
        private const val ACCESS_CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val ACCESS_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val ACCESS_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        //ACCESS_COARSE_LOCATION
        private const val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

        val TAG: String = PermissionUtils::class.java.simpleName


        /**
         * Called to check permission(In Android M and above versions only)
         *
         * @param permission, which we need to pass
         * @return true, if permission is granted else false
         */
        private fun checkForPermission(context: Context, permission: String): Boolean {
            val result = ContextCompat.checkSelfPermission(context, permission)
            //If permission is granted then it returns 0 as result
            return result == PackageManager.PERMISSION_GRANTED
        }




        fun getStoragePermission(activity: Activity): Boolean {
            //Managing run time permission for camera and external storage .
                val perms = arrayOf(
                    ACCESS_READ_EXTERNAL_STORAGE
                )
                return if (checkForPermission(activity, ACCESS_READ_EXTERNAL_STORAGE) &&
                    checkForPermission(activity, ACCESS_WRITE_EXTERNAL_STORAGE)
                ) {
                    true
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        activity.requestPermissions(
                            arrayOf(
                                ACCESS_READ_EXTERNAL_STORAGE,
                                ACCESS_WRITE_EXTERNAL_STORAGE
                            ),
                            Constants.REQUEST_CODE_STORAGE
                        )
                        /*
                        EasyPermissions.requestPermissions(
                            activity,
                            activity.getString(R.string.storage_permission),
                            Constants.REQUEST_CODE_STORAGE,
                            ACCESS_READ_EXTERNAL_STORAGE
                        )*/
                    }
                    false
                }
        }


        /**
         * Method to request Camera permissions
         */
        fun getCameraPermission(activity: Activity): Boolean {
            //Managing run time permission for camera and external storage .
            return if (checkForPermission(activity, ACCESS_CAMERA_PERMISSION) &&
                checkForPermission(activity, ACCESS_READ_EXTERNAL_STORAGE)
            ) {
                true
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(
                        arrayOf(
                            ACCESS_CAMERA_PERMISSION,
                            ACCESS_READ_EXTERNAL_STORAGE,
                            ACCESS_WRITE_EXTERNAL_STORAGE
                        ),
                        Constants.REQUEST_CODE_STORAGE
                    )
                }
                false
            }
        }



        /**
         * Method to request gallery permissions
         */
        fun getGalleryPermission(activity: Activity): Boolean {
            //Managing run time permission for camera and external storage .
            return if (checkForPermission(
                    activity,
                    ACCESS_READ_EXTERNAL_STORAGE
                )
            ) {
                true
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(
                        arrayOf(ACCESS_READ_EXTERNAL_STORAGE),
                        Constants.REQUEST_CODE_STORAGE
                    )
                }
                false
            }
        }
    }
}