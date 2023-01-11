package com.cloudcoin2.wallet.Model;

public class RaidaItems {
    private String serverAddress;
    private int ports;

    public RaidaItems(String serverAddress, int ports) {
        this.serverAddress = serverAddress;
        this.ports = ports;

    }
    public String getServerAddress() {
        return serverAddress;
    }
    public int getPorts() {
        return ports;
    }



}
