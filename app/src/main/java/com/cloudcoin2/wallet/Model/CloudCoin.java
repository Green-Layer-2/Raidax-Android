package com.cloudcoin2.wallet.Model;

import static com.cloudcoin2.wallet.Utils.RAIDA.bytesToHex;

import java.nio.ByteBuffer;
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



    private int coinType=0; // 0 = bank 1=fracked  2 = counterfeit 3 = limbo




    private String path;
    private List<Integer> frackedServers= new ArrayList();
    List<List<CloudCoin>> frackedCoins = new ArrayList<>();


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
