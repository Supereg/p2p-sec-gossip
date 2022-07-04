package de.tum.gossip.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Created by Andi on 21.06.22.
 */
public class ChannelInboundHandler extends SimpleChannelInboundHandler<InboundPacket<? extends InboundPacketHandler>> {
    public static final AttributeKey<ProtocolDescription> PROTOCOL_DESCRIPTION_KEY = AttributeKey.valueOf("protocol-description");

    private final ProtocolDescription protocol;
    private InboundPacketHandler packetHandler;

    private Channel channel;
    private boolean disconnected = false;

    public ChannelInboundHandler(ProtocolDescription protocol, InboundPacketHandler initialHandler) {
        this.protocol = protocol;
        this.packetHandler = initialHandler;
    }

    public Channel getHandle() {
        return channel;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen() && !disconnected;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.channel = ctx.channel();

        // TODO create custom channel?

        this.packetHandler.onConnect(this);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, InboundPacket msg) {
        if (!this.isConnected()) {
            return;
        }

        //noinspection unchecked
        msg.accept(packetHandler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // TODO proper logging!
        this.close();
    }

    public <Handler extends InboundPacketHandler> void replacePacketHandler(Handler handler) {
        this.replacePacketHandler((Supplier<Handler>) () -> handler);
    }

    public <Handler extends InboundPacketHandler> void replacePacketHandler(Supplier<Handler> handler) {
        this.packetHandler = handler.get();
        if (this.isConnected()) {
            this.packetHandler.onConnect(this);
        }
    }

    public void close() {
        // TODO ability to supply close reason?
        if (this.channel.isOpen()) {
            this.channel.close().awaitUninterruptibly();
        }

        if (!this.disconnected) {
            this.disconnected = true;
            this.packetHandler.onDisconnect();
        }
    }
}