package com.cloudcoin2.wallet.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class PeekResult {
    public byte[] data;
    public ArrayList<Coin> coins;

    public byte[] getData() { return  data;}

    public void setData(byte[] data) {
        if (data.length % 5 != 0) {
            throw new IllegalArgumentException("Data array must be a multiple of 5 bytes long.");
        }
        this.data = data;
        coins = new ArrayList<Coin>();
        for (int i = 0; i < data.length; i += 5) {
            byte[] coinData = Arrays.copyOfRange(data, i, i + 5);
            Coin coin = new Coin();
            coin.setData(coinData);
            coins.add(coin);
        }
    }
}
