package com.cloudcoin2.wallet.Utils;

public class RaidaResponse {

    byte raidaId;
    byte shardId;
    byte status;



    int commandCode;
    byte executionTime;
    byte[] frameCount;
    byte[] clientEcho;
    byte[] challengeHash;

    byte[] responseBody;

    private byte[] response;


    public byte[] getResponse() {
        return this.response;
    }

    public String getResponseHex() {
        return Utils.bytesToHex(this.response);
    }


    byte[] masterTicket;

    public RaidaResponse(byte[] response, int commandCode) {
        this.commandCode = commandCode;
        this.response = response;
        if(response.length>=12)
        {
            raidaId = response[0];
            shardId = response[1];
            status = response[2];
            executionTime = response[3];
            frameCount = new byte[2];
            System.arraycopy(response, 4, frameCount, 0, 2);
            clientEcho = new byte[2];
            System.arraycopy(response, 6, clientEcho, 0, 2);
            challengeHash = new byte[4];
            System.arraycopy(response, 8, challengeHash, 0, 4);
            byte[] statusBytes = {status};

            // first 12 bytes is common in all response.

            switch (commandCode) {
                case 0: // pown returns 4 bytes of MT and Variable bytes of MS
                    if(response.length>=16) {
                        masterTicket = new byte[4];
                        System.arraycopy(response, 12, masterTicket, 0, 4);
                    }
                    if(response.length>16) {
                        int bodySize = response.length - 16;
                        responseBody = new byte[bodySize];
                        System.arraycopy(response, 16, responseBody, 0, bodySize);
                    }
                    break;
                case 1: // detect returns variable bytes of MS but no  MT
                case 3: // fix returns variable bytes of MS but no  MT
                case 30: // for free body get body but no
                case 31:
                    if(response.length>12) {
                        int bodySize = response.length - 12;
                        responseBody = new byte[bodySize];
                        System.arraycopy(response, 12, responseBody, 0, bodySize);
                    }
                    break;
                case 11: // get ticket returns MT  but no MS
                    if(response.length>=16) {
                        masterTicket = new byte[4];
                        System.arraycopy(response, 12, masterTicket, 0, 4);
                    }
                    break;

            }

        }
    }


    public byte getRaidaId() {
        return raidaId;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }


    public byte getShardId() {
        return shardId;
    }

    public byte getStatus() {
        return status;
    }

    public byte getExecutionTime() {
        return executionTime;
    }

    public byte[] getFrameCount() {
        return frameCount;
    }

    public byte[] getClientEcho() {
        return clientEcho;
    }

    public byte[] getChallengeHash() {
        return challengeHash;
    }
    public byte[] getMasterTicket() {
        return masterTicket;
    }
    public int getCommandCode() {
        return commandCode;
    }





}

