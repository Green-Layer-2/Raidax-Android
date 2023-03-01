package com.cloudcoin2.wallet.Model;

public class UDPCall {
    private byte[] data;
    private int index;
    private int commandCode;

    public UDPCall(byte[] data, int index, int commandCode) {
        this.data = data;
        this.index = index;
        this.commandCode = commandCode;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(int commandCode) {
        this.commandCode = commandCode;
    }


}
