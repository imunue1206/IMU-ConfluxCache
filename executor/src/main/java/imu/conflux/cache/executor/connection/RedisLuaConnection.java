package imu.conflux.cache.executor.connection;

import imu.conflux.cache.executor.lua.connectionSupport.RESPDecoder;
import imu.conflux.cache.executor.lua.connectionSupport.RESPEncoder;
import imu.conflux.cache.executor.lua.connectionSupport.ResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RedisLuaConnection {

    private final Channel channel;
    private final EventLoopGroup group;
    private ResponseHandler responseHandler;

    public RedisLuaConnection(String host, int port) throws Exception {
        this.responseHandler = new ResponseHandler();
        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new RESPEncoder(),
                                new RESPDecoder(),
                                responseHandler
                        );
                    }
                });

        this.channel = bootstrap.connect(host, port).sync().channel();
    }

    public Channel getChannel() {
        return channel;
    }

    public ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void close() {
        channel.close();
        group.shutdownGracefully();
    }
}
