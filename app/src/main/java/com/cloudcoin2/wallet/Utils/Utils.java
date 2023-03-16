package com.cloudcoin2.wallet.Utils;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

    public static String toHexString( byte[] bytes )
    {
        StringBuffer sb = new StringBuffer( bytes.length*2 );
        for( int i = 0; i < bytes.length; i++ )
        {
            sb.append( toHex(bytes[i] >> 4) );
            sb.append( toHex(bytes[i]) );
        }

        return sb.toString();
    }

    private static char toHex(int nibble)
    {
        final char[] hexDigit =
                {
                        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
                };
        return hexDigit[nibble & 0xF];
    }

    public static byte[] generateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        String enc = String.format("%08X", crc.getValue());

        return longToBytes(crc.getValue());
    }

    public static byte[] generateMD5Hash(String input) {
        try {
            // Create MessageDigest instance for MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            // Add input bytes to digest
            md.update(input.getBytes());
            // Get the hash's bytes (16 bytes long)
            byte[] hashBytes = md.digest();
            // Return the first 16 bytes of the hash
            return Arrays.copyOfRange(hashBytes, 0, 16);
        } catch (Exception e) {
            throw new RuntimeException("Error generating MD5 hash", e);
        }
    }

}
