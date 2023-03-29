package com.cloudcoin2.wallet.Utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.*;

import android.util.Log;

import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.Model.UDPCall;
import com.cloudcoin2.wallet.deposit.DepositFragment;

public class RAIDAX {

    public static final int NUM_SERVERS = 25;
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
    private ArrayList<PeekResult> peekResults = new ArrayList<>();
    public static ArrayList<Denominations> denominations = new ArrayList<>();
    public static ArrayList<CloudCoin> peekCloudCoins = new ArrayList<>();
    public char[][] peekResultCodes = new char[RAIDAX.NUM_SERVERS][];
    public static boolean peekAllPassed = false;

    public static String TAG = "RAIDAX";

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

    public void RAIDAX() {
        RAIDAX.denominations.clear();
        RAIDAX.denominations.add(new Denominations(-8, .00000001));
        RAIDAX.denominations.add(new Denominations(-7, .0000001));
        RAIDAX.denominations.add(new Denominations(-6, .000001));
        RAIDAX.denominations.add(new Denominations(-5, .00001));
        RAIDAX.denominations.add(new Denominations(-4, .0001));
        RAIDAX.denominations.add(new Denominations(-3, .001));
        RAIDAX.denominations.add(new Denominations(-2, .01));
        RAIDAX.denominations.add(new Denominations(-1, .1));
        RAIDAX.denominations.add(new Denominations(0, 1));
        RAIDAX.denominations.add(new Denominations(1, 10));
        RAIDAX.denominations.add(new Denominations(2, 100));
        RAIDAX.denominations.add(new Denominations(3, 1000));
        RAIDAX.denominations.add(new Denominations(4, 10000));
        RAIDAX.denominations.add(new Denominations(5, 100000));
        RAIDAX.denominations.add(new Denominations(6, 1000000));
        RAIDAX.denominations.add(new Denominations(7, 10000000));
    }

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

    public void resetPeekResultCodes() {
        Arrays.fill(peekResultCodes, new char[0]);
    }

    public void initiatePeekResultCodes(int length, char val) {
        char[] defaultChars = new char[length];
        Arrays.fill(defaultChars, val);
        Arrays.fill(peekResultCodes, defaultChars);
    }

    public void fillPeekRow(int index, int length, char val) {
        char[] defaultChars = new char[length];
        Arrays.fill(defaultChars, val);
        peekResultCodes[index] = defaultChars;
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

    public static byte[] generateMD5Hash(String input) {
        try {
            // Create MessageDigest instance for MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            // Add input bytes to digest
            md.update(input.getBytes());
            // Get the hash's bytes (16 bytes long)
            byte[] hashBytes = md.digest();
            // Return the first 16 bytes of the hash
            return Arrays.copyOfRange(hashBytes, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating MD5 hash", e);
        }
    }

    public void loadServers() {
        try {
            Log.d("Locker", "Starting Locker Import task");
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
        } catch (Exception e) {

        }

    }

    public void removeFromLocker() throws Exception{
        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] request = Protocol.GenerateRequest(i, CommandCodes.RemoveLocker, "", CommandGroups.Locker);
            udpCalls.add(new UDPCall(request, i, CommandCodes.RemoveLocker));
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

        ArrayList<RaidaResponse> results = new ArrayList<>();
        for (Future<RaidaResponse> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                // Handle any exceptions thrown during the execution of the task
            }
        }
        int i =0;

        initiatePeekResultCodes(peekCloudCoins.size(), 'x');
        i = 0;
        // Populate Pown results
        for (RaidaResponse result : results) {
            String resultCode = Utils.bytesToHex(result.getResponse()).substring(4, 6);
            if (resultCode.equals("F1")) {
                fillPeekRow(i, peekCloudCoins.size(), 'p');
                Log.d(RAIDAX.TAG,"Remove All Passed");
            } else if (resultCode.equals("25")) {
                fillPeekRow(i, peekCloudCoins.size(), 'f');
                Log.d(RAIDAX.TAG,"Remove All failed" + i);
            } else {

            }
            i++;
        }

        // Calculate Pown result
        i = 0;
        for (CloudCoin cc:
                RAIDAX.peekCloudCoins) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < RAIDAX.NUM_SERVERS; j++) {
                sb.append(peekResultCodes[j][i]);
            }
            cc.setPownString(sb.toString());
            i++;
            Log.d(RAIDAX.TAG, "Pown String for " + i + ":" + sb);
        }

        for (CloudCoin cc : RAIDAX.peekCloudCoins) {
            if(cc.getTargetFolder().equals("Bank")) {
                CloudCoinFileWriter.WriteCoinToFile(cc, 9, DepositFragment.bankDirPath);
                Log.d(RAIDAX.TAG, "Wrote to " + DepositFragment.bankDirPath);
            }
            if(cc.getTargetFolder().equals("Counterfeit")) {
                CloudCoinFileWriter.WriteCoinToFile(cc, 9, DepositFragment.counterfeitPath);
                Log.d(RAIDAX.TAG, "Wrote to " + DepositFragment.counterfeitPath);
            }

        }

        long count = RAIDAX.peekCloudCoins.stream().filter(obj -> obj.getTargetFolder() == "Bank").count();
        long ccount = RAIDAX.peekCloudCoins.stream().filter(obj -> obj.getTargetFolder() == "Counterfeit").count();

        Log.d(RAIDAX.TAG,"Bank Coins" + count);
        Log.d(RAIDAX.TAG,"Counterfeits Coins" + ccount);

        udpCalls.clear();
        connectionPool.releaseAllConnections();

    }

    public static byte[] generateRandomAN(int length) {
        SecureRandom random = new SecureRandom();
        byte[] random_AN = new byte[length];
        random.nextBytes(random_AN);
        return random_AN;
    }

    public void importLockerCode(String code) throws Exception {
        loadServers();

        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] request = Protocol.GenerateRequest(i, CommandCodes.Peek, code, CommandGroups.Locker);
            udpCalls.add(new UDPCall(request, i, CommandCodes.Peek));
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

        ArrayList<RaidaResponse> results = new ArrayList<>();
        for (Future<RaidaResponse> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                // Handle any exceptions thrown during the execution of the task
            }
        }
        int successCount = processPeekResults(results);
        raidaResponses.clear();
        raidaResponses = results;

        if (successCount == 25) {
            Log.d("RAIDAX", "Starting Pown");
        }

        udpCalls.clear();
        connectionPool.releaseAllConnections();

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
                    echo();
                } catch (Exception e) {
                }
            }
        } finally {
            synchronized (lock) {
                isExecuting = false;
            }
        }
    }

    public void echo() throws Exception {
        loadServers();

        for (int i = 0; i < raidaLists.size(); i++) {
            byte[] request = Protocol.GenerateRequest(i, CommandCodes.Echo, "", CommandGroups.Status);
            Log.d("RAIDAX", Utils.bytesToHex(request));
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

    public void processEchoResults(List<RaidaResponse> results) {
        EchoResult echoResult = new EchoResult(0, 0);
        echoResult.compute(results);
        System.out.println("Got Total Results:" + echoResult.getResultCount());
        System.out.println("Got Pass Results:" + echoResult.getPassCount());
        System.out.println("Got Fail Results:" + echoResult.getFailCount());
        this.echoResult = echoResult;
    }

    public static byte[] extractPeekData(byte[] input) {
        if (input.length < 34) {
            throw new IllegalArgumentException("Input array must be at least 34 bytes long.");
        }
        byte[] output = Arrays.copyOfRange(input, 32, input.length - 2);
        return output;
    }

    public int processPeekResults(List<RaidaResponse> results) {
        int passCount = 0;
        int i = 0;
        peekAllPassed = false;
        peekResults.clear();

        ArrayList<Coin> pcoins = new ArrayList<>();
        resetPeekResultCodes();
        for (RaidaResponse result : results) {
            String resultCode = Utils.bytesToHex(result.getResponse()).substring(4, 6);
            PeekResult peekResult = new PeekResult();
            peekResult.setData(extractPeekData(result.getResponse()));
            peekResults.add(peekResult);
            for (Coin coin : peekResult.coins) {
                //Log.d("RAIDAX", "DN:" + coin.getDenomination() + ", SN:" + coin.getSN());
                if (!pcoins.contains(coin))
                    pcoins.add(coin);

            }
            if (resultCode.equals("F1"))
                passCount++;
            if (passCount == 25)
                peekAllPassed = true;
            //Log.d("RAIDAX", Utils.bytesToHex(result.getResponse()));
            //Log.d("RAIDAX", "Code:" + resultCode);
            i++;
        }

        RAIDAX.peekCloudCoins.clear();
        for (Coin pcoin : pcoins) {
            RAIDAX.peekCloudCoins.add(new CloudCoin(pcoin));
        }

        initiatePeekResultCodes(pcoins.size(), 'x');
        i = 0;
        // Populate Pown results
        for (RaidaResponse result : results) {
            String resultCode = Utils.bytesToHex(result.getResponse()).substring(4, 6);
            if (resultCode.equals("F1")) {
                fillPeekRow(i, pcoins.size(), 'p');
            } else if (resultCode.equals("F2")) {
                fillPeekRow(i, pcoins.size(), 'f');
            } else {

            }
            i++;
        }
        // Calculate Pown result
        i = 0;
        for (CloudCoin cc:
             RAIDAX.peekCloudCoins) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < RAIDAX.NUM_SERVERS; j++) {
                sb.append(peekResultCodes[j][i]);
            }
            cc.setPownString(sb.toString());
            i++;
            Log.d(RAIDAX.TAG, "Pown String for " + i + ":" + sb);
        }

        Log.d("RAIDAX", "Pass Count:" + passCount + "Coin List size:" + pcoins.size());
        return passCount;
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
                int bufferSize = 8192; // Use a large enough buffer to accommodate the largest expected response
                DatagramPacket rdp = new DatagramPacket(new byte[bufferSize], bufferSize);

                ds.receive(rdp);
                int responseSize = rdp.getLength(); // Determine the actual size of the response

                byte[] realData = Arrays.copyOf(rdp.getData(), responseSize); // Create a new array with the correct
                                                                              // size
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

    public void setCallbacks(UDPCallBackInterface callbacks) {
        udpCallbacks = callbacks;
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
