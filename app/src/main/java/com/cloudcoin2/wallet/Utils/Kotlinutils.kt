package com.cloudcoin2.wallet.Utils


import android.util.Log
import java.io.File
import java.lang.Exception
import java.util.ArrayList
import android.R
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.bumptech.glide.load.engine.Resource
import java.io.IOException

import java.nio.channels.AsynchronousFileChannel.open
import java.nio.channels.AsynchronousServerSocketChannel.open
import java.nio.channels.DatagramChannel.open
import java.nio.channels.Selector.open
import android.content.res.AssetManager





object KotlinUtils {

   fun readBinaryFile(fileName: String): ByteArray? {
        val file = File(fileName)
        var bytes: ByteArray? = file.readBytes();

        return bytes
    }





}