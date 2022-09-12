package de.tum.gossip.p2p.packets;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipServerHandshakeListener;
import io.netty.buffer.ByteBuf;

import static de.tum.gossip.crypto.GossipCrypto.RSA_SIGNATURE_BYTES_LENGTH;

/**
 * Created by Andi on 07.07.22.
 *
 *                                 1  1  1  1  1  1
 *   0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |RESERVED|PW|            Reserved               |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                                               |
 * /                  Signature                    /
 * |                                               |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * |                Server Challenge               |
 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * // TODO fully document
 */
public class GossipPacketHandshakeIdentityVerification2 implements Packet<GossipServerHandshakeListener> {
    /**
     * Refer to {@link GossipPacketHandshakeIdentityVerification1#signature}.
     */
    public byte[] signature; // 512 bytes

    public GossipPacketHandshakeIdentityVerification2() {}

    public GossipPacketHandshakeIdentityVerification2(byte[] signature) {
        Preconditions.checkState(signature.length == RSA_SIGNATURE_BYTES_LENGTH, signature.length + " vs. " + RSA_SIGNATURE_BYTES_LENGTH);
        this.signature = signature;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeBytes(signature);
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        signature = new byte[RSA_SIGNATURE_BYTES_LENGTH];
        byteBuf.readBytes(signature);
    }

    @Override
    public void accept(GossipServerHandshakeListener handler) {
        // TODO remove: handler.handle(this);
    }
}