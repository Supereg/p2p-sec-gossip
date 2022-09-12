package de.tum.gossip.p2p.packets;

import com.google.common.base.Preconditions;
import de.tum.gossip.net.packets.Packet;
import de.tum.gossip.p2p.protocol.GossipClientHandshakeListener;
import io.netty.buffer.ByteBuf;

import static de.tum.gossip.crypto.GossipCrypto.RSA_SIGNATURE_BYTES_LENGTH;

/**
 * Created by Andi on 07.07.22.
 */
public class GossipPacketHandshakeIdentityVerification1 implements Packet<GossipClientHandshakeListener> {
    /**
     * 512 bytes of signature, signing an imposed 64 bit challenge appended by the own `PeerIdentity`.
     * The remote peer proofs that he actually owns the according key for the respective identity
     * right NOW (by answering the challenge)!
     * Our verification that the TLS certificate was signed by the hostkey doesn't really prove
     * that the remote peer has the hostkey at hand right now.
     */
    public byte[] signature; // 512 bytes
    public byte[] clientChallenge; // 8 bytes

    public GossipPacketHandshakeIdentityVerification1() {}

    public GossipPacketHandshakeIdentityVerification1(byte[] signature, byte[] clientChallenge) {
        Preconditions.checkState(signature.length == RSA_SIGNATURE_BYTES_LENGTH);
        Preconditions.checkState(clientChallenge.length == 8);
        this.signature = signature;
        this.clientChallenge = clientChallenge;
    }

    @Override
    public void serialize(ByteBuf byteBuf) {
        byteBuf.writeBytes(signature);
        byteBuf.writeBytes(clientChallenge);
    }

    @Override
    public void deserialize(ByteBuf byteBuf) {
        signature = new byte[RSA_SIGNATURE_BYTES_LENGTH];
        clientChallenge = new byte[8];

        byteBuf.readBytes(signature);
        byteBuf.readBytes(clientChallenge);
    }

    @Override
    public void accept(GossipClientHandshakeListener handler) {
        // TODO remove handler.handle(this);
    }
}