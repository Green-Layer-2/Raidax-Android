package com.cloudcoin2.wallet.Model;

public class EchoStatus {

    private int raidaId;
    private String status;

    public int getRaidaId() {
        return raidaId;
    }

    public void setRaidaId(int raidaId) {
        this.raidaId = raidaId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EchoStatus(int raidaId, String status) {
        this.raidaId = raidaId;
        this.status = status;
    }
}
