package de.tum.gossip.p2p.packets;

import com.google.common.base.Preconditions;
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
 * |               Peer Identity                   |
 * |         (SHA256 Hash Of Public Key)           |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class GossipPacketHandshakeHello implements Packet<GossipServerHandshakeListener> {
    // TODO replace all "who am i"!
    public enum Version {
        VERSION_1,
        ;

        public static final Version CURRENT = VERSION_1;
    }

    public static class UnsupportedVersionException extends RuntimeException {}

    public Version version; // 1 byte version field
    // 3 bytes reserved (expected to be zero)

    // TODO document: proof of availability of the hostkey!
    public byte[] serverChallenge; // 8 bytes

    // public PeerIdentity identity; // 32 bytes

    public GossipPacketHandshakeHello() {}

    public GossipPacketHandshakeHello(byte[] serverChallenge) {
        Preconditions.checkState(serverChallenge.length == 8);
        this.version = Version.CURRENT;
        this.serverChallenge = serverChallenge;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeByte(version.ordinal() + 1);
        byteBuf.writeBytes(new byte[] {0, 0, 0}); // 3 bytes reserved space.
        byteBuf.writeBytes(serverChallenge);
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        byte version = byteBuf.readByte();
        if (version <= 0 || version > Version.values().length) {
            throw new UnsupportedVersionException(); // TODO catch exception and encode as error!
        }

        this.version = Version.values()[version - 1];

        byteBuf.readBytes(new byte[3]);

        byte[] bytes = new byte[8];
        byteBuf.readBytes(bytes);
        serverChallenge = bytes;
    }

    @Override
    public void accept(GossipServerHandshakeListener handler) {
        handler.handle(this);
    }
}