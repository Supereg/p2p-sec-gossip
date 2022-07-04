package de.tum.gossip.net;

/**
 * Created by Andi on 21.06.22.
 */
public interface Packet {}
/*
public abstract class Packet<Handler extends InboundPacketHandler> {
    public Packet() {}

    public abstract void deserialize(ByteBuf byteBuf);

    public abstract void serialize(ByteBuf byteBuf);

    public abstract void accept(Handler handler);
}
*/

