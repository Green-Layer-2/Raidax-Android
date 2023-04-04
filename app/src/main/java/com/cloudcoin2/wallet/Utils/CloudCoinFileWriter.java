package com.cloudcoin2.wallet.Utils;

import android.system.Os;
import android.util.Log;

import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.deposit.DepositFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloudCoinFileWriter {

    public static byte[] readBytesFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        long fileSize = file.length();
        byte[] buffer = new byte[(int) fileSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(buffer);
        }
        return buffer;
    }
    public static List<CloudCoin> loadCoinsFromFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".bin"));

        List<CloudCoin> coins = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] data = readBytesFromFile(file.getAbsolutePath());
                coins.add(new CloudCoin(data));
                            } catch (IOException e) {
                // Handle IO Exception
            }
        }
        return coins;
    }



public static boolean WriteBytesToFile(byte[] data, String path) {
        File file = null;
        if (DepositFragment.sdcard != null) {
            file = new File(DepositFragment.sdcard, path);
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            Log.d(RAIDAX.TAG,"Wrote response to "+ path);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

//        try {
//            FileOutputStream fos = new FileOutputStream(path);
//            fos.write(data);
//            fos.close();
//            Log.d(RAIDAX.TAG,"Wrote response to "+ path);
//        } catch (Exception e) {
//            Log.d(RAIDAX.TAG, e.getMessage() + path);
//            return false;
//        }
        return true;
    }

    public static boolean WriteCoinToFile(CloudCoin cc, int format, String path) {
        try {
            String pathSeparator = System.getProperty("file.separator");

            String fileName = path + pathSeparator + cc.generateSingleCoinFileName(".bin");
            Log.d(RAIDAX.TAG, "Writing coin to " + fileName);
            String fmt = "";
            if (format == 9)
                fmt = "9";
            byte[] header = generateHeader(fmt, 1, 1, 0, 1, null, (byte) 0, "");
            byte[] cloudCoinData = cc.toByteArray(9); // Assuming there is a method in CloudCoin class to convert the
                                                      // object to a byte array

            // Concatenate header and cloudCoinData
            byte[] combinedData = new byte[header.length + cloudCoinData.length];
            System.arraycopy(header, 0, combinedData, 0, header.length);
            System.arraycopy(cloudCoinData, 0, combinedData, header.length, cloudCoinData.length);

            // Write combinedData to file

            try {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(combinedData);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(fileName);

            if (file.exists() && file.isFile()) {
                Log.d(RAIDAX.TAG, "File Size:" + file.length());
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] generateHeader(String format, int cloudID, int coinId, int encType, int coinCount,
            byte[] md5Hash, byte flags, String receipt) {
        byte[] header = new byte[32];

        header[0] = (byte) format.charAt(0);
        header[1] = (byte) cloudID;

        byte[] coinIdBytes = ByteBuffer.allocate(2).putShort((short) coinId).array();
        System.arraycopy(coinIdBytes, 0, header, 2, 2);

        byte[] encTypeByte = { (byte) encType };
        System.arraycopy(encTypeByte, 0, header, 4, 1);

        byte[] coinCountBytes = ByteBuffer.allocate(2).putShort((short) coinCount).array();
        System.arraycopy(coinCountBytes, 0, header, 5, 2);

        if (md5Hash == null) {
            md5Hash = new byte[7]; // Initialize with zeros
        }
        System.arraycopy(md5Hash, 0, header, 7, 7);

        header[14] = flags;

        byte[] receiptBytes = receipt.getBytes(StandardCharsets.UTF_8);
        if (receiptBytes.length > 16) {
            System.arraycopy(receiptBytes, 0, header, 15, 16);
        } else {
            System.arraycopy(receiptBytes, 0, header, 15, receiptBytes.length);
        }

        return header;
    }
}
