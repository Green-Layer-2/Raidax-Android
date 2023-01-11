package com.cloudcoin2.wallet.Utils;

public interface UDPCallBackInterface {

    void ReportBack(byte[] lMsg, String hex, int commandCode,int raidaID,int passcount);
    void ReportBackError(Exception e, byte[] data, int commandCode,int raidaID);

}
