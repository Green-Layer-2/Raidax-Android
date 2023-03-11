package com.cloudcoin2.wallet.Utils;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.*;
import static com.cloudcoin2.wallet.Utils.EncryptionOutputKt.encrypt;

import android.util.Log;

import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.Model.SerialAn;
import com.cloudcoin2.wallet.Model.UDPCall;


public class RAIDAX {

    private static final int MAX_CONNECTIONS = 25;
    private String SERVER_URL = "https://g1.raida-guardian-tx.us/coin4.txt";

    private static final int MIN_PASS = 16;
    private static final String SEPARATOR = Integer.toBinaryString((Integer.parseInt("3e3e", 16)));
    private static final int MAX_COINS = 28; // how many coins to pown/detect/find at a time
    private static final int MAX_DETECT = 50; // how many coins to fix at a time
    private static final int MAX_FIX = 290; // how many coins to fix at a time
    private static final int MAX_PACKETS = 64; // how many max packets in a single request
    private static final int CONNECTION_TIMEOUT = 3000; // 3 second timeout for fetching hosts file
    private static final int UDP_CONNECTION_TIMEOUT = 5000; // 5 second timeout for UDP response
    private static final int MAX_RETRIES = 3; // try maximum 3 times in case packets are lost
    private List<UDPCall> udpCalls = new ArrayList<>();
    private boolean debug = false;
    private int retry = 0, mPassCount = 0, mResponseCount = 0;
    public ArrayList<RaidaResponse> raidaResponses = new ArrayList<>();
    private UDPCallBackInterface udpCallbacks;
    private EchoResult echoResult = new EchoResult(0, 0);
    private final Object lock = new Object();
    private boolean isExecuting = false;

    private static final ConnectionPool<DatagramSocket> connectionPool = new ConnectionPool<>(MAX_CONNECTIONS, () -> {
        try {
            return new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    });

    ArrayList<RaidaItems> raidaLists;

    private static final String[] hosts = {
            "https://g0.chelgu.cz/coin4.txt",
            "https://g1.raida-guardian-tx.us/coin4.txt",
            "https://cloudcoin.asia/coin4.txt",
            "https://ladyjade.cc/coin4.txt",
            "https://guardwatch.cc/coin4.txt",
            "https://g5.raida-guardian.cx/coin4.txt",
            "https://g6.goodguardian.xyz/coin4.txt",
            "https://ga7.nl/coin4.txt",
            "https://g8.raidaguardian.nz/coin4.txt",
            "https://mattyd.click/coin4.txt",
            "https://guardian25.com/coin4.txt",
            "https://raidacash.com/coin4.txt",
            "https://aeroflightcb300.com/coin4.txt",
            "https://cloudcoinconsortium.art/coin4.txt",
            "https://g14.gsxcover.com/coin4.txt",
            "https://newprojects.space/coin4.txt",
            "https://guardianscloud.xyz/coin4.txt",
            "https://g17.raida-guardian.net/coin4.txt",
            "https://raidaguardian.al/coin4.txt",
            "https://newprojects.tech/coin4.txt",
            "https://cloudcoins.asia/coin4.txt",
            "https://guardian.al/coin4.txt",
            "https://encrypting.us/coin4.txt",
            "https://cuvar.net/coin4.txt",
            "https://g24.rsxcover.com/coin4.txt"
    };

    private static RAIDAX raida;
    private final ArrayList<EchoStatus> mList = new ArrayList<>(25);

    public static RAIDAX getInstance() {
        if (raida == null) {
            synchronized (RAIDAX.class) {
                if (raida == null) {
                    raida = new RAIDAX();
                }
            }
        }
        return raida;
    }

    public static byte[] generateXHeader(int raidaID, int coinID, int length) {

        byte[] header = new byte[32];
        // Fill in the request data with the echo command

        header[0] = 0x01; // VR - Version of Routing header
        header[1] = 0x00; // SP - Split ID (not used in this example)
        header[2] = (byte) raidaID; // DA - Data Agent Index (not used in this example)
        header[3] = 0x00; // SH - Shard ID (not used in this example)
        header[4] = 0x00; // CG - Command Group (Authentication)
        header[5] = 0x00; // CM - Command (Echo)
        header[6] = 0x00; // ID - Cloud/Coin ID 0 (not used in this example)
        header[7] = (byte) coinID; // ID - Cloud/Coin ID 1 (not used in this example)

        // Fill in the Presentation group
        header[8] = 0x01; // VR - Version of PLS
        header[9] = 0x00; // AP - Application 0
        header[10] = 0x00; // AP - Application 1
        header[11] = 0x00; // CP - Compression (none)
        header[12] = 0x00; // TR - Translation (none)
        header[13] = 0x00; // AI - AI Translation (not used in this example)
        header[14] = 0x00; // RE - Reserved (not used in this example)
        header[15] = 0x00; // RE - Reserved (not used in this example)

        // Fill in the Encryption group
        header[16] = 0x00; // EN - Encryption Type (none)
        header[17] = 0x00; // DE - Denomination (not used in this example)
        header[18] = 0x00; // SN - Encryption SN 0 (not used in this example)
        header[19] = 0x00; // SN - Encryption SN 1 (not used in this example)
        header[20] = 0x00; // SN - Encryption SN 2 (not used in this example)
        header[21] = 0X00; // SN - Encryption SN 3 (not used in this example)
        header[22] = (byte) 0; // BL - Body Length (not used in this example)
        header[23] = (byte) length; // BL - Body Length (not used in this example)

        // Fill in the Nonce group
        header[24] = 0x00; // NO - Nonce 0 (not used in this example)
        header[25] = 0x00; // NO - Nonce 1 (not used in this example)
        header[26] = 0x00; // NO - Nonce 2 (not used in this example)
        header[27] = 0x00; // NO - Nonce 3 (not used in this example)
        header[28] = 0x00; // NO - Nonce 4 (not used in this example)
        header[29] = 0x00; // NO - Nonce 5 (not used in this example)
        header[30] = 0x00; // NO - Nonce 6 /
        header[31] = 0x00; // NO - Nonce 6 /

        return header;
    }

    public byte[] generateRandom(int length) {
        return generateRandom(length, null);
    }

    public byte[] generateRandom(int length, String seed) {
        String AB = "0123456789ABCDEF";
        // if(seed!=null)
        // Log.d("Seed:",seed);

        if (debug) {
            if (seed == null || seed.equals("41414141414141414141414141414141")) {
                AB = "BBBBBBBBBBBBBBBB";
            } else {
                AB = "AAAAAAAAAAAAAAAA";
            }
        }
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));

        String pan = sb.toString();
        byte[] bytes = pan.getBytes(StandardCharsets.UTF_8);
        byte[] returnBytes = new byte[length];
        System.arraycopy(bytes, 0, returnBytes, 0, length);
        // Log.d("RandomX:",bytesToHex(returnBytes));
        return returnBytes;
    }

    public byte[] generateChallenge() {
        byte[] challenge = generateRandom(12);
        byte[] checksumTotal = Utils.generateCRC32(challenge);
        byte[] checksum = new byte[4];
        System.arraycopy(checksumTotal, checksumTotal.length - 4, checksum, 0, 4);
        byte[] mData = new byte[challenge.length + checksum.length + 2];
        mData[mData.length - 2] = 0x3e;
        mData[mData.length - 1] = 0x3e;
        ByteBuffer buff = ByteBuffer.wrap(mData);
        buff.put(challenge);
        buff.put(checksum);
        byte[] challengeData = buff.array();
        return challengeData;
    }

    public void execute(int commandCode) throws InterruptedException {
        synchronized (lock) {
            if (isExecuting) {
                throw new IllegalStateException("Cannot execute while another command is already running");
            }
            isExecuting = true;
        }
        try {
            if (commandCode == CommandCodes.Echo) {
                try {
                    doEcho();
                } catch (Exception e) {

                }
            }
        } finally {
            synchronized (lock) {
                isExecuting = false;
            }
        }
    }

    public void doEcho() throws Exception {
        String html = null;
        udpCalls = new ArrayList<>();
        retry = 0;
        if (raidaLists == null || raidaLists.size() == 0) {
            html = getServerList();
            if (html != null && html.length() > 0)
                System.out.print(html);
            raidaLists = createServerList(html);
        }

        mList.clear();
        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] request = Protocol.GenerateRequest(i, CommandCodes.Echo);
            udpCalls.add(new UDPCall(request, i, CommandCodes.Echo));
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<RaidaResponse>> futures = new ArrayList<>();

        // Assuming you have a list of UDPCall objects called udpCalls
        for (UDPCall udpCall : udpCalls) {
            Future<RaidaResponse> future = executorService.submit(() -> executeCommand(udpCall));
            futures.add(future);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        List<RaidaResponse> results = new ArrayList<>();
        for (Future<RaidaResponse> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                // Handle any exceptions thrown during the execution of the task
            }
        }
        // for(RaidaResponse response: results) {
        // processResults(response);
        // }
        processEchoResults(results);
        connectionPool.releaseAllConnections();

    }

    public void processResults(List<RaidaResponse> results, int commandCode) {
        if (commandCode == CommandCodes.Echo) {
            EchoResult echoResult = new EchoResult(0, 0);
            echoResult.compute(results);
            this.echoResult = echoResult;
        }
    }

    public void processResults(RaidaResponse response) {
        System.out.println("Got Result from :" + response.raidaId + ". \nResponse: " + response.getResponseHex());
    }

    public void processEchoResults(List<RaidaResponse> results) {
        EchoResult echoResult = new EchoResult(0, 0);
        echoResult.compute(results);
        System.out.println("Got Total Results:" + echoResult.getResultCount());
        System.out.println("Got Pass Results:" + echoResult.getPassCount());
        System.out.println("Got Fail Results:" + echoResult.getFailCount());
        this.echoResult = echoResult;
    }

    public RaidaResponse executeCommand(UDPCall udp) throws InterruptedException {
        DatagramSocket ds = connectionPool.getConnection();
        try {
            InetAddress address;
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(raidaLists.get(udp.getIndex()).getServerAddress());
            DatagramPacket dp;
            if (udp.getIndex() == 0) {
                String s = serverAddr.toString();
                String newaddress = s.substring(1);
                address = InetAddress.getByName(newaddress);
            } else {
                address = serverAddr;
            }
            dp = new DatagramPacket(udp.getData(), udp.getData().length, address,
                    raidaLists.get(udp.getIndex()).getPorts());
            ds.send(dp);
            ds.setSoTimeout(UDP_CONNECTION_TIMEOUT);

            try {
                byte[] lMsg = new byte[500];

                byte[] realData = null;
                DatagramPacket rdp;
                rdp = new DatagramPacket(lMsg, lMsg.length);

                ds.receive(rdp);
                realData = rdp.getData();

                RaidaResponse response = new RaidaResponse(realData, udp.getCommandCode());
                ds.close();
                return response;
            } catch (SocketTimeoutException e) {

                if (retry < 2) {
                    retry++;
                    Thread.sleep(100);
                    getUDPResponse(dp, udp, ds, retry);
                } else {
                    e.printStackTrace();
                    ds.close();
                    RaidaResponse errorResponse = new RaidaResponse(e.getMessage().getBytes(), udp.getCommandCode());
                    return errorResponse;
                }

            }

        } catch (Exception e) {
            RaidaResponse errorResponse = new RaidaResponse(e.getMessage().getBytes(), udp.getCommandCode());
            return errorResponse;
        }
        RaidaResponse errorResponse = new RaidaResponse("Can not execute".getBytes(), udp.getCommandCode());
        return errorResponse;
    }

    public void makeEcho() throws Exception {
        raidaResponses.clear();
        String html = null;
        udpCalls = new ArrayList<>();
        retry = 0;
        if (raidaLists == null || raidaLists.size() == 0) {
            html = getServerList();
            if (html != null && html.length() > 0)
                System.out.print(html);
            raidaLists = createServerList(html);
        }

        mList.clear();
        for (int i = 0; i < raidaLists.size(); i++) {

            byte[] request = Protocol.GenerateRequest(i, CommandCodes.Echo);
            makeUdpCall(new UDPCall(request, i, CommandCodes.Echo));
        }

    }

    public void echo() throws Exception {
        raidaResponses.clear();
        String html = null;
        udpCalls = new ArrayList<>();
        retry = 0;
        if (raidaLists == null || raidaLists.size() == 0) {
            html = getServerList();
            if (html != null && html.length() > 0)
                System.out.print(html);
            raidaLists = createServerList(html);
        }

        mList.clear();
        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] header = generateHeader(i, 0);
            byte[] body = generateChallenge();
            byte[] eheader = generateXHeader(i, 4, body.length);
            byte[] mData = new byte[header.length + body.length];

            byte[] request = new byte[eheader.length + body.length];
            System.arraycopy(eheader, 0, request, 0, eheader.length);
            System.arraycopy(body, 0, request, 32, body.length);

            makeUdpCall(new UDPCall(request, i, 4));
        }
    }

    public void setCallbacks(UDPCallBackInterface callbacks) {
        udpCallbacks = callbacks;
    }

    public byte[] generateHeader(int raidaID, int type, int udpnum, byte udpChecksum, boolean encryption, byte[] nonce,
                                 byte[] mSerialNo) {
        byte[] udpHeader = new byte[32];
        byte[] serialNo = { (byte) 0, (byte) 0, (byte) 0 };
        if (nonce == null) {
            nonce = new byte[] { (byte) 0, (byte) 0, (byte) 0 };
        }
        byte mEnc = (byte) 0;

        if (udpnum < 1)
            udpnum = 1;

        byte[] size = new byte[2];
        size[0] = (byte) ((udpnum >> 8) & 0xFF);
        size[1] = (byte) (udpnum & 0xFF);

        byte coinType = 1;
        if (type == 30 || type == 31)
            coinType = 0;

        // Log.d("HEADER", "UDP NUM:" + udpnum);
        if (encryption) {
            if (!((nonce.equals("null")) || (nonce.length == 0))) {

                if (mSerialNo.length == 3 && nonce.length == 3) {
                    mEnc = (byte) 1;
                    serialNo = mSerialNo;
                }

            }
        }
        udpHeader[0] = 1;
        udpHeader[1] = 0;
        udpHeader[2] = (byte) raidaID;
        udpHeader[3] = 0;
        udpHeader[4] = 0;
        udpHeader[5] = (byte) type;
        udpHeader[6] = udpChecksum;
        udpHeader[7] = 4;
        udpHeader[8] = 1;
        udpHeader[9] = 0;
        udpHeader[10] = 0;
        udpHeader[11] = 0;
        udpHeader[12] = 0;
        udpHeader[13] = 0;
        udpHeader[14] = 0;
        udpHeader[15] = 0;
        udpHeader[16] = mEnc;
        udpHeader[17] = 0;
        udpHeader[18] = 0;
        udpHeader[19] = serialNo[0];
        udpHeader[20] = serialNo[1];
        udpHeader[21] = serialNo[2];
        udpHeader[22] = 0;
        udpHeader[23] = 0;
        udpHeader[24] = nonce[0];
        udpHeader[25] = nonce[1];
        udpHeader[26] = nonce[2];
        udpHeader[27] = 0;
        udpHeader[28] = 0;
        udpHeader[29] = 0;
        udpHeader[30] = 0;
        udpHeader[31] = 0;

        // Log.d("HEADER", bytesToHex(udpHeader));

        return udpHeader;
    }

    public byte[] generateHeader(int raidaID, int type) {
        return generateHeader(raidaID, type, 1, (byte) 0, false, null, null);
    }

    public byte[] generateHeader(int raidaID, int type, int udpnum) {
        return generateHeader(raidaID, type, udpnum, (byte) 0, false, null, null);
    }

    public byte[] generateHeader(int raidaID, int type, int udpnum, byte udpChecksum) {
        return generateHeader(raidaID, type, udpnum, udpChecksum, false, null, null);
    }

    private String readFromUnsecuredServer(String url)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_TIMEOUT);
        connection.connect();

        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null;) {
            html.append(line).append("\n");
        }
        in.close();

        String serverList = html.toString();
        if (serverList.length() == 0) {
            throw new IOException("Unable to retrieve server list from: " + url);
        }
        return serverList;
    }

    public String getServerURL() {
        int index = new Random().nextInt(hosts.length);
        SERVER_URL = hosts[index];
        return SERVER_URL;
    }

    public String getServerList() {
        String url = getServerURL();

        try {
            return readFromUnsecuredServer(url);
        } catch (IOException e) {

            return getServerList();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {

            return "";
        }
    }

    public ArrayList<RaidaItems> createServerList(String rawdata) throws Exception {
        if (rawdata != null) {
            String[] parts = rawdata.split("# Mirrors");
            String part1 = parts[0]; // 004
            // Log.d("URL", part1);
            String[] servers = part1.split("Primary RAIDA");
            String serverList = servers[1];
            // Log.d("serverList", serverList);
            String[] data = serverList.split("\n");

            // String[] data = CommonUtils.removeBlankElements(fdata);

            int length = data.length;
            raidaLists = new ArrayList<RaidaItems>();
            for (int i = 1; i < length; i++) {
                String details = data[i];
                String[] splitdata = details.split(" ");
                String[] finaldata = splitdata[0].split(":");
                String address = finaldata[0];
                int port = Integer.parseInt(finaldata[1]);

                // uncomment next two lines to simulate a raida (in this case raida 5) being
                // down
                /*
                 * if(i==6)
                 * address="127.0.0.1";
                 */
                RaidaItems raidaItems = new RaidaItems(address, port);

                raidaLists.add(raidaItems);
            }

        } else {
            throw new Exception("Unable to parse RAIDA list from server file");
        }
        return raidaLists;

    }

    private void makeUdpCall(UDPCall udp) {
        makeUdpCall(udp, false);
    }

    private void makeUdpCall(UDPCall udp, boolean ignore) {

        // add to the stack for replaying.
        udpCalls.add(udp);

        // final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Log.d("size", String.valueOf(raidaLists.size()));

                // Log.d("RAIDA " + udp.getIndex(), "Running CMD:" + udp.getCommandCode() + " ON
                // Server:"
                // + raidaLists.get(udp.getIndex()).getServerAddress());
                // Log.d("RAIDA " + udp.getIndex(), "Request: " + bytesToHex(udp.getData()));
                // System.out.println(raidaLists.get(i).getPorts());
                DatagramSocket ds = null;
                try {
                    InetAddress address;
                    ds = new DatagramSocket();
                    InetAddress serverAddr = InetAddress.getByName(raidaLists.get(udp.getIndex()).getServerAddress());
                    DatagramPacket dp;
                    if (udp.getIndex() == 0) {
                        String s = serverAddr.toString();
                        String newaddress = s.substring(1);
                        address = InetAddress.getByName(newaddress);
                    } else {
                        address = serverAddr;
                    }
                    dp = new DatagramPacket(udp.getData(), udp.getData().length, address,
                            raidaLists.get(udp.getIndex()).getPorts());
                    ds.send(dp);
                    ds.setSoTimeout(UDP_CONNECTION_TIMEOUT);

                    if (!ignore) {

                        // Log.d("RAIDA" + udp.getIndex(),
                        // "Attempt #0 to get response to CMD:" + udp.getCommandCode());
                        try {

                            getUDPResponse(dp, udp, ds, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ds.close();
                            handleResponseError(e, udp);
                        }

                        // System.out.println(bytesToHex(lMsg));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    ds.close();
                    handleResponseError(e, udp);
                }
            }
        });
        thread.start();
    }

    private void getUDPResponse(DatagramPacket dp, UDPCall udp, DatagramSocket ds, int retry) throws Exception {
        try {
            byte[] lMsg = new byte[500];

            byte[] realData = null;
            dp = new DatagramPacket(lMsg, lMsg.length);

            ds.receive(dp);
            realData = dp.getData();

            RaidaResponse response = new RaidaResponse(realData, udp.getCommandCode());
            // System.out.println("RAIDA " + response.getRaidaId() + ":Response to CMD " +
            // udp.getCommandCode() + ": " + Utils.bytesToHex(realData));
            ds.close();

            handleResponse(response, realData, udp.getCommandCode());
        } catch (SocketTimeoutException e) {

            if (retry < 2) {
                retry++;
                // Log.d("RAIDA" + udp.getIndex(), "Socket time out exception, trying to receive
                // again #" + receiveRetry);
                Thread.sleep(100);
                getUDPResponse(dp, udp, ds, retry);
            } else {
                // Log.d("RAIDA" + udp.getIndex(), "Socket time out exception even after max
                // retries, giving up for now");
                e.printStackTrace();
                ds.close();
                handleResponseError(e, udp);
            }

        }

    }

    private void handleResponse(RaidaResponse response, byte[] lMsg, int commandCode) throws Exception {

        String hex = Utils.bytesToHex(lMsg);
        byte[] statusBytes = { response.getStatus() };
        String status = Utils.bytesToHex(statusBytes);
        if (status.equals("1F") && retry < MAX_RETRIES) {
            System.out.println(
                    "RAIDA" + response.getRaidaId() + " Packet is lost or re-ordered, need to resend UDP packet");
            retry++;
            // need to resend everything
            // replayUDPCalls();
        } else {
            // if (!ticketing || commandCode == 11) {
            // raidaResponses.add(response);
            // Log.d("raidaresponse count for " + String.valueOf(commandCode),
            // String.valueOf(raidaResponses.size()));
            // }
            switch (commandCode) {
                case 0:
                case 1:
                    // handleCoinResponse(lMsg, hex, response, commandCode);
                    break;
                case 11:
                    // handleTicketResponse(lMsg, hex, response);
                    break;
                case 3:
                    // handleFixResponse(lMsg, hex, response);
                    break;
                case 4:
                    System.out.println("RAIDA " + response.raidaId + "Echo Response: " + hex.substring(0, 68));
                    break;
                case 83:
                    // Log.d("PEEK","Got Peek Response"+ hex);
                    break;
            }

            // Log.d("RAIDA" + response.getRaidaId(), "Going to callback with command code "
            // + commandCode);
            udpCallbacks.ReportBack(lMsg, hex, commandCode, response.getRaidaId(), mPassCount);
        }
    }

    private void handleResponseError(Exception e, UDPCall udp) {
        System.out.println(
                "RAIDA" + udp.getIndex() + "Going to error callback with command code " + udp.getCommandCode());
        // udpCallbacks.ReportBackError(e, udp.getData(), udp.getCommandCode(),
        // udp.getIndex());
    }

    public EchoResult getEchoResult() {
        return echoResult;
    }

}
