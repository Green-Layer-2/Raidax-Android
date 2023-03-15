package com.cloudcoin2.wallet.Utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class Protocol {

    public static byte[] generateChallenge() {
        byte[] challenge = generateRandom(12);
        byte[] checksumTotal = Utils.generateCRC32(challenge);
        byte[] checksum = new byte[4];
        System.arraycopy(checksumTotal, checksumTotal.length - 4, checksum, 0, 4);
        byte[] mData = new byte[challenge.length + checksum.length + 2];
        mData[mData.length - 2] = 0x3e;
        mData[mData.length - 1] = 0x3e;
        ByteBuffer buff = ByteBuffer.wrap(mData);
        buff.put(challenge);
        buff.put(checksum);
        byte[] challengeData = buff.array();
        return challengeData;
    }

    public static byte[] generateRandom(int length) {
        return generateRandom(length, null, false);
    }

    public static byte[] generateRandom(int length, String seed, boolean debug) {
        String AB = "0123456789ABCDEF";
        // if(seed!=null)
        // Log.d("Seed:",seed);

        if (debug) {
            if (seed == null || seed.equals("41414141414141414141414141414141")) {
                AB = "BBBBBBBBBBBBBBBB";
            } else {
                AB = "AAAAAAAAAAAAAAAA";
            }
        }
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));

        String pan = sb.toString();
        byte[] bytes = pan.getBytes(StandardCharsets.UTF_8);
        byte[] returnBytes = new byte[length];
        System.arraycopy(bytes, 0, returnBytes, 0, length);
        // Log.d("RandomX:",bytesToHex(returnBytes));
        return returnBytes;
    }


    public static byte[] GenerateRequest(int raidaID, int commandCode, String code, int commandGroup ) {
        if(commandCode == CommandCodes.Echo) {
            byte[] body = generateChallenge();
            byte[] header = generateXHeader(raidaID, commandCode, body.length, commandGroup);

            byte[] request = new byte[header.length + body.length];
            System.arraycopy(header, 0, request, 0, header.length);
            System.arraycopy(body, 0, request, 32, body.length);

            return request;
        }
        if(commandCode == CommandCodes.Peek) {

            byte[] challenge = generateChallenge();
            String an = raidaID + code;
            byte[] md5Bytes = Utils.generateMD5Hash(an);

            byte[] body = new byte[34];
            byte[] header = generateXHeader(raidaID, commandCode, body.length, commandGroup);

            System.arraycopy(challenge, 0, body,0, 16);
            System.arraycopy(md5Bytes, 0, body,16, 16);

            body[body.length -1] = 0x3e;
            body[body.length -2] = 0x3e;

            byte[] request = new byte[header.length + body.length];
            System.arraycopy(header, 0, request, 0, header.length);
            System.arraycopy(body, 0, request, 32, body.length);

            return request;

        }
        return null;
    }

    public static byte[] generateXHeader(int raidaID, int commandCode, int length, int commandGroup) {

        byte[] header = new byte[32];
        // Fill in the request data with the echo command

        header[0] = 0x01; // VR - Version of Routing header
        header[1] = 0x00; // SP - Split ID (not used in this example)
        header[2] = (byte) raidaID; // DA - Data Agent Index (not used in this example)
        header[3] = 0x00; // SH - Shard ID (not used in this example)
        header[4] = (byte)commandGroup; // CG - Command Group (Authentication)
        header[5] = (byte)commandCode; // CM - Command (Echo)
        header[6] = 0x00; // ID - Cloud/Coin ID 0 (not used in this example)
        header[7] = (byte) 4; // ID - Cloud/Coin ID 1 (not used in this example)

        // Fill in the Presentation group
        header[8] = 0x01; // VR - Version of PLS
        header[9] = 0x00; // AP - Application 0
        header[10] = 0x00; // AP - Application 1
        header[11] = 0x00; // CP - Compression (none)
        header[12] = 0x00; // TR - Translation (none)
        header[13] = 0x00; // AI - AI Translation (not used in this example)
        header[14] = 0x00; // RE - Reserved (not used in this example)
        header[15] = 0x00; // RE - Reserved (not used in this example)

        // Fill in the Encryption group
        header[16] = 0x00; // EN - Encryption Type (none)
        header[17] = 0x00; // DE - Denomination (not used in this example)
        header[18] = 0x00; // SN - Encryption SN 0 (not used in this example)
        header[19] = 0x00; // SN - Encryption SN 1 (not used in this example)
        header[20] = 0x00; // SN - Encryption SN 2 (not used in this example)
        header[21] = 0X00; // SN - Encryption SN 3 (not used in this example)
        header[22] = (byte) 0; // BL - Body Length (not used in this example)
        header[23] = (byte) length; // BL - Body Length (not used in this example)

        // Fill in the Nonce group
        header[24] = 0x00; // NO - Nonce 0 (not used in this example)
        header[25] = 0x00; // NO - Nonce 1 (not used in this example)
        header[26] = 0x00; // NO - Nonce 2 (not used in this example)
        header[27] = 0x00; // NO - Nonce 3 (not used in this example)
        header[28] = 0x00; // NO - Nonce 4 (not used in this example)
        header[29] = 0x00; // NO - Nonce 5 (not used in this example)
        header[30] = 0x00; // NO - Nonce 6 /
        header[31] = 0x00; // NO - Nonce 6 /

        return header;
    }


    public static byte[] generateHeader(int raidaID, int type, int udpnum, byte udpChecksum, boolean encryption, byte[] nonce,
                                        byte[] mSerialNo) {
        byte[] udpHeader = new byte[32];
        byte[] serialNo = { (byte) 0, (byte) 0, (byte) 0 };
        if (nonce == null) {
            nonce = new byte[] { (byte) 0, (byte) 0, (byte) 0 };
        }
        byte mEnc = (byte) 0;

        if (udpnum < 1)
            udpnum = 1;

        byte[] size = new byte[2];
        size[0] = (byte) ((udpnum >> 8) & 0xFF);
        size[1] = (byte) (udpnum & 0xFF);

        byte coinType = 1;
        if (type == 30 || type == 31)
            coinType = 0;

        //Log.d("HEADER", "UDP NUM:" + udpnum);
        if (encryption) {
            if (!((nonce.equals("null")) || (nonce.length == 0))) {

                if (mSerialNo.length == 3 && nonce.length == 3) {
                    mEnc = (byte) 1;
                    serialNo = mSerialNo;
                }

            }
        }
        udpHeader[0] = 1;
        udpHeader[1] = 0;
        udpHeader[2] = (byte) raidaID;
        udpHeader[3] = 0;
        udpHeader[4] = 0;
        udpHeader[5] = (byte) type;
        udpHeader[6] = udpChecksum;
        udpHeader[7] = 4;
        udpHeader[8] = 1;
        udpHeader[9] = 0;
        udpHeader[10] = 0;
        udpHeader[11] = 0;
        udpHeader[12] = 0;
        udpHeader[13] = 0;
        udpHeader[14] = 0;
        udpHeader[15] = 0;
        udpHeader[16] = mEnc;
        udpHeader[17] = 0;
        udpHeader[18] = 0;
        udpHeader[19] = serialNo[0];
        udpHeader[20] = serialNo[1];
        udpHeader[21] = serialNo[2];
        udpHeader[22] = 0;
        udpHeader[23] = 0;
        udpHeader[24] = nonce[0];
        udpHeader[25] = nonce[1];
        udpHeader[26] = nonce[2];
        udpHeader[27] = 0;
        udpHeader[28] = 0;
        udpHeader[29] = 0;
        udpHeader[30] = 0;
        udpHeader[31] = 0;

        //Log.d("HEADER", bytesToHex(udpHeader));

        return udpHeader;
    }

    public static byte[] generateHeader(int raidaID, int type) {
        return generateHeader(raidaID, type, 1, (byte) 0, false, null, null);
    }

    public static byte[] generateHeader(int raidaID, int type, int udpnum) {
        return generateHeader(raidaID, type, udpnum, (byte) 0, false, null, null);
    }

    public static byte[] generateHeader(int raidaID, int type, int udpnum, byte udpChecksum) {
        return generateHeader(raidaID, type, udpnum, udpChecksum, false, null, null);
    }


}
