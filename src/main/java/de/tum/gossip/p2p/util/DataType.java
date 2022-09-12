package de.tum.gossip.p2p.util;

/**
 * A data type used in the gossip protocol to identity the transported data.
 * <p>
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
