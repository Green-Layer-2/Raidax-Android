package com.cloudcoin2.wallet.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class CommonUtils {
    public static String createDirectory(File path, String dirName,String DIR_BASE) {
        String idPath;

        idPath = path + "/" + DIR_BASE + "/" + dirName;
        try {
            File idPathFile = new File(idPath);
            idPathFile.mkdirs();
            //Log.d("path created",idPath);
        } catch (Exception e) {
            Log.e("TAG", "Can not create Import directory");
            return null;
        }

        return idPath;
    }

    public static int getCoinCount(String DirPath) {
        int count = 0;
        if(DirPath!=null) {
            File file = new File(DirPath);
            File[] list = file.listFiles();

            if (list != null) {
                for (File f : list) {
                    count++;
                    System.out.println("170 " + count);
                }
            }
        }
        return count;
    }

    public static String getFileExtension(String f) {
        String ext = "";
        int i = f.lastIndexOf('.');

        if (i > 0 && i < f.length() - 1) {
            ext = f.substring(i + 1);
        }

        return ext;
    }


    @SuppressLint("Range")
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getBaseDirectory(Context context) {
        String localPath = context.getExternalFilesDir("").getAbsolutePath();
        //return Environment.getExternalStorageDirectory().getAbsolutePath();
        return  localPath;
    }

    public static String getFolderPath(Context context, String folderName) {
        String basePath = getBaseDirectory(context) + "/" + "CloudCoins";
        String folderPath = basePath + "/" + folderName;
        return  folderPath;
    }

    public static String[] removeBlankElements(String[] arr) {
        // Create a list to store non-blank elements
        List<String> nonBlankElements = new ArrayList<String>();

        // Loop through the input array
        for (String str : arr) {
            // Check if the string is not blank (i.e. not empty or only whitespace)
            if (!str.trim().isEmpty()) {
                // Add the non-blank string to the list
                nonBlankElements.add(str);
            }
        }

        // Convert the list back to an array
        return nonBlankElements.toArray(new String[nonBlankElements.size()]);
    }


    public static int countFiles(String folderName, String ext) {
        try {
            File folder = new File(folderName);
            List fileNames = Arrays.asList(folder.list())
                    .stream()
                    .filter(x -> x.contains(ext))
                    .collect(Collectors.toList());
            int total=fileNames.size();
            return  total;

        }
        catch (Exception e){

            e.printStackTrace();
            return  0;
        }
    }


    public static int generateNumber(int low,int high) {
        Random r = new Random();
        int result = r.nextInt(high - low) + low;
        return result;
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(new Date());
    }/*from   www.jav a2  s  .  c o  m*/


}
