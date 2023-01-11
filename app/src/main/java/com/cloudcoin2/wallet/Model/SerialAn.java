package com.cloudcoin2.wallet.Model;

import java.util.ArrayList;
import java.util.List;

public class SerialAn {

    private List<byte[]> serials = new ArrayList<>();
    private List<byte[][]> ans = new ArrayList<>();



    public void add(byte[] serial, byte[][] ans) {
        this.serials.add(serial);
        this.ans.add(ans);

    }

    public int size() throws Exception {
        if(serials.size()!=ans.size())
        {
            throw new Exception("Serial and ans array size must be same.");
        }
        else
        {
            return serials.size();
        }
    }

    public byte[][] getSerials()
    {
        byte[][] mSerialno = new byte[serials.size()][3];

        for (int i = 0; i < serials.size(); i++) {
            System.arraycopy(serials.get(i), 0, mSerialno[i], 0, 3);

        }
        return mSerialno;

    }

    public byte[][][] getAns()
    {
        byte[][][] mAns = new byte[ans.size()][25][16];
        for (int i = 0; i < serials.size(); i++) {
            for (int j = 0; j < 25; j++) {
                System.arraycopy(ans.get(i)[j], 0, mAns[i][j], 0, 16);
            }
        }

        return mAns;

    }
}
