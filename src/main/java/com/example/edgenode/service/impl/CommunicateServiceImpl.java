package com.example.edgenode.service.impl;

import com.example.edgenode.netty.EchoClientHandler;
import com.example.edgenode.service.CommunicateService;
import com.example.edgenode.service.MetaService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 通信模块
 */
@Service
public class CommunicateServiceImpl implements CommunicateService{

    @Value("${application.name}")
    private String applicationName;

    @Autowired
    private MetaService metaService;

    /**
     * 广播
     * @param msg
     */
    @Override
    public void broadCast(String msg) throws InterruptedException {
        //1. 先去拿到所有的服务的列表
        List<InetSocketAddress> liveApplications = metaService.getAliveNodes();

        //2. 创建NettyClient
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        try {
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("decoder",new StringDecoder());
                            socketChannel.pipeline().addLast("encoder",new StringEncoder());
                            socketChannel.pipeline().addLast(new EchoClientHandler());
                        }
                    });
        //绑定端口

        } catch (Exception e) {
            group.shutdownGracefully().sync();
        }


        for(InetSocketAddress socketAddress:liveApplications){
            ChannelFuture f = b.connect(socketAddress.getHostName(),socketAddress.getPort()).sync();
            f.channel().writeAndFlush(msg);
        }
//        //3. 发送广播消息
//        for(EchoClient client:clients){
//            while(client.getHandler().getCtx()==null) {
//            }
//            client.getHandler().writeMsg("haha");
//        }
    }

//    public static void main(String[] args) {
//        CommunicateServiceImpl communicateService = new CommunicateServiceImpl();
//        communicateService.broadCast("haha");
//    }
}
