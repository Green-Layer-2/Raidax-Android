package com.cloudcoin2.wallet.Utils;

public class Coin {
    private int dn;
    private int sn;
    public int getDenomination() { return  dn;}
    public int getSN(){ return  sn;}
    public void setData(byte[] data) {
        if (data.length != 5) {
            throw new IllegalArgumentException("Data array must be exactly 5 bytes long.");
        }
        dn = data[0];
        sn = ((data[1] & 0xFF) << 24) |
                ((data[2] & 0xFF) << 16) |
                ((data[3] & 0xFF) << 8) |
                ((data[4] & 0xFF) << 0);
    }
}
