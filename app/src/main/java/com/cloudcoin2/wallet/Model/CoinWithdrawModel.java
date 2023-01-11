package com.cloudcoin2.wallet.Model;

/**
 * Created by SIRSHA BANERJEE on 16/12/20
 */
public class CoinWithdrawModel {

    public int denomination;
    public int totalValue;
    public int count;

    public CoinWithdrawModel(int denomination, int totalValue, int count) {
        this.denomination = denomination;
        this.totalValue = totalValue;
        this.count = count;
    }


}
