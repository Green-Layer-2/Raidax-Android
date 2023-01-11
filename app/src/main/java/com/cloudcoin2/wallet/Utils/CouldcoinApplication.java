package com.cloudcoin2.wallet.Utils;

import android.app.Application;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;

import androidx.room.Room;


import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.db.AppDatabase;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.Settings;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SIRSHA BANERJEE on 15/12/20
 * Last Updated Navraj singh on 06/12/22
 * Changed the Root Directory location to Internal storage to comply with Google App store policies
 */
public class CouldcoinApplication extends Application {

    static String DIR_BASE = "CloudCoins";
    static String IMPORT_DIR_NAME = "logs";
    public static String CloudcoinHome = "";
    private String importDirPath;

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        CloudcoinHome = CommonUtils.getBaseDirectory(this);
        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "cloudcoin-database").fallbackToDestructiveMigration().build();
        File path = getExternalFilesDir("");
        String dir = path.toString();
        Log.e("Testing",dir);
        importDirPath = createDirectory(path, IMPORT_DIR_NAME);
        if (isExternalStorageWritable()) {
            File appDirectory = new File(dir + "/" + DIR_BASE);
            File logDirectory = new File(importDirPath + "/logs");

            File logFile = new File(logDirectory, "logcat" + ".txt");
            Constants.LOG_PATH = logFile.getPath();
            // create app folder
            if (!appDirectory.exists()) {
                appDirectory.mkdir();
            }

            //CommonUtils.createDirectory(appDirectory.toString(),)
            // create log folder
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(logFile);
                writer.print("");
                writer.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            //   clear the previous logcat and then write the new one to the file
            try {
               // int pid = android.os.Process.myPid();
               // Process process = Runtime.getRuntime().exec("logcat -c");
                Process process = Runtime.getRuntime().exec("logcat *:D -v long -r 250000 -n 10 -f " + logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (isExternalStorageReadable()) {
            // only readable
        } else {
            // not accessible
        }

    }

    private String createDirectory(File path, String dirName) {
        String idPath;

        idPath = path + "/" + DIR_BASE + "/" + dirName;
        try {
            File idPathFile = new File(idPath);
            idPathFile.mkdirs();
        } catch (Exception e) {
            // Log.e(TAG, "Can not create Import directory");
            return null;
        }

        return idPath;
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
