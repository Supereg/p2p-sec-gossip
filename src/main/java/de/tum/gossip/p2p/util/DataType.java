package de.tum.gossip.p2p.util;

/**
 * Created by Andi on 07.07.22.
 */
public record DataType(int dataType) {
    @Override
    public String toString() {
        return "DataType{" +
                "dataType=" + Integer.toHexString(dataType) +
                '}';
    }
}
