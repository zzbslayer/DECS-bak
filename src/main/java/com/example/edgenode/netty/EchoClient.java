package com.example.edgenode.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class EchoClient {

    private final String host;
    private final int port;
    private EchoClientHandler handler = new EchoClientHandler();


    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    /**
     * 运行流程：
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new EchoClient("10.0.0.229",9092).start();
    }

    public void start() throws Exception {

        /**
         * Netty用于接收客户端请求的线程池职责如下。
         * （1）接收客户端TCP连接，初始化Channel参数；
         * （2）将链路状态变更事件通知给ChannelPipeline
         */
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host,port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(handler);
                        }
                    });
            //绑定端口
            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            group.shutdownGracefully().sync();
        }
    }

    public EchoClientHandler getHandler() {
        return handler;
    }
}