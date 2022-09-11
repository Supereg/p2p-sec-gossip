package de.tum.gossip.net.util;

/**
 * Created by Andi on 11.09.22.
 */
public class Hex {
    // from https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for(byte b: bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}