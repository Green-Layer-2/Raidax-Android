package com.cloudcoin2.wallet.Utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EchoResult {
    private int resultCount = 0;
    private int passCount = 0;
    private int failCount = 0;
    private ArrayList<String> responseCodes = new ArrayList<>();
    public int getResultCount() { return resultCount;}
    public int getPassCount() { return passCount;}
    public int getFailCount() { return failCount;}
    public ArrayList<String> getResponseCodes() { return responseCodes;}
    EchoResult(int resultCount, int passCount) {
        this.passCount = passCount;
        this.resultCount = resultCount;
    }

    public void compute(List<RaidaResponse> responses) {
        resultCount = 0;
        passCount = 0;
        failCount = 0;
        for (RaidaResponse raidaResponse : responses) {
            resultCount++;
            if(raidaResponse.getResponseHex()!=null) {
                //Log.d("RAIDAX", raidaResponse.getResponseHex());
                if(raidaResponse.getResponseHex().length() > 20) {
                    String responseCode = raidaResponse.getResponseHex().substring(4,6);
                    if(responseCode.equals("FA")) passCount++;
                    else failCount++;
                    responseCodes.add(responseCode);
                }
            }
        }

    }
}
