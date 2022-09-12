package de.tum.gossip.p2p.packets;

import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipServerHandshakeListener;
import io.netty.buffer.ByteBuf;

/**
 * Created by Andi on 06.07.22. TODO remove and document!
 *
 * <h3>Packet Format</h3>
 *
 *                                 1  1  1  1  1  1
 *   0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |  VERSION  |            Reserved               |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class GossipPacketHandshakeHello implements Packet<GossipServerHandshakeListener> {
    public enum Version {
        VERSION_1,
        ;

        public static final Version CURRENT = VERSION_1;
    }

    public static class UnsupportedVersionException extends RuntimeException {}

    public Version version; // 1 byte version field
    // 3 bytes reserved (expected to be zero)

    public GossipPacketHandshakeHello() {
        this.version = Version.CURRENT;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeByte(version.ordinal() + 1);
        byteBuf.writeBytes(new byte[3]); // 3 bytes reserved space.
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        byte version = byteBuf.readByte();
        if (version <= 0 || version > Version.values().length) {
            throw new UnsupportedVersionException();
        }

        this.version = Version.values()[version - 1];

        byteBuf.readBytes(new byte[3]);
    }

    @Override
    public void accept(GossipServerHandshakeListener handler) {
        handler.handle(this);
    }
}