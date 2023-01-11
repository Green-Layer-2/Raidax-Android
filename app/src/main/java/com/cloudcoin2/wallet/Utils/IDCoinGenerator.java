package com.cloudcoin2.wallet.Utils;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.DetectionCoinsModel;
import com.cloudcoin2.wallet.Model.RaidaResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class IDCoinGenerator implements SocketListener {
    private static final String SEPERATOR = Integer.toBinaryString((Integer.parseInt("3e3e", 16)));

    private static final String[] WSSHosts = {
            "ebc0-99a2-92e-10420.skyvault.cc",
            "ebc2-4555a2-92e-10422.skyvault.cc",
            "ebc4-9aes2-92e-10424.skyvault.cc",
            "ebc6-13a2-92e-10426.skyvault.cc",
            "ebc8-11a2-92e-10428.skyvault.cc",
            "ebc10-56a2-92e-104210.skyvault.cc",
            "ebc12-88a2-92e-10412.skyvault.cc",
            "ebc14-90a2-92e-10414.skyvault.cc",
            "ebc16-66a2-92e-10416.skyvault.cc",
            "ebc18-231a2-92e-10418.skyvault.cc",
            "ebc20-13489-92e-10420.skyvault.cc",
            "ebc22-kka2-92e-10422.skyvault.cc",
            "ebc24-mnna2-92e-10444.skyvault.cc",
            "ebc26-uuia2-92e-10426.skyvault.cc",
            "ebc28-eera2-92e-10428.skyvault.cc",
            "ebc30-zxda2-92e-10430.skyvault.cc",
            "ebc32-wera2-92e-10432.skyvault.cc",
            "ebc34-34oa2-92e-10434.skyvault.cc",
            "ebc36-mhha2-92e-10436.skyvault.cc",
            "ebc38-qqra2-92e-10438.skyvault.cc",
            "ebc40-bhta2-92e-10440.skyvault.cc",
            "ebc42-nkla2-92e-10442.skyvault.cc",
            "cbe88-3i0a2-63e-21233.skyvault.cc",
            "7cbdbe-2arbf-e29-64401.skyvault.cc",
            "ebc48-adea2-92e-10448.skyvault.cc"
    };
    private  RAIDA raida = RAIDA.getInstance();
    private int noResponseCount = 0;
    private int passCount = 0;
    private  int failedCount = 0;
    private CloudCoin idCoin = null;
    private final OkHttpClient client = new OkHttpClient();
    private String  IDPath;
    static String DIR_BASE = "CloudCoins";
    static String ID_DIR_NAME = "ID";


    public byte[] generateData(byte[] sn, int raidaIndex) {
       byte[] header = raida.generateHeader(raidaIndex,31);
        byte[] challenge = raida.generateChallenge();
        byte[] an = raida.generatePan();
        idCoin.setAn(raidaIndex,an);

        byte[] body = new BigInteger(SEPERATOR, 2).toByteArray();
        byte[] mData = new byte[header.length + challenge.length + body.length + an.length + sn.length];
        ByteBuffer buff = ByteBuffer.wrap(mData);
        buff.put(header);
        buff.put(challenge);
        buff.put(sn);
        buff.put(an);
        buff.put(body);
        byte[] socketData = buff.array();
        return socketData;
    }

    private void generateCoin() {
        noResponseCount =0;
        passCount = 0;
        failedCount = 0;
        int serial =
                CommonUtils.generateNumber(26001, 100000);
        String sNo = Integer.toBinaryString(serial);
        byte[] sn = new BigInteger(sNo, 2).toByteArray();
        idCoin = new CloudCoin(sn,null,null);

        for (int i = 0; i < WSSHosts.length; i++) {
            String url = "wss://".concat(WSSHosts[i]).concat(":8888");
            Request request = new Request.Builder().url(url).build();

            String body = RAIDA.bytesToHex(generateData(sn,i));
            Log.d("RAIDA "+i,"BODY: "+body);
            WebSocketClient listener = new WebSocketClient(body, this);

             WebSocket ws = client.newWebSocket(request, listener);
            //


        }
    }

    @Override
    public void onResponse(String socketResponse) {
        if (socketResponse == "00000000000000000000000000000000000000000000000000") {
            noResponseCount++;
        } else {
            RaidaResponse response = new RaidaResponse(hexToBytes(socketResponse),31);
            byte[] statusBytes = {response.getStatus()};
            String responseCode = RAIDA.bytesToHex(statusBytes);
            Log.d("RAIDA "+response.getRaidaId(),"RESPONSE CODE: "+responseCode);
            if(responseCode.equals("FA"))
                passCount++;
            else if(responseCode.equals("28"))
            {
                // this coin is already used.abort and retry with different coin
                client.dispatcher().executorService().shutdown();
                generateCoin();
            }
            else if(responseCode.equals("29"))
            {
                // service is down for 6 seconds, sleep and then retry
                client.dispatcher().executorService().shutdown();
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                generateCoin();

            }

        }
        if(noResponseCount+passCount+failedCount == 25)
        {
            client.dispatcher().executorService().shutdown();
            if(passCount>=16)
            {
                writeIDcoinToFolder();
            }


        }

    }

    private void writeIDcoinToFolder() {
        try {
            byte[] binary = RAIDA.getInstance().coinToBinary(idCoin);
            File file = new File(IDPath, idCoin.getFileName());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(binary);
                // fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] hexToBytes(String hex) {

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) j;
        }
        return bytes;
    }

    public CloudCoin getIDCoin()
    {
        ArrayList<Uri> filePaths = new ArrayList<>();
        int IDcoincc = CommonUtils.getCoinCount(IDPath);

        if(IDcoincc==0)
        return null;
        else{
            File file = new File(IDPath);
            File[] fList = file.listFiles();

            if (fList != null) {
               Uri path2=Uri.fromFile(new File(fList[0].getPath()));
               filePaths.add(path2);
               Log.d("IDpath",path2.toString());
               File idFile=new File(path2.getPath());
               byte[] bytes = KotlinUtils.INSTANCE.readBinaryFile(filePaths.get(0).getPath());
                RAIDA raida = RAIDA.getInstance();
                raida.setDebug(true);
                try {
                    if(bytes!=null) {
                        CloudCoin cc = raida.binaryToCoin(bytes);
                        return cc;
                    }
                    else{
                        //Toast.makeText(, "", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }
        return null;
    }
}
