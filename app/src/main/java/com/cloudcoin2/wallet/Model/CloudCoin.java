package com.cloudcoin2.wallet.Model;

import static com.cloudcoin2.wallet.Utils.RAIDA.bytesToHex;

import android.util.Log;

import com.cloudcoin2.wallet.Utils.Coin;
import com.cloudcoin2.wallet.Utils.Denominations;
import com.cloudcoin2.wallet.Utils.RAIDAX;
import com.cloudcoin2.wallet.Utils.Utils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class CloudCoin {
    private byte[] serial;
    private byte[][] ans;
    private byte denomination ;
    private byte[][] pans;
    private byte[] pownStatus = {(byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1,
                                 (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1,
                                 (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1,
                                 (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1,
                                 (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1};
    ;
    private byte[] pownResponse;
    private int passCount = 0;
    private int failCount = 0;
    private int noResponseCount = 25;
    private String pownString;
    private  int countOccurrences(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
    public void setPownString(String pownString) {
        this.pownString= pownString;
        int passCount = countOccurrences(pownString,'p');
        int failCount = countOccurrences(pownString,'f');

        if(pownString.equals("ppppppppppppppppppppppppp")) {
            targetFolder = "Bank";
        } else if (pownString.equals("fffffffffffffffffffffffff")) {
            targetFolder = "Counterfeit";
        } else if (passCount > 13) {
            targetFolder = "Bank";
        } else {
            targetFolder = "Counterfeit";
        }

    }

    private String targetFolder;

    public String getTargetFolder() { return  targetFolder;}

    public void setTargetFolder(String targetFolder) { this.targetFolder = targetFolder;}

    public byte[] toByteArray(int format) {
        if(format == 9) return  toByteArrayFormat9();

        return  null;
    }


    public byte[] toByteArrayFormat9() {
        int ansLength = 0;
        for (byte[] an : ans) {
            ansLength += an.length;
        }

        byte[] byteArray = new byte[1 + 1 +1 + serial.length + ansLength];
        int currentIndex = 0;
        byteArray[currentIndex++] = 1;
        byteArray[currentIndex++] = 1;

        byteArray[currentIndex] = denomination;
        currentIndex += 1;

        System.arraycopy(serial, 0, byteArray, currentIndex, serial.length);
        currentIndex += serial.length;

        for (byte[] an : ans) {
            System.arraycopy(an, 0, byteArray, currentIndex, an.length);
            currentIndex += an.length;
        }

        return byteArray;
    }

    private int coinType=0; // 0 = bank 1=fracked  2 = counterfeit 3 = limbo




    private String path;
    private List<Integer> frackedServers= new ArrayList();
    List<List<CloudCoin>> frackedCoins = new ArrayList<>();

    public CloudCoin(int dn,int serial) {
        this.denomination = (byte) dn;
        this.serial = intToByteArray(serial);
        this.ans = new byte[25][];
        for (int i = 0; i < RAIDAX.NUM_SERVERS; i++) {
            ans[i] = generateRandomAN(16);
        }

    }


    public CloudCoin(Coin coin) {
        this.denomination = (byte) coin.getDenomination();
        this.serial = intToByteArray(coin.getSN());
        this.pownString = "ppppppppppppppppppppppppp";
        this.ans = new byte[RAIDAX.NUM_SERVERS][];
        this.pans = new byte[RAIDAX.NUM_SERVERS][];

        for (int i = 0; i < RAIDAX.NUM_SERVERS; i++) {
            ans[i] = generateRandomAN(16);
            pans[i] = ans[i];
            //Log.d(RAIDAX.TAG,"Generated AN:" + Utils.bytesToHex(ans[i]));
        }

    }

    public void generateANs() {
        for (int i = 0; i < RAIDAX.NUM_SERVERS; i++) {

        }
    }

    public  byte[] intToByteArray(int value) {
        return new byte[] {
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    public CloudCoin(byte[] serial, byte[][] ans, byte[][] pans) {

        this.serial = serial;
        if (ans != null)
            this.ans = ans;
        else this.ans = new byte[25][16];


        if (this.pans != null)
            this.pans = pans;
        else this.pans = new byte[25][16];

        this.pownResponse = new byte[12];
    }

    public String getFileName()
    {
        return "1.CloudCoin.1."+Integer.parseInt(bytesToHex(serial), 16)+".bin";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    public byte[] getPownResponse() {
        return pownResponse;
    }

    public void setPownResponse(byte[] pownResponse) {
        this.pownResponse = pownResponse;
    }

    public byte[] getSerial() {
        return serial;
    }

    public int getSerialAsInt() {
        return ((serial[0] & 0xFF) << 24) |
                ((serial[1] & 0xFF) << 16) |
                ((serial[2] & 0xFF) << 8) |
                ((serial[3] & 0xFF) << 0);
    }



    public void setSerial(byte[] serial) {
        this.serial = serial;
    }

    public byte getDenomination() { return this.denomination;}

    public void setDenomination(byte denomination) {

        this.denomination = denomination;
    }

    public byte[][] getAns() {
        return ans;
    }

    public void setAns(byte[][] ans) {
        this.ans = ans;
    }

    public byte[][] getPans() {
        return pans;
    }

    public void setPan(int raidaId,byte[] value) {
        if(value.length==16)
            System.arraycopy(value, 0, pans[raidaId], 0, 16);
    }

    public void setAn(int raidaId,byte[] value) {
        if(value.length==16)
            System.arraycopy(value, 0, ans[raidaId], 0, 16);
    }

    public void copyAnsToPans() throws Exception {
        for(int i=0;i<25;i++)
        {
            if(ans.length>i && pans.length>i && ans[i].length==16)
            {
                System.arraycopy(ans[i], 0, pans[i], 0, 16);
            }
            else
            {
                throw new Exception("unable to find valid AN for coin, coin seems to be corrupt");
            }

        }
    }

    public void setPans(byte[][] pans) {
        this.pans = pans;
    }

    public byte[] getPownStatus() {
        return pownStatus;
    }

    public byte getPownStatus(int raidaId) {
        if(raidaId>=0 && raidaId<=24)
            return pownStatus[raidaId];
        else return (byte) -1;
    }

    public String generateSingleCoinFileName(String extension) {
        // Convert denomination byte to an integer
        int btc = denomination & 0xFF;

        // Convert serial bytes to an integer (assuming the serial number is stored in big-endian order)
        int serialNumber = ByteBuffer.wrap(serial).getInt();

        // Calculate the satoshies
        int sat = (int) (Math.pow(2, denomination) * 1000);

        String denominationWhole = String.valueOf(btc);
        String satoshiesFraction = Denominations.getDenomination(denomination);

        String fileName = denominationWhole + ".BTC." + satoshiesFraction + ".SAT." + pownString + "." + serialNumber + "." + extension;
        return fileName;
    }
    public boolean  isFracked()
    {
        return this.passCount >= 16 && this.failCount >= 1;
    }

    public void setPownStatus(int raidaId, int value) {
        if (raidaId <= pownStatus.length && value >= -1 && value<=1)
        {
            pownStatus[raidaId] = (byte) value;
            recalculateCounts();
        }

    }

    public static byte[] generateRandomAN(int length) {
        SecureRandom random = new SecureRandom();
        byte[] random_AN = new byte[length];

        for (int i = 0; i < length; i++) {
            byte randomByte;
            do {
                randomByte = (byte) random.nextInt(256);
            } while (randomByte == 0);
            random_AN[i] = randomByte;
        }

        return random_AN;
    }
    private void recalculateCounts()
    {
        passCount=0;
        failCount=0;
        noResponseCount=0;
        frackedServers.clear();
        for(int raidaId=0;raidaId<pownStatus.length;raidaId++)
        {
            // pass
            if (pownStatus[raidaId] == (byte) 1) {
                 passCount++;
            }
            // fail
            if (pownStatus[raidaId] == (byte) 0) {
                failCount++;
                frackedServers.add(raidaId);
            }

        }
        noResponseCount = 25 - (passCount + failCount);
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public int getNoResponseCount() {
        return noResponseCount;
    }


    public boolean isValid() {
        if (this.serial != null && this.serial.length == 3 && this.ans != null && this.ans.length == 25) {
            for (int i = 0; i < 25; i++) {
                if (this.ans[i] == null || this.ans[i].length != 16)
                    return false;
            }
            return true;
        }

        return false;
    }

    public void setCoinType(int coinType) {
        this.coinType = coinType;
    }

    public int getCoinType() {
        return coinType;
    }


}
