package com.cloudcoin2.wallet.Utils;

import static com.cloudcoin2.wallet.Utils.EncryptionOutputKt.encrypt;

import android.util.Log;

import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.Model.RaidaResponse;
import com.cloudcoin2.wallet.Model.SerialAn;
import com.cloudcoin2.wallet.Model.UDPCall;

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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * The RAIDA class implements the RAIDA version 2.0 protocol and implements the
 * following services -
 * Echo, Detect, Pown, Get Ticket, Fix Fracked, Fix Lost and can generate
 * Request headers compatible
 * with any RAIDA2 protocol service, and parse any RAIDA2 responses to extract
 * status codes and
 * other metadata.
 *
 * Coded by Partha Dasgupta - last updated May 2022
 */

public class RAIDA {

    private static final int MIN_PASS = 16;
    private static final String SEPERATOR = Integer.toBinaryString((Integer.parseInt("3e3e", 16)));
    private static final int MAX_COINS = 28; // how many coins to pown/detect/find at a time
    private static final int MAX_DETECT = 50; // how many coins to fix at a time
    private static final int MAX_FIX = 290; // how many coins to fix at a time
    private static final int MAX_PACKETS = 64; // how many max packets in a single request
    private static final int CONNECTION_TIMEOUT = 3000; // 3 second timeout for fetching hosts file
    private static final int UDP_CONNECTION_TIMEOUT = 5000; // 5 second timeout for UDP response
    private static final int MAX_RETRIES = 3; // try maximum 3 times in case packets are lost

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

    private static final String[] COMMANDS = {
            "POWN",
            "Detect",
            "Find",
            "Fix",
            "Echo",
            "Validate Ticket",
            "Put Key",
            "Move Key",
            "Identify",
            "Request Move",
            "Recover",
            "Get Ticket",
            "Get Key",
            "N/A",
            "N/A",
            "Version",
            "News",
            "Logs",
            "N/A",
            "Report Lost",
            "ANG",
            "PANG",
            "ANGPANG",
            "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A",
            "FREE ID",
            "FREE ID"

    };
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static RAIDA raida;
    private final ArrayList<EchoStatus> mList = new ArrayList<>(25);
    private final String mPownStatus = "";
    public ArrayList<RaidaResponse> raidaResponses = new ArrayList<>();
    public int numFixRaidas = 0;
    ArrayList<RaidaItems> raidaLists;
    ArrayList<HashMap<String, byte[]>> proposedPans = new ArrayList<HashMap<String, byte[]>>();
    private List<CloudCoin> cloudCoins = new ArrayList<>();
    private List<UDPCall> udpCalls = new ArrayList<>();
    private boolean debug = false;
    private int retry = 0, mPassCount = 0, mResponseCount = 0;
    private String SERVER_URL = "https://guardian0.chelgu.cz/host.txt";
    private int numUDP = 1;
    private UDPCallBackInterface udpCallbacks;
    private ArrayList<byte[]> masterTickets;
    private ArrayList<CloudCoin> authenticCoins = new ArrayList<>();
    private int receiveRetry = 0;
    private int numFixableCoins = 0;
    private boolean ticketing = false;

    public RAIDA() {
        if (raida != null)
            throw new RuntimeException("Use getinstance to get an instance of the class");

    }

    public static RAIDA getInstance() {
        if (raida == null) {
            synchronized (RAIDA.class) {
                if (raida == null) {
                    raida = new RAIDA();
                }
            }
        }
        return raida;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Returns a bitset containing the values in bytes.
    public static BitSet fromByteArray(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    private static Boolean isBitSet(byte b, int bit) {
        return (b & (1 << bit)) != 0;
    }

    public static byte[][][] splitSerials(byte[][] arrayToSplit, int chunkSize) {
        if (chunkSize <= 0) {
            return null; // just in case :)
        }
        // first we have to check if the array can be split in multiple
        // arrays of equal 'chunk' size
        int rest = arrayToSplit.length % chunkSize; // if rest>0 then our last array will have less elements than the
                                                    // others
        // then we check in how many arrays we can split our input array
        int chunks = arrayToSplit.length / chunkSize + (rest > 0 ? 1 : 0); // we may have to add an additional array for
                                                                           // the 'rest'
        // now we know how many arrays we need and create our result array
        byte[][][] arrays = new byte[chunks][][];
        // we create our resulting arrays by copying the corresponding
        // part from the input array. If we have a rest (rest>0), then
        // the last array will have less elements than the others. This
        // needs to be handled separately, so we iterate 1 times less.
        for (int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++) {
            // this copies 'chunk' times 'chunkSize' elements into a new array
            arrays[i] = Arrays.copyOfRange(arrayToSplit, i * chunkSize, i * chunkSize + chunkSize);
        }
        if (rest > 0) { // only when we have a rest
            // we copy the remaining elements into the last chunk
            arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * chunkSize,
                    (chunks - 1) * chunkSize + rest);
        }
        return arrays; // that's it
    }

    public static byte[][][][] splitANorPAN(byte[][][] arrayToSplit, int chunkSize) {
        if (chunkSize <= 0) {
            return null; // just in case :)
        }
        // first we have to check if the array can be split in multiple
        // arrays of equal 'chunk' size
        int rest = arrayToSplit.length % chunkSize; // if rest>0 then our last array will have less elements than the
                                                    // others
        // then we check in how many arrays we can split our input array
        int chunks = arrayToSplit.length / chunkSize + (rest > 0 ? 1 : 0); // we may have to add an additional array for
                                                                           // the 'rest'
        // now we know how many arrays we need and create our result array
        byte[][][][] arrays = new byte[chunks][][][];
        // we create our resulting arrays by copying the corresponding
        // part from the input array. If we have a rest (rest>0), then
        // the last array will have less elements than the others. This
        // needs to be handled separately, so we iterate 1 times less.
        for (int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++) {
            // this copies 'chunk' times 'chunkSize' elements into a new array
            arrays[i] = Arrays.copyOfRange(arrayToSplit, i * chunkSize, i * chunkSize + chunkSize);
        }
        if (rest > 0) { // only when we have a rest
            // we copy the remaining elements into the last chunk
            arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * chunkSize,
                    (chunks - 1) * chunkSize + rest);
        }
        return arrays; // that's it
    }

    public void setDebug(boolean flag) {
        debug = flag;
    }

    public String getServerURL() {
        int index = new Random().nextInt(hosts.length);
        SERVER_URL = hosts[index];
        return SERVER_URL;
    }

    public String getServerList() {
        String url = getServerURL();
        Log.d("URL", url);
        try {
            URLConnection connection = (new URL(url)).openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(CONNECTION_TIMEOUT);
            connection.connect();

            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder html = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) {
                html.append(line).append("/n");
            }
            in.close();

            // String rawdata = html.substring(0, html.length() - 2);
            // Log.d("servertext", rawdata);
            String serverList = html.toString();
            if (serverList.length() == 0) {
                Log.e("RAIDA", "Unable to retrieve server list from: " + url + ", trying again with another");
                return getServerList();
            }
            return serverList;

        } catch (IOException e) {
            Log.e("RAIDA", "Unable to retrieve server list from: " + url + ", trying again with another");
            return getServerList();
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
            String[] data = serverList.split("/n");
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

    public void setRaidaList(ArrayList<RaidaItems> raidaListList) {
        raidaLists = raidaListList;
    }

    public byte[] generateHeader(int raidaID, int type, int udpnum, byte udpChecksum, boolean encryption, byte[] nonce,
            byte[] mSerialNo) {
        byte[] udpHeader = new byte[22];
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

        Log.d("HEADER", "UDP NUM:" + udpnum);
        if (encryption) {
            if (!((nonce.equals("null")) || (nonce.length == 0))) {

                if (mSerialNo.length == 3 && nonce.length == 3) {
                    mEnc = (byte) 1;
                    serialNo = mSerialNo;
                }

            }
        }
        udpHeader[0] = 0;
        udpHeader[1] = 0;
        udpHeader[2] = (byte) raidaID;
        udpHeader[3] = 0;
        udpHeader[4] = 0;
        udpHeader[5] = (byte) type;
        udpHeader[6] = udpChecksum;
        udpHeader[7] = 0;
        udpHeader[8] = coinType;
        udpHeader[9] = nonce[0];
        udpHeader[10] = nonce[1];
        udpHeader[11] = nonce[2];
        udpHeader[12] = (byte) 0xEE;
        udpHeader[13] = (byte) 0xFF;
        udpHeader[14] = size[0];
        udpHeader[15] = size[1];
        udpHeader[16] = mEnc;
        udpHeader[17] = 0;
        udpHeader[18] = 0;
        udpHeader[19] = serialNo[0];
        udpHeader[20] = serialNo[1];
        udpHeader[21] = serialNo[2];
        Log.d("HEADER", bytesToHex(udpHeader));

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

    private void makeTCPCall(UDPCall udp) {

        // final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Log.d("size", String.valueOf(raidaLists.size()));

                Log.d("TCP RAIDA " + udp.getIndex(), "Running CMD:" + COMMANDS[udp.getCommandCode()] + " ON Server:"
                        + raidaLists.get(udp.getIndex()).getServerAddress());
                Log.d("TCP RAIDA " + udp.getIndex(), "Request: " + bytesToHex(udp.getData()));
                // System.out.println(raidaLists.get(i).getPorts());

                Socket sendChannel = null;
                try {
                    sendChannel = new Socket(raidaLists.get(udp.getIndex()).getServerAddress(),
                            raidaLists.get(udp.getIndex()).getPorts());
                    OutputStream writer = sendChannel.getOutputStream();
                    BufferedOutputStream bufferedWriter = new BufferedOutputStream(writer);
                    byte[] fullData = udp.getData();
                    byte[] buffer = new byte[1024];

                    bufferedWriter.write(fullData, 0, fullData.length);
                    // Flush the data in the socket to the server
                    bufferedWriter.flush();

                    InputStream reader = sendChannel.getInputStream();
                    byte[] realData = new byte[500];
                    int countBytesRead = reader.read(realData);
                    RaidaResponse response = new RaidaResponse(realData, udp.getCommandCode());

                    Log.d("RAIDA " + response.getRaidaId(),
                            "Response to CMD " + COMMANDS[udp.getCommandCode()] + ": " + bytesToHex(realData));
                    handleResponse(response, realData, udp.getCommandCode());

                } catch (Exception e) {
                    e.printStackTrace();
                    handleResponseError(e, udp);
                }
            }
        });
        thread.start();

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

                Log.d("RAIDA " + udp.getIndex(), "Running CMD:" + COMMANDS[udp.getCommandCode()] + " ON Server:"
                        + raidaLists.get(udp.getIndex()).getServerAddress());
                Log.d("RAIDA " + udp.getIndex(), "Request: " + bytesToHex(udp.getData()));
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

                        Log.d("RAIDA" + udp.getIndex(),
                                "Attempt #0 to get response to CMD:" + COMMANDS[udp.getCommandCode()]);
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
            Log.d("RAIDA " + response.getRaidaId(),
                    "Response to CMD " + COMMANDS[udp.getCommandCode()] + ": " + bytesToHex(realData));
            ds.close();

            handleResponse(response, realData, udp.getCommandCode());
        } catch (SocketTimeoutException e) {

            if (retry < 2) {
                retry++;
                Log.d("RAIDA" + udp.getIndex(), "Socket time out exception, trying to receive again #" + receiveRetry);
                Thread.sleep(100);
                getUDPResponse(dp, udp, ds, retry);
            } else {
                Log.d("RAIDA" + udp.getIndex(), "Socket time out exception even after max retries, giving up for now");
                e.printStackTrace();
                ds.close();
                handleResponseError(e, udp);
            }

        }

    }

    private void handleResponseError(Exception e, UDPCall udp) {
        Log.d("RAIDA" + udp.getIndex(), "Going to error callback with command code " + udp.getCommandCode());
        udpCallbacks.ReportBackError(e, udp.getData(), udp.getCommandCode(), udp.getIndex());
    }

    private void handleResponse(RaidaResponse response, byte[] lMsg, int commandCode) throws Exception {

        String hex = bytesToHex(lMsg);
        byte[] statusBytes = { response.getStatus() };
        String status = bytesToHex(statusBytes);
        if (status.equals("1F") && retry < MAX_RETRIES) {
            Log.e("RAIDA" + response.getRaidaId(), "Packet is lost or re-ordered, need to resend UDP packet");
            retry++;
            // need to resend everything
            replayUDPCalls();
        } else {
            if (!ticketing || commandCode == 11) {
                raidaResponses.add(response);
                Log.d("raidaresponse count for " + String.valueOf(commandCode), String.valueOf(raidaResponses.size()));
            }
            switch (commandCode) {
                case 0:
                case 1:
                    handleCoinResponse(lMsg, hex, response, commandCode);
                    break;
                case 11:
                    handleTicketResponse(lMsg, hex, response);
                    break;
                case 3:
                    handleFixResponse(lMsg, hex, response);
                    break;
            }

            Log.d("RAIDA" + response.getRaidaId(), "Going to callback with command code " + commandCode);
            udpCallbacks.ReportBack(lMsg, hex, commandCode, response.getRaidaId(), mPassCount);
        }
    }

    private void replayUDPCalls() {
        List<UDPCall> localUDPCalls = new ArrayList<UDPCall>();
        localUDPCalls = udpCalls;
        udpCalls = new ArrayList<UDPCall>();
        for (int i = 0; i < localUDPCalls.size(); i++) {
            makeUdpCall(localUDPCalls.get(i));
        }
    }

    private void handleTicketResponse(byte[] lMsg, String hex, RaidaResponse response) {
        byte[] statusBytes = { response.getStatus() };
        String status = bytesToHex(statusBytes);
        int raidaId = response.getRaidaId();
        Log.d("RAIDA " + raidaId, "Status:" + status);
        byte[] masterTicket = response.getMasterTicket();
        if (masterTicket != null && masterTicket.length == 4) {
            Log.d("RAIDA " + raidaId, "Master Ticket:" + bytesToHex(masterTicket));

        }

    }

    private void handleFixResponse(byte[] lMsg, String hex, RaidaResponse response) {
        byte[] statusBytes = { response.getStatus() };
        String status = bytesToHex(statusBytes);
        int raidaId = response.getRaidaId();
        HashMap<String, byte[]> pans = proposedPans.get(raidaId);

        Log.d("RAIDA " + raidaId, "Status:" + status);
        for (int i = 0; i < cloudCoins.size(); i++) {
            cloudCoins.get(i).setPownResponse(response.getResponseBody());
            String index = bytesToHex(cloudCoins.get(i).getSerial());
            byte[] proposedPan = pans.get(index);

            switch (status) {
                case "F1": // all pass
                    cloudCoins.get(i).setPownStatus(raidaId, 1);
                    if (proposedPan != null && proposedPan.length == 16) {
                        Log.d("RAIDA " + response.getRaidaId(), "Setting PAN to " + bytesToHex(proposedPan));
                        cloudCoins.get(i).setPan(raidaId, proposedPan);
                    }
                    mPassCount++;
                    break;
                case "F2": // all fail
                    cloudCoins.get(i).setPownStatus(raidaId, 0);
                    break;
                case "F3": // mixed
                    byte[] responseBody = response.getResponseBody();
                    // every byte of body holds 8 responses; Therefore we need to find which byte
                    // contains this coins index.
                    int byteIndex = (int) Math.floor(i / 8);

                    // A C
                    // 0 0 0 0 0 0 0 1
                    // 0 0 0 0 0 0 1 1
                    // 1 0 0 0 0 0 0 0

                    // now we need to know which bit contains the value for this byte
                    int bitIndex = i % 8;
                    Log.d("RAIDA " + response.getRaidaId(), "response body:" + bytesToHex(responseBody));
                    Log.d("RAIDA " + response.getRaidaId(), "coin serial:" + index);
                    Log.d("RAIDA " + response.getRaidaId(), "byte index:" + byteIndex);
                    Log.d("RAIDA " + response.getRaidaId(), "bit index:" + bitIndex);
                    int testBitValue = isBitSet(responseBody[byteIndex], bitIndex) ? 1 : 0;
                    Log.d("RAIDA " + response.getRaidaId(), "bit index: " + bitIndex + " has value" + testBitValue);

                    // for (int j = 0; j < 8; j++) {
                    // int testBitValue = isBitSet(responseBody[byteIndex], j) ? 1 : 0;
                    // Log.d("RAIDA " + response.getRaidaId(), "bit index: " + j + " has value" +
                    // testBitValue);
                    // }

                    // if this bit is set, then pass, else fail
                    int bitValue = isBitSet(responseBody[byteIndex], bitIndex) ? 1 : 0;
                    cloudCoins.get(i).setPownStatus(raidaId, bitValue);
                    if (bitValue == 1) {
                        if (proposedPan != null && proposedPan.length == 16) {
                            Log.d("RAIDA " + response.getRaidaId(), "Setting PAN to " + bytesToHex(proposedPan));
                            cloudCoins.get(i).setPan(raidaId, proposedPan);
                        }

                    }
                    break;
                default: // no response, but this case will most likely not trigger, because if no
                    // response there will be a time out instead of proper callback
                    cloudCoins.get(i).setPownStatus(raidaId, -1);
                    break;
            }

        }

    }

    private void handleCoinResponse(byte[] lMsg, String hex, RaidaResponse response, int commandCode) {
        byte[] statusBytes = { response.getStatus() };
        String status = bytesToHex(statusBytes);
        int raidaId = response.getRaidaId();
        Log.d("RAIDA " + raidaId, "Status:" + status);

        for (int i = 0; i < cloudCoins.size(); i++) {
            if (commandCode == 1)
                try {
                    cloudCoins.get(i).copyAnsToPans();
                } catch (Exception e) {
                    Log.e("RAIDA", "Exception while copying ans to pans :" + e.getMessage());
                }
            cloudCoins.get(i).setPownResponse(response.getResponseBody());

            switch (status) {
                case "F1": // all pass
                    cloudCoins.get(i).setPownStatus(raidaId, 1);
                    mPassCount++;
                    break;
                case "F2": // all fail
                    cloudCoins.get(i).setPownStatus(raidaId, 0);
                    break;
                case "F3": // mixed
                    byte[] responseBody = response.getResponseBody();
                    // every byte of body holds 8 responses; Therefore we need to find which byte
                    // contains this coins index.
                    int byteIndex = (int) Math.floor(i / 8);

                    // A C
                    // 0 0 0 0 0 0 0 1
                    // 0 0 0 0 0 0 1 1
                    // 1 0 0 0 0 0 0 0

                    // now we need to know which bit contains the value for this byte
                    int bitIndex = i % 8;

                    Log.d("RAIDA " + response.getRaidaId(), "coin serial:" + bytesToHex(cloudCoins.get(i).getSerial()));
                    Log.d("RAIDA " + response.getRaidaId(), "byte index:" + byteIndex);
                    Log.d("RAIDA " + response.getRaidaId(), "bit index:" + bitIndex);
                    int testBitValue = isBitSet(responseBody[byteIndex], bitIndex) ? 1 : 0;
                    Log.d("RAIDA " + response.getRaidaId(), "bit index: " + bitIndex + " has value" + testBitValue);

                    // for (int j = 0; j < 8; j++) {
                    // int testBitValue = isBitSet(responseBody[byteIndex], bitIndex) ? 1 : 0;
                    // Log.d("RAIDA " + response.getRaidaId(), "bit index:" + j + " has value" +
                    // testBitValue);
                    // }

                    // if this bit is set, then pass, else fail
                    int bitValue = isBitSet(responseBody[byteIndex], bitIndex) ? 1 : 0;
                    cloudCoins.get(i).setPownStatus(raidaId, bitValue);
                    break;
                default: // no response, but this case will most likely not trigger, because if no
                    // response there will be a time out instead of proper callback
                    cloudCoins.get(i).setPownStatus(raidaId, -1);
                    break;
            }

        }

    }

    public byte[] generateNonce() {
        return generateRandom(12);
    }

    public byte[] generateChallenge() {
        byte[] challenge = generateRandom(12);
        byte[] checksumTotal = generateCRC32(challenge);
        byte[] checksum = new byte[4];
        System.arraycopy(checksumTotal, checksumTotal.length - 4, checksum, 0, 4);
        // Log.d("CRC32 4 bytes:",bytesToHex(checksum));

        // Log.d("RANDOM SIZE", challenge.length+"");
        // Log.d("CHECKSUM SIZE", checksum.length+"");
        byte[] mData = new byte[challenge.length + checksum.length];
        // Log.d("CHALLENGE SIZE", mData.length+"");
        // Log.d("FIRST", bytesToHex(challenge));
        ByteBuffer buff = ByteBuffer.wrap(mData);
        buff.put(challenge);
        buff.put(checksum);
        byte[] challengeData = buff.array();
        return challengeData;
    }

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private byte[] generateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        String enc = String.format("%08X", crc.getValue());
        Log.d("CRC32", "Checksum:" + enc);
        return longToBytes(crc.getValue());
    }

    public EncryptionOutput encryptBody(byte[] body, byte[] secret) {
        EncryptionOutput output = encrypt(new SecretKeySpec(secret, 0, secret.length, "AES"), body);

        return output;

    }

    public byte[] generatePan(String pan) {
        if (!debug)
            return generateRandom(16);
        else
            return generateRandom(16, pan);
    }

    public byte[] generatePan() {
        return generatePan(null);
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

    public List<CloudCoin> fixDebugCoins(List<CloudCoin> coins) {
        return fixDebugCoins(coins, null);
    }

    public List<CloudCoin> fixDebugCoins(List<CloudCoin> coins, String seed) {
        byte[] pan = generateRandom(16, seed);
        for (int i = 0; i < coins.size(); i++) {
            for (int index = 0; index < 25; index++) {
                coins.get(i).setAn(index, pan);
            }

        }
        return coins;
    }

    public byte[] generateRandom(int length) {
        return generateRandom(length, null);
    }

    public void echo() throws Exception {
        ticketing = false;
        Log.d("Echo", "Starting Echo task");
        raidaResponses.clear();
        String html = null;
        udpCalls = new ArrayList<>();
        retry = 0;
        if (raidaLists == null || raidaLists.size() == 0) {
            html = getServerList();
            if (html != null && html.length() > 0)
                Log.d("RAIDA", html);
            raidaLists = createServerList(html);
        }

        mList.clear();
        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] header = generateHeader(i, 4);
            byte[] body = new BigInteger(SEPERATOR, 2).toByteArray();
            byte[] mData = new byte[header.length + body.length];
            ByteBuffer buff = ByteBuffer.wrap(mData);
            buff.put(header);
            buff.put(body);
            byte[] echoData = buff.array();
            makeUdpCall(new UDPCall(echoData, i, 4));
        }
    }

    private void generateBody(int command, byte[][] mSerialno, byte[][][] an, byte[][][] inputPan) {

    }

    private void getCurrentBody(List<byte[]> body, int index) {

    }

    public void fixUDPCall(byte[][] mSerialno, int raidaId, boolean encryption) throws Exception {
        int udpNum = (int) Math.ceil((double) mSerialno.length / (double) MAX_FIX);
        numUDP = udpNum;
        byte udpCheckSum = (byte) 0;

        Log.d("HEADER", "Total Coins:" + mSerialno.length + ", max coins:" + MAX_FIX
                + ", udp num:" + udpNum);

        // maximum 64 packets in 1 request
        if (udpNum > MAX_PACKETS) {
            int numRequests = (int) Math.ceil((double) udpNum / (double) MAX_PACKETS);
            for (int i = 0; i < numRequests; i++) {
                int length = MAX_PACKETS * MAX_FIX;
                if (i == numRequests - 1)
                    length = mSerialno.length - (MAX_PACKETS * MAX_FIX * i);

                byte[][] chunkSerialno = new byte[length][3];
                System.arraycopy(mSerialno, 0, chunkSerialno, i * MAX_PACKETS * MAX_FIX,
                        length);
                fixUDPCall(chunkSerialno, i, encryption);
            }
        } else {

            udpCalls = new ArrayList<>();
            retry = 0;
            byte[][][] serials = splitSerials(mSerialno, MAX_FIX);
            byte[][][][] pgs = null;
            List<byte[]> allChallenges = new ArrayList<byte[]>();

            int totalBodySize = 0;
            byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();
            List<byte[]> allBody = new ArrayList<byte[]>();
            List<byte[]> allEncryptedBody = new ArrayList<byte[]>();
            List<byte[]> allNonce = new ArrayList<byte[]>();
            HashMap<String, byte[]> pans = new HashMap<String, byte[]>();

            if (serials != null) {
                for (int k = 0; k < serials.length; k++) {

                    byte[][] serialChunk = serials[k];
                    byte[] challenge = generateChallenge();

                    // 4 bytes x 25 raidas master ticket = 100 bytes, 3 bytes for each coin, 16
                    // bytes PG
                    int totalBodyLength = 100 + (3 * serialChunk.length) + 16;

                    // if first packet, then also include the 16 byte challenge in length
                    if (k == 0) {
                        // Log.d("RAIDA" + raidaId, "Body size without challenge and footer= " +
                        // serialChunk.length + " x 3 + 100 + 16 =" + totalBodyLength);
                        // Log.d("RAIDA" + raidaId, "Body size with challenge=" + totalBodyLength + "+"
                        // + challenge.length);

                        totalBodyLength += challenge.length;
                    }

                    totalBodySize += totalBodyLength;

                    byte[] bufferBody = new byte[totalBodyLength];
                    ByteBuffer buff = ByteBuffer.wrap(bufferBody);

                    // if first packet, add the challenge
                    if (k == 0) {
                        buff.put(challenge);
                        // Log.d("RAIDA " + raidaId, "FULL CHALLENGE:" + bytesToHex(challenge));
                    }
                    // for fix PG use true random or it gives error
                    boolean oldFlag = this.debug;
                    if (this.debug)
                        this.debug = false;
                    byte[] pg = generateRandom(16);
                    this.debug = oldFlag;

                    for (int j = 0; j < serialChunk.length; j++) {
                        // Log.d("RAIDA " + raidaId, "SERIAL:" + bytesToHex(serialChunk[j]));
                        // Log.d("RAIDA " + raidaId, "PG:" + bytesToHex(pg));
                        // copy serial to body
                        buff.put(serialChunk[j]);
                        byte[] pan = pgToPan(raidaId, pg, serialChunk[j]);
                        // Log.d("RAIDA " + raidaId, "proposed PAN via PG:" + bytesToHex(pan));
                        pans.put(bytesToHex(serialChunk[j]), pan);
                    }
                    buff.put(pg);
                    if (masterTickets.size() != 25) {
                        throw new Exception("Need master tickets from each RAIDA to process fixing");
                    }

                    for (int r = 0; r < masterTickets.size(); r++) {
                        if (masterTickets.get(r) != null)
                            buff.put(masterTickets.get(r));

                    }

                    byte[] totalPownBody = buff.array();
                    byte[] nonce = null;
                    byte[] encryptedBody = totalPownBody;

                    // TODO remove hardcoded encryption false, and use actual AN of id coin as
                    // secret
                    encryption = false;
                    if (encryption) {
                        EncryptionOutput output = encryptBody(totalPownBody, new byte[16]);
                        nonce = output.getIv();
                        encryptedBody = output.getCiphertext();
                        allNonce.add(nonce);
                    }
                    allBody.add(totalPownBody);
                    allEncryptedBody.add(encryptedBody);

                }
            }
            proposedPans.get(raidaId).putAll(pans);

            if (udpNum > 0) // checksum calculation needed only for multi packet calls
            {
                totalBodySize = totalBodySize + 2; // to include the trailer bytes
                if (encryption) {

                    udpCheckSum = generateCheckSum(allEncryptedBody, totalBodySize, raidaId);
                } else {
                    udpCheckSum = generateCheckSum(allBody, totalBodySize, raidaId);
                }
            }

            for (int k = 0; k < serials.length; k++) {
                byte[] encryptedBody = allBody.get(k);
                byte[] nonce = null;
                byte[] header = null;
                if (encryption) {
                    encryptedBody = allEncryptedBody.get(k);
                    nonce = allNonce.get(k);
                }

                int dataLength = encryptedBody.length;
                // if first packet, include the header in length
                if (k == 0) {
                    header = generateHeader(raidaId, 3, udpNum, udpCheckSum, encryption, nonce, mSerialno[0]);
                    dataLength += header.length;
                }

                // if last packet, include the footer in length
                if (k == (serials.length - 1))
                    dataLength += footer.length;

                byte[] mData = new byte[dataLength];

                ByteBuffer commonBuffer = ByteBuffer.wrap(mData);
                // if first packet, put in the header
                if (k == 0)
                    commonBuffer.put(header);
                commonBuffer.put(encryptedBody);

                // if last packet, put in the separator
                if (k == (serials.length - 1))
                    commonBuffer.put(footer);

                byte[] commonData = commonBuffer.array();

                Log.d("RAIDA" + raidaId, "MAKING UDP CALL " + k + " with data " + bytesToHex(commonData));
                boolean ignore = true;
                if (k == (serials.length - 1))
                    ignore = false;
                if (udpNum > 0)
                    Thread.sleep(100);
                makeUdpCall(new UDPCall(commonData, raidaId, 3), ignore);

            }

        }

    }

    // common function to handle pown, detect, find and get ticket
    public void commonUDPCall(int command, byte[][] mSerialno, byte[][][] an, byte[][][] inputPan,
            boolean encryption)
            throws Exception {
        int max_coins = MAX_COINS;
        if (command == 1)
            max_coins = MAX_DETECT;

        int udpNum = (int) Math.ceil((double) mSerialno.length / (double) max_coins);
        numUDP = udpNum;
        byte udpCheckSum = (byte) 0;

        Log.d("HEADER", "Total Coins:" + mSerialno.length + ", max coins:" + max_coins
                + ", udp num:" + udpNum);
        // maximum 64 packets in 1 request
        if (udpNum > MAX_PACKETS) {
            int numRequests = (int) Math.ceil((double) udpNum / (double) MAX_PACKETS);
            for (int i = 0; i < numRequests; i++) {
                int length = MAX_PACKETS * max_coins;
                if (i == numRequests - 1)
                    length = mSerialno.length - (MAX_PACKETS * max_coins * i);

                byte[][] chunkSerialno = new byte[length][3];
                System.arraycopy(mSerialno, 0, chunkSerialno, i * MAX_PACKETS * max_coins,
                        length);

                byte[][][] chunkAn = new byte[length][25][16];
                System.arraycopy(an, 0, chunkAn, i * MAX_PACKETS * max_coins,
                        length);

                byte[][][] chunkInputPan = inputPan;
                if (inputPan != null) {
                    chunkInputPan = new byte[length][25][16];
                    System.arraycopy(an, 0, chunkAn, i * MAX_PACKETS * max_coins,
                            length);

                }
                commonUDPCall(command, chunkSerialno, chunkAn, chunkInputPan, encryption);
            }
        } else {

            udpCalls = new ArrayList<>();
            retry = 0;

            int bodySize = 19;
            if (command == 0 || command == 2) // pown or find needs 35 bytes, detect and get ticket needs 19 bytes
                bodySize = 35;
            byte[][][] serials = splitSerials(mSerialno, max_coins);
            byte[][][][] ans = splitANorPAN(an, max_coins);
            byte[][][][] pans = null;

            if (mSerialno.length != an.length) {
                throw new Exception("coin and an length not same");
            }
            if (serials.length != ans.length) {
                throw new Exception("coin and an length not same");
            }

            if (command == 2) {
                if (inputPan == null || inputPan.length == 0) {
                    throw new Exception("pan needed for find operation");
                } else {
                    pans = splitANorPAN(inputPan, max_coins);
                    if (pans.length != ans.length) {
                        throw new Exception("pan and an length not same");
                    }
                }

            }

            /**
             * For Multi packet powning (typically powning more than 28 coins at one go,
             * we need to use multiple UDP packets. When using multiple UDP packets, here
             * are the rules -
             * 1. Only First packet contains header and challenge. This packet does NOT
             * contain the
             * separator (3E3E).
             * 2. Intermediary packets contain just the body (no header or challenge or
             * separator)
             * 3. Last packet contains just the body(no header or challenge) AND the
             * separator.
             *
             * Even for multi packet powning, challenge is generated only once (for the
             * first packet)
             * Therefore, only 25 challenges are needed 1 for each RAIDA. We should generate
             * them
             * Beforehand, because we need to generate the CRC32 of the entire body (all
             * packets
             * combined) to be put in the header for multi packet request, at byte position
             * 6
             *
             * We also need to generate the PANS of all the coins preemptively, so that we
             * can generate
             * the total body, and calculate the CRC32.
             *
             * Also, at one time when sending a multi packet request, we can send only 64
             * packets at a time.
             * Therefore, if you are powning > 28x64 coins at a time, you need to split it
             * into arrays
             * of 28x64 coins at a time, then consider each of them a separate multi packet
             * powning
             * request.
             */
            List<byte[]> allChallenges = new ArrayList<>();
            for (int i = 0; i < raidaLists.size(); i++) {
                allChallenges.add(generateChallenge());

            }
            for (int i = 0; i < raidaLists.size(); i++) {
                int totalBodySize = 0;
                byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();

                List<byte[]> allBody = new ArrayList<>();
                List<byte[]> allEncryptedBody = new ArrayList<>();
                List<byte[]> allNonce = new ArrayList<>();

                for (int k = 0; k < serials.length; k++) {

                    byte[][] serialChunk = serials[k];
                    byte[][][] anChunk = ans[k];
                    byte[][][] panChunk = null;
                    byte[] challenge = allChallenges.get(i);

                    if (command == 2)
                        panChunk = pans[k];
                    int totalBodyLength = bodySize * serialChunk.length;

                    // if first packet, then also include the challenge in length
                    if (k == 0) {
                        Log.d("RAIDA" + i, "Body size without challenge and footer= " + serialChunk.length + "x"
                                + bodySize + "=" + totalBodyLength);
                        Log.d("RAIDA" + i, "Body size with challenge=" + totalBodyLength + "+" + challenge.length);

                        totalBodyLength += challenge.length;
                    }

                    totalBodySize += totalBodyLength;

                    byte[] bufferBody = new byte[totalBodyLength];
                    ByteBuffer buff = ByteBuffer.wrap(bufferBody);

                    // if first packet, add the challenge
                    if (k == 0) {
                        buff.put(challenge);
                        Log.d("RAIDA " + i, "FULL CHALLENGE:" + bytesToHex(challenge));
                    }

                    for (int j = 0; j < serialChunk.length; j++) {
                        byte[] body = new byte[bodySize];

                        // Log.d("RAIDA " + i, "SERIAL:" + bytesToHex(serialChunk[j]));

                        // copy serial to body
                        System.arraycopy(serialChunk[j], 0, body, 0, serialChunk[j].length);

                        // copy an to body after serial
                        System.arraycopy(anChunk[j][i], 0, body, serialChunk[j].length,
                                anChunk[j][i].length);
                        // Log.d("RAIDA " + i, "AN:" + bytesToHex(anChunk[j][i]));

                        if (command == 0) // if we are powning copy pan to body after an
                        {
                            byte[] pan = new byte[16];
                            if (!debug)
                                pan = generatePan();
                            else
                                pan = generatePan(bytesToHex(anChunk[j][i]));
                            // Log.d("RAIDA " + i, "PAN:" + bytesToHex(pan));
                            int coinIndex = k * 28 + j;
                            // Log.d("RAIDA " + i, "setting PAN for coin index:" + coinIndex);
                            if (cloudCoins.size() >= coinIndex)
                                cloudCoins.get(coinIndex).setPan(i, pan);
                            else
                                throw new Exception("Bug in calculating coin index");

                            int offset = serialChunk[j].length + anChunk[j][i].length; // ideally 19
                            System.arraycopy(pan, 0, body, offset,
                                    pan.length);

                        }

                        if (command == 2) // if we are finding copy pan to body after an
                        {
                            int offset = serialChunk[j].length + anChunk[j][i].length; // ideally 19
                            System.arraycopy(panChunk[j][i], 0, body, offset,
                                    panChunk[j][i].length);
                        }
                        buff.put(body);

                    }

                    byte[] totalPownBody = buff.array();
                    byte[] nonce = null;
                    byte[] encryptedBody = totalPownBody;

                    if (encryption) {
                        EncryptionOutput output = encryptBody(totalPownBody, an[0][i]);
                        nonce = output.getIv();
                        encryptedBody = output.getCiphertext();
                        allNonce.add(nonce);
                    }
                    allBody.add(totalPownBody);
                    allEncryptedBody.add(encryptedBody);
                }
                if (udpNum > 0) // checksum calculation needed only for multi packet calls
                {
                    totalBodySize = totalBodySize + 2; // to include the trailer bytes

                    if (encryption) {

                        udpCheckSum = generateCheckSum(allEncryptedBody, totalBodySize, i);
                    } else {
                        udpCheckSum = generateCheckSum(allBody, totalBodySize, i);
                    }
                }

                for (int k = 0; k < serials.length; k++) {

                    byte[] encryptedBody = allBody.get(k);
                    byte[] nonce = null;
                    byte[] header = null;
                    if (encryption) {
                        encryptedBody = allEncryptedBody.get(k);
                        nonce = allNonce.get(k);
                    }

                    int dataLength = encryptedBody.length;
                    // if first packet, include the header in length
                    if (k == 0) {
                        header = generateHeader(i, command, udpNum, udpCheckSum, encryption, nonce, mSerialno[0]);
                        dataLength += header.length;
                    }

                    // if last packet, include the footer in length
                    if (k == (serials.length - 1))
                        dataLength += footer.length;

                    byte[] mData = new byte[dataLength];

                    ByteBuffer commonBuffer = ByteBuffer.wrap(mData);
                    // if first packet, put in the header
                    if (k == 0)
                        commonBuffer.put(header);
                    commonBuffer.put(encryptedBody);

                    // if last packet, put in the separator
                    if (k == (serials.length - 1))
                        commonBuffer.put(footer);

                    byte[] commonData = commonBuffer.array();
                    Log.d("RAIDA" + i, "MAKING UDP CALL " + k + " with data " + bytesToHex(commonData));
                    boolean ignore = true;
                    if (k == (serials.length - 1))
                        ignore = false;
                    if (udpNum > 0)
                        Thread.sleep(100);
                    makeUdpCall(new UDPCall(commonData, i, command), ignore);
                }

            }
        }

    }

    public void fixTCPCall(byte[][] mSerialno, int raidaId, boolean encryption) throws Exception {
        if (masterTickets.size() != 25) {
            throw new Exception("Need master tickets from each RAIDA to process fixing");
        }

        byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();

        byte[] header;

        byte[] fullBody;
        byte[] fullEncryptedBody;
        byte[] challenge = generateChallenge();
        byte udpCheckSum = (byte) 0;
        int totalPacketLength = 0;

        // 4 bytes x 25 raidas master ticket = 100 bytes, 3 bytes for each coin, 16
        // bytes PG + challenge
        int totalBodyLength = 100 + (3 * mSerialno.length) + 16 + challenge.length;
        HashMap<String, byte[]> pans = new HashMap<String, byte[]>();
        // first make the body

        byte[] bufferBody = new byte[totalBodyLength];
        ByteBuffer buff = ByteBuffer.wrap(bufferBody);

        // first put challenge to body
        buff.put(challenge);

        // for fix PG use true random or it gives error
        boolean oldFlag = this.debug;
        if (this.debug)
            this.debug = false;
        byte[] pg = generateRandom(16);
        this.debug = oldFlag;

        // loop through the coins to prepare the body by putting all serial numbers
        for (int j = 0; j < mSerialno.length; j++) {
            // Log.d("RAIDA " + raidaId, "SERIAL:" + bytesToHex(serialChunk[j]));
            // Log.d("RAIDA " + raidaId, "PG:" + bytesToHex(pg));
            // copy serial to body
            buff.put(mSerialno[j]);
            byte[] pan = pgToPan(raidaId, pg, mSerialno[j]);
            // Log.d("RAIDA " + raidaId, "proposed PAN via PG:" + bytesToHex(pan));
            pans.put(bytesToHex(mSerialno[j]), pan);
        }
        // now put the pan generator
        buff.put(pg);

        // now put all master tickets to body
        for (int r = 0; r < masterTickets.size(); r++) {
            if (masterTickets.get(r) != null)
                buff.put(masterTickets.get(r));

        }

        fullBody = buff.array();
        byte[] nonce = null;
        fullEncryptedBody = fullBody;

        // TODO remove hardcoded encryption false, and use actual AN of id coin as
        // secret
        encryption = false;
        if (encryption) {
            EncryptionOutput output = encryptBody(fullBody, new byte[16]);
            nonce = output.getIv();
            fullEncryptedBody = output.getCiphertext();
        }
        proposedPans.get(raidaId).putAll(pans);

        header = generateHeader(raidaId, 3, fullEncryptedBody.length + footer.length,
                udpCheckSum, encryption, nonce, mSerialno[0]);

        totalPacketLength = header.length + fullEncryptedBody.length + footer.length;

        byte[] mData = new byte[totalPacketLength];

        ByteBuffer commonBuffer = ByteBuffer.wrap(mData);

        commonBuffer.put(header);
        commonBuffer.put(fullEncryptedBody);
        commonBuffer.put(footer);
        byte[] commonData = commonBuffer.array();
        Log.d("RAIDA" + raidaId, "MAKING TCP CALL with data " + bytesToHex(commonData));

        makeTCPCall(new UDPCall(commonData, raidaId, 3));
    }

    public void commonTCPCall(int command, byte[][] mSerialno, byte[][][] an, byte[][][] inputPan,
            boolean encryption)
            throws Exception {
        int bodySize = 19;
        if (command == 0 || command == 2) // pown or find needs 35 bytes, detect and get ticket needs 19 bytes
            bodySize = 35;

        if (mSerialno.length != an.length) {
            throw new Exception("coin and an length not same");
        }

        if (command == 2) {
            if (inputPan == null || inputPan.length == 0) {
                throw new Exception("pan needed for find operation");
            } else {

                if (an.length != inputPan.length) {
                    throw new Exception("pan and an length not same");
                }
            }

        }

        List<byte[]> allChallenges = new ArrayList<>();
        for (int i = 0; i < raidaLists.size(); i++) {
            allChallenges.add(generateChallenge());

        }
        for (int i = 0; i < raidaLists.size(); i++) {
            int totalPacketLength;

            byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();
            byte[] header;

            byte[] fullBody;
            byte[] fullEncryptedBody;

            byte[] challenge = allChallenges.get(i);
            byte[] nonce = null;

            int totalBodyLength = bodySize * mSerialno.length;
            Log.d("RAIDA" + i, "Body size without challenge and footer= " + mSerialno.length + "x" + bodySize + "="
                    + totalBodyLength);
            Log.d("RAIDA" + i, "Body size with challenge=" + totalBodyLength + "+" + challenge.length);

            totalBodyLength += challenge.length;

            byte[] bufferBody = new byte[totalBodyLength];
            ByteBuffer buff = ByteBuffer.wrap(bufferBody);

            // first put the challenge in the body
            buff.put(challenge);
            Log.d("RAIDA " + i, "FULL CHALLENGE:" + bytesToHex(challenge));

            // then loop through the coins to form the rest of the body

            for (int k = 0; k < mSerialno.length; k++) {

                byte[] serialChunk = mSerialno[k];
                byte[][] anChunk = an[k];
                byte[][] panChunk = null;

                if (command == 2)
                    panChunk = inputPan[k];

                byte[] body = new byte[bodySize];

                // copy serial to body
                System.arraycopy(serialChunk, 0, body, 0, serialChunk.length);

                // copy an to body after serial
                System.arraycopy(anChunk[i], 0, body, serialChunk.length,
                        anChunk[i].length);
                // Log.d("RAIDA " + i, "AN:" + bytesToHex(anChunk[j][i]));

                if (command == 0) // if we are powning copy pan to body after an
                {
                    byte[] pan = new byte[16];
                    if (!debug)
                        pan = generatePan();
                    else
                        pan = generatePan(bytesToHex(anChunk[i]));
                    // Log.d("RAIDA " + i, "PAN:" + bytesToHex(pan));
                    int coinIndex = k;
                    // Log.d("RAIDA " + i, "setting PAN for coin index:" + coinIndex);
                    if (cloudCoins.size() >= coinIndex)
                        cloudCoins.get(coinIndex).setPan(i, pan);
                    else
                        throw new Exception("Bug in calculating coin index");

                    int offset = serialChunk.length + anChunk[i].length; // ideally 19
                    System.arraycopy(pan, 0, body, offset,
                            pan.length);

                }

                if (command == 2) // if we are finding copy pan to body after an
                {
                    int offset = serialChunk.length + anChunk[i].length; // ideally 19
                    System.arraycopy(panChunk[i], 0, body, offset,
                            panChunk[i].length);
                }
                // put body to buff
                buff.put(body);

            }
            fullBody = buff.array();
            fullEncryptedBody = fullBody;

            if (encryption) {
                EncryptionOutput output = encryptBody(fullBody, an[0][i]);
                nonce = output.getIv();
                fullEncryptedBody = output.getCiphertext();
            }

            // generate header using id on in last param instead of first coin
            header = generateHeader(i, command, fullEncryptedBody.length + footer.length, (byte) 0, encryption, nonce,
                    mSerialno[0]);

            totalPacketLength = header.length + fullEncryptedBody.length + footer.length;

            byte[] mData = new byte[totalPacketLength];

            ByteBuffer commonBuffer = ByteBuffer.wrap(mData);

            commonBuffer.put(header);
            commonBuffer.put(fullEncryptedBody);
            commonBuffer.put(footer);
            byte[] commonData = commonBuffer.array();
            Log.d("RAIDA" + i, "MAKING TCP CALL with data " + bytesToHex(commonData));

            makeTCPCall(new UDPCall(commonData, i, command));
        }

    }

    private byte[] pgToPan(int raida, byte[] panGenerator, byte[] serial) {
        final String MD5 = "MD5";
        String panString = String.valueOf(raida) + Integer.parseInt(bytesToHex(serial), 16) + bytesToHex(panGenerator);
        Log.d("RAIDA" + raida, "PAN String for MD5:" + panString);
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(panString.getBytes());
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte generateCheckSum(List<byte[]> allBody, int bodySize, int raida) {
        Log.d("RAIDA" + raida, "Body size:" + bodySize);
        Log.d("RAIDA" + raida, "Generating checksum for multi packet UDP");
        byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();
        byte[] mData = new byte[bodySize];
        ByteBuffer commonBuffer = ByteBuffer.wrap(mData);
        for (int i = 0; i < allBody.size(); i++) {
            commonBuffer.put(allBody.get(i));
        }
        commonBuffer.put(footer);

        byte[] commonData = commonBuffer.array();
        byte[] checksumTotal = generateCRC32(commonData);

        byte[] checksum = new byte[4];
        System.arraycopy(checksumTotal, checksumTotal.length - 4, checksum, 0, 4);

        byte[] checksumByte = { checksum[3] };
        // Log.d("RAIDA" + raida, "Calculating checksum of body:" +
        // bytesToHex(commonData));
        // Log.d("RAIDA" + raida, "CRC32 Checksum of body:" + bytesToHex(checksumByte));

        return checksum[3];
    }

    public void pown(byte[][] mSerialno, byte[][][] an) throws Exception {

        cloudCoins = new ArrayList<>();
        cloudCoins = binaryToCoins(mSerialno, an);
        raidaResponses = new ArrayList<>();
        commonTCPCall(0, mSerialno, an, null, false);

    }

    private SerialAn extractCoins(List<CloudCoin> coins) {

        SerialAn extractedCoins = new SerialAn();
        cloudCoins = new ArrayList<>();
        for (int i = 0; i < coins.size(); i++) {
            CloudCoin coin = coins.get(i);
            if (coin.isValid()) {
                cloudCoins.add(coin);
                extractedCoins.add(coin.getSerial(), coin.getAns());

            }
        }
        return extractedCoins;

    }

    public void pown(List<CloudCoin> coins) throws Exception {
        ticketing = false;
        mPassCount = 0;
        mResponseCount = 0;
        cloudCoins = new ArrayList<>();
        raidaResponses = new ArrayList<>();

        SerialAn extractedCoins = extractCoins(coins);
        int size = extractedCoins.size();
        if (size == 0) {
            throw new Exception("Coins not extractable to valid byte arrays");
        }

        commonTCPCall(0, extractedCoins.getSerials(), extractedCoins.getAns(), null, false);

        /*
         * List<byte[]> serials = new ArrayList<>();
         * List<byte[][]> ans = new ArrayList<>();
         * 
         * 
         * for (int i = 0; i < coins.size(); i++) {
         * byte[] serial = new byte[3];
         * byte[][] an = new byte[25][16];
         * CloudCoin coin = coins.get(i);
         * if (coin.isValid()) {
         * cloudCoins.add(coin);
         * System.arraycopy(coin.getSerial(), 0, serial, 0, 3);
         * for (int j = 0; j < 25; j++) {
         * System.arraycopy(coin.getAns()[j], 0, an[j], 0, 16);
         * }
         * serials.add(serial);
         * ans.add(an);
         * }
         * }
         * if (serials.size() > 0 && ans.size() > 0 && ans.size() == serials.size()) {
         * byte[][] mSerialno = new byte[serials.size()][3];
         * byte[][][] mAns = new byte[ans.size()][25][16];
         * for (int i = 0; i < serials.size(); i++) {
         * System.arraycopy(serials.get(i), 0, mSerialno[i], 0, 3);
         * for (int j = 0; j < 25; j++) {
         * System.arraycopy(ans.get(i)[j], 0, mAns[i][j], 0, 16);
         * }
         * }
         * 
         * commonUDPCall(0, mSerialno, mAns, null, false);
         * 
         * } else {
         * throw new Exception("Coins not extractable to valid byte arrays");
         * }
         */

    }

    public void detect(byte[][] mSerialno, byte[][][] an) throws Exception {
        cloudCoins = new ArrayList<>();
        raidaResponses = new ArrayList<>();
        commonTCPCall(1, mSerialno, an, null, false);

    }

    public void detect(List<CloudCoin> coins) throws Exception {
        ticketing = false;
        mPassCount = 0;
        mResponseCount = 0;
        cloudCoins = new ArrayList<>();
        raidaResponses = new ArrayList<>();

        SerialAn extractedCoins = extractCoins(coins);
        int size = extractedCoins.size();
        if (size == 0) {
            throw new Exception("Coins not extractable to valid byte arrays");
        }

        commonTCPCall(1, extractedCoins.getSerials(), extractedCoins.getAns(), null, false);

    }

    public void getTicket(byte[][] mSerialno, byte[][][] an) throws Exception {
        cloudCoins = new ArrayList<>();
        raidaResponses = new ArrayList<>();
        commonTCPCall(11, mSerialno, an, null, false);

    }

    public void getTicket() throws Exception {
        getTicket(null);
    }

    public void getTicket(List<CloudCoin> coins) throws Exception {
        ticketing = true;

        if (coins == null)
            coins = cloudCoins;
        raidaResponses = new ArrayList<>();
        mPassCount = 0;
        mResponseCount = 0;
        cloudCoins = new ArrayList<>();
        SerialAn extractedCoins = extractCoins(coins);
        int size = extractedCoins.size();
        if (size == 0) {
            throw new Exception("Coins not extractable to valid byte arrays");
        }
        Log.d("TICKET", "Trying to get tickets for fixing " + size + " coins");
        commonTCPCall(11, extractedCoins.getSerials(), extractedCoins.getAns(), null, false);

    }

    public void find(byte[][] mSerialno, byte[][][] an, byte[][][] pan) throws Exception {
        commonTCPCall(0, mSerialno, an, pan, false);

    }

    public void fix(byte[][] mSerialno, byte[][] tickets) {
        ticketing = false;
        int udpNum = (int) Math.ceil(mSerialno.length / MAX_FIX);
        byte[] footer = new BigInteger(SEPERATOR, 2).toByteArray();
        byte[][][] serials = splitSerials(mSerialno, MAX_FIX);
        retry = 0;
        udpCalls = new ArrayList<>();
        for (int i = 0; i < raidaLists.size(); i++) {
            for (int k = 0; k < serials.length; k++) {
                byte[][] serialChunk = serials[k];
                // random 16 byte password generator
                byte[] generator = generateRandom(16);
                byte[] challenge = generateChallenge();

                byte[] bufferBody = new byte[challenge.length + (3 * serialChunk.length) + generator.length +
                        (tickets.length * tickets[0].length)];
                ByteBuffer buff = ByteBuffer.wrap(bufferBody);

                // put the challenge
                buff.put(challenge);

                // put the serial numbers in this chunk
                for (int j = 0; j < serialChunk.length; j++) {
                    buff.put(serialChunk[j]);
                }

                // put the generator
                buff.put(generator);

                // put the tickets
                for (int x = 0; x < 25; x++) {
                    buff.put(tickets[x]);
                }

                byte[] totalFixBody = buff.array();
                // EncryptionOutput output = encryptBody(totalFixBody,an[0][i]);
                // byte[] nonce= output.getIv();
                byte[] encryptedBody = totalFixBody;// output.getCiphertext();
                byte[] header = generateHeader(i, 3, udpNum);
                byte[] mData = new byte[header.length + encryptedBody.length + footer.length];

                ByteBuffer fixBuffer = ByteBuffer.wrap(mData);
                fixBuffer.put(header);
                fixBuffer.put(encryptedBody);
                fixBuffer.put(footer);
                byte[] fixData = fixBuffer.array();
                makeUdpCall(new UDPCall(fixData, i, 3));

            }

        }

    }

    public void fix() throws Exception {
        fix(null);
    }

    public int getNumFixableCoins() {
        return numFixableCoins;
    }

    public void fix(List<CloudCoin> coins) throws Exception {
        ticketing = false;
        masterTickets = new ArrayList<>();
        proposedPans.clear();
        if (coins == null)
            coins = cloudCoins;

        if (raidaResponses.size() != 25) {
            Log.e("RAIDA", "Ticket Response Size(should be 25):" + raidaResponses.size());
            throw new Exception("Number of RAIDA responses to fetch master ticket from must be exactly 25, found: "
                    + raidaResponses.size());
        }
        // first get the tickets out of responses
        for (int i = 0; i < 25; i++) {
            masterTickets.add(raidaResponses.get(i).getMasterTicket());
            proposedPans.add(new HashMap<String, byte[]>());
        }
        // now we can reset
        cloudCoins = new ArrayList<>();
        raidaResponses = new ArrayList<>();
        mPassCount = 0;
        mResponseCount = 0;
        numFixableCoins = 0;
        authenticCoins.clear();

        SerialAn extractedCoins = extractCoins(coins);
        int size = extractedCoins.size();
        if (size == 0) {
            throw new Exception("Coins not extractable to valid byte arrays");
        }

        ArrayList<ArrayList<CloudCoin>> fixableCoins = new ArrayList<>();

        Log.d("RAIDA", "Checking which of the selected coins are fixable among " + cloudCoins.size() + " coins");

        for (int i = 0; i < cloudCoins.size(); i++) {
            CloudCoin coin = cloudCoins.get(i);
            if (coin.getPassCount() < MIN_PASS) {

                Log.d("RAIDA", "Coin:" + bytesToHex(coin.getSerial()) + " has " + coin.getPassCount()
                        + " authentic raidas and hence excluded from fixing");
                continue;
            }
            coin.copyAnsToPans();
            authenticCoins.add(coin);

            for (int j = 0; j < 25; j++) {
                if (fixableCoins.size() < (j + 1))
                    fixableCoins.add(new ArrayList<>());

                if (coin.getPownStatus()[j] != (byte) 1) {

                    Log.d("RAIDA" + j, "Coin:" + bytesToHex(coin.getSerial()) + " queued for fixing");
                    fixableCoins.get(j).add(coin);
                } else {
                    Log.d("RAIDA" + j, "Coin:" + bytesToHex(coin.getSerial())
                            + " is already authentic on this raida and hence excluded from fixing");

                }

            }

        }
        numFixRaidas = 0;
        numFixableCoins = fixableCoins.size();
        if (fixableCoins.size() == 0) {
            Log.d("RAIDA", "No Fixable coins");
        }

        for (int i = 0; i < fixableCoins.size(); i++) {
            if (fixableCoins.get(i).size() > 0) // theres any coins to fix on this raida
            {
                Log.d("RAIDA" + i, "Fixable coins:" + fixableCoins.get(i).size());

                numFixRaidas++;
                Log.d("RAIDA" + i, "Trying to fix " + fixableCoins.get(i).size() + " coins");
                extractedCoins = extractCoins(fixableCoins.get(i));
                fixTCPCall(extractedCoins.getSerials(), i, false);
            } else {
                Log.d("RAIDA" + i, "No fixable coins for RAIDA " + i);
            }

        }

    }

    public int getNumFixRaidas() {
        return numFixRaidas;
    }

    public void setCallbacks(UDPCallBackInterface callbacks) {
        udpCallbacks = callbacks;
    }
    // COIN HEADER (0-31 bytes)
    // FT CL ID ID SP EN HS HS HS HS HS HS HS HS HS FL - see
    // https://github.com/worthingtonse/RAIDAX/blob/main/file_format.md for details.
    // for standard CC, will be all 0
    // RC RC RC RC RC RC RC RC RC RC RC RC RC RC RC RC - will be random or zeroes.
    // COIN BODY (32-448 bytes)
    // 32/33/34 = serial number, 35-47 (13 bytes) for powning status, 48 - 448 (400
    // bytes) = 16 bytes of an X 25 = 400 bytes

    public byte[] coinsToBinary(List<CloudCoin> coins) throws Exception {
        int coinLength = (coins.size() * 416) + 32;
        byte[] coinData = new byte[coinLength];
        for (int i = 0; i < coins.size(); i++) {
            byte[] coin = coinToBinary(coins.get(i));
            if (i == 0) {
                System.arraycopy(coin, 0, coinData, 0, 448);
            } else {
                int index = 32 + (i * 416);
                System.arraycopy(coin, 32, coinData, index, 416);
            }

        }
        return coinData;
    }

    public byte[] coinToBinary(CloudCoin coin) throws Exception {
        byte[] binary = new byte[448];
        if (!coin.isValid()) {
            throw new Exception(
                    "Invalid Cloud coin object cannot be converted on binary coin, wrong serial or ans data");
        }
        // fill first 32 bytes with 0s
        for (int i = 0; i < 32; i++) {
            binary[i] = (byte) 0;
        }

        binary[3] = (byte) 1;

        // fill serial number
        System.arraycopy(coin.getSerial(), 0, binary, 32, 3);

        // fill powning status with 0 for now.
        for (int i = 35; i < 48; i++) {
            binary[i] = (byte) 0;
        }

        // fill the ans with pans
        for (int i = 0; i < 25; i++) {
            if (bytesToHex(coin.getPans()[i]) == "00000000000000000000000000000000") {
                Log.e("Critical Error", "PAN is all 0 for coin: " + bytesToHex(coin.getSerial())
                        + " - this is likely a critical error");
            }
            // Log.d("TEST", "Setting PAN to" + bytesToHex(coin.getPans()[i]));
            System.arraycopy(coin.getPans()[i], 0, binary, 48 + (i * 16), 16);
        }
        return binary;
    }

    // extract coin from a single coin bytearray
    public CloudCoin binaryToCoin(byte[] binary) throws Exception {
        if (binary.length != 448) {
            throw new Exception("Invalid binary file");
        }

        byte[] serial = new byte[3];
        // extract serial
        System.arraycopy(binary, 32, serial, 0, 3);

        // extract ans
        byte[][] ans = new byte[25][16];

        for (int i = 0; i < 25; i++) {
            System.arraycopy(binary, 48 + (i * 16), ans[i], 0, 16);

        }
        return new CloudCoin(serial, ans, null);
    }

    // extract coin from a single or multi coin bytearray and return a list of coins
    public List<CloudCoin> binaryToCoins(byte[] binaries) throws Exception {
        if (binaries.length % 448 != 0 && (binaries.length - 32) % 416 != 0) {
            throw new Exception("Invalid binary file because file size is " + binaries.length);
        }
        int type = 0;

        if ((binaries.length - 32) % 416 == 0)
            type = 1;

        List<CloudCoin> coins = new ArrayList<>();
        int numCoins = type == 0 ? binaries.length / 448 : (binaries.length - 32) / 416;
        Log.d("POWN", "Found:" + numCoins + " coins");

        if (type == 0) {
            for (int j = 0; j < numCoins; j++) {
                byte[] binary = new byte[448];
                System.arraycopy(binaries, 0, binary, (j * 448), 448);

                byte[] serial = new byte[3];
                // extract serial
                System.arraycopy(binary, 32, serial, 0, 3);

                // extract ans
                byte[][] ans = new byte[25][16];

                for (int i = 0; i < 25; i++) {
                    System.arraycopy(binary, 48 + (i * 16), ans[i], 0, 16);
                    Log.d("POWN: " + bytesToHex(serial), "AN " + i + ": " + bytesToHex(ans[i]));

                }
                coins.add(new CloudCoin(serial, ans, null));
            }
        } else {
            for (int j = 0; j < numCoins; j++) {
                byte[] binary = new byte[416];
                Log.d("POWN", "Extracting coin:" + j);
                if (j == 0) {
                    System.arraycopy(binaries, 32, binary, 0, 416);
                } else {
                    int index = 32 + (j * 416);
                    Log.d("POWN: ", "COPY:" + index + " to " + (index + 416));
                    System.arraycopy(binaries, index, binary, 0, 416);
                }

                byte[] serial = new byte[3];
                // extract serial
                System.arraycopy(binary, 0, serial, 0, 3);

                // extract ans
                byte[][] ans = new byte[25][16];

                for (int i = 0; i < 25; i++) {
                    System.arraycopy(binary, 16 + (i * 16), ans[i], 0, 16);
                    Log.d("POWN: " + bytesToHex(serial), "AN " + i + ": " + bytesToHex(ans[i]));

                }
                coins.add(new CloudCoin(serial, ans, null));
            }
        }

        return coins;
    }

    // extract coin from already split serial/an array
    public List<CloudCoin> binaryToCoins(byte[][] serials, byte[][][] ans) throws Exception {
        if (serials.length <= 0 || ans.length <= 0 || serials.length != ans.length) {
            throw new Exception("Invalid binary file");
        }
        List<CloudCoin> coins = new ArrayList<>();
        int numCoins = serials.length;
        for (int j = 0; j < numCoins; j++) {

            byte[] serial = new byte[3];
            // extract serial
            serial = serials[j];

            // extract ans
            byte[][] an = ans[j];

            coins.add(new CloudCoin(serial, an, null));
        }

        return coins;
    }

    public List<CloudCoin> getCloudCoins() {
        return cloudCoins;
    }

    public ArrayList<CloudCoin> getAuthenticCoins() {
        return authenticCoins;
    }

    public int getNumUDP() {
        return numUDP;
    }

    public void coinToPng(String origin, String destination, ArrayList<byte[]> coins) {
        int byteSize = 416 * coins.size() + 32;
        byte[] coinData = new byte[byteSize];
        ByteBuffer buff = ByteBuffer.wrap(coinData);
        for (int i = 0; i < coins.size(); i++) {

            if (i == 0 && coins.get(i).length == 448) {
                buff.put(coins.get(i));
            } else {
                if (coins.get(i).length == 416) {
                    buff.put(coins.get(i));
                } else if (coins.get(i).length == 448) {
                    byte[] thiscoin = new byte[416];
                    System.arraycopy(coins.get(i), 32, thiscoin, 0, 416);
                    buff.put(thiscoin);
                }
            }
        }
        byte[] coinByteArray = buff.array();
        Log.d("RAIDA", "Writing coin data:" + bytesToHex(coinByteArray));
        PngImage.addPropChunk(origin, destination, coinByteArray);
    }

    public void coinToPng(InputStream origin, String destination, ArrayList<byte[]> coins) {
        int byteSize = 416 * coins.size() + 32;

        byte[] coinData = new byte[byteSize];
        ByteBuffer buff = ByteBuffer.wrap(coinData);
        for (int i = 0; i < coins.size(); i++) {
            if (coins.get(i).length == 448) {
                if (i == 0) {
                    buff.put(coins.get(i));
                } else {
                    if (coins.get(i).length == 416) {
                        buff.put(coins.get(i));
                    } else if (coins.get(i).length == 448) {
                        byte[] thiscoin = new byte[416];
                        System.arraycopy(coins.get(i), 32, thiscoin, 0, 416);
                        buff.put(thiscoin);
                    }
                }
            }
        }
        byte[] coinByteArray = buff.array();
        Log.d("RAIDA", "Writing coin data:" + bytesToHex(coinByteArray));
        PngImage.addPropChunk(origin, destination, coinByteArray);
    }

}
