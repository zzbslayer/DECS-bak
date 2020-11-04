package com.example.edgenode.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * 客户端处理类
 */
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private ChannelHandlerContext ctx;

    /**
     * 在到服务器的连接已经建立之后将被调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.copiedBuffer("成功连接Netty服务器!\n", CharsetUtil.UTF_8));
        this.ctx = ctx;
    }

    /**
     * 当从服务器接收到一个消息时被调用
     * @param channelHandlerContext
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("Client received: "+ byteBuf.toString(CharsetUtil.UTF_8));
    }

    /**
     * 在处理过程中引发异常时被调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 写信息
     * @param msg
     */
    public void writeMsg(String msg){
        ctx.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }
}

