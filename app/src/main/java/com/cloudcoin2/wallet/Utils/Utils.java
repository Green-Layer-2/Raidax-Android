package com.cloudcoin2.wallet.Utils;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }


    public static byte[] generateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        String enc = String.format("%08X", crc.getValue());

        return longToBytes(crc.getValue());
    }


}
