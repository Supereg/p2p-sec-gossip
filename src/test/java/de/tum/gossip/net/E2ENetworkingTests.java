package de.tum.gossip.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by Andi on 27.06.22.
 */
public class E2ENetworkingTests {
    private interface HelloPacketHandler extends InboundPacketHandler {
        void handle(HelloPacket packet);
    }

    private static class HelloPacket implements OutboundPacket, InboundPacket<HelloPacketHandler> {
        public String message;

        public HelloPacket() {}

        public HelloPacket(String message) {
            this.message = message;
        }

        @Override
        public void deserialize(ByteBuf byteBuf) {
            message = ByteBufUtils.readString(byteBuf);
        }

        @Override
        public void serialize(ByteBuf byteBuf) {
            ByteBufUtils.writeString(byteBuf, message);
        }

        @Override
        public void accept(HelloPacketHandler handler) {
            handler.handle(this);
        }
    }

    private class EchoServerHandler implements HelloPacketHandler {
        private ChannelInboundHandler channel;

        @Override
        public void onConnect(ChannelInboundHandler channel) {
            onConnectCalled++;
            this.channel = channel;
        }

        @Override
        public void onDisconnect() {
            onDisconnectCalled++;
        }

        @Override
        public void handle(HelloPacket packet) {
            packetHandleCalled++;

            serverReceivedMessage = packet.message;
            channel.getHandle().writeAndFlush(packet); // TODO replace with "sendPacket"!
        }
    }

    private class EchoClientHandler implements HelloPacketHandler {
        @Override
        public void onConnect(ChannelInboundHandler channel) {
            onConnectCalled++;

            channel.getHandle().writeAndFlush(new HelloPacket("Hello World!"))
                    .syncUninterruptibly();
        }

        @Override
        public void onDisconnect() {
            onDisconnectCalled++;
        }

        @Override
        public void handle(HelloPacket packet) {
            packetHandleCalled++;
            clientReceivedMessage = packet.message;

            packetReceivePromise.setSuccess(null);
        }
    }

    private Integer onConnectCalled = 0;
    private Integer onDisconnectCalled = 0;
    private Integer packetHandleCalled = 0;
    private String clientReceivedMessage;
    private String serverReceivedMessage;

    private Promise<Void> packetReceivePromise;

    @Test
    public void e2eEchoClientServerTest() throws InterruptedException {
        var protocol = new ProtocolDescription()
                .registerInboundAndOutbound(1, HelloPacket::new);

        var eventLoop = new NioEventLoopGroup(2);
        var server = protocol.makeServer(2446, eventLoop, EchoServerHandler::new);
        var client = protocol.makeClient("localhost", 2446, eventLoop, EchoClientHandler::new);

        // create a promise the client can call when it gets its packet back!
        packetReceivePromise = eventLoop.next().newPromise();

        // both operations are blocking
        server.run();
        client.connect();

        // onConnect of the client will write the HelloPacket!

        packetReceivePromise.sync();

        client
                .disconnect()
                .sync();

        server
                .stop()
                .sync();

        Thread.sleep(200);

        assertEquals(2, onConnectCalled);
        assertEquals(2, onDisconnectCalled);
        assertEquals(2, packetHandleCalled);
        assertEquals(clientReceivedMessage, "Hello World!");
        assertEquals(serverReceivedMessage, "Hello World!");
    }
}