package com.example.edgenode;

import com.example.edgenode.netty.EchoServer;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaClient
@EnableScheduling
public class EdgenodeApplication implements CommandLineRunner{

    @Value("${netty.port}")
    private int port;

    @Value("${netty.url}")
    private String url;


	@Autowired
	private EchoServer echoServer;

	public static void main(String[] args) {
		SpringApplication.run(EdgenodeApplication.class, args);
	}


    @Override
    public void run(String... args) throws Exception {
        ChannelFuture future = echoServer.start(url,port);
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                echoServer.destroy();
            }
        });
        //服务端管道关闭的监听器并同步阻塞,直到channel关闭,线程才会往下执行,结束进程
        future.channel().closeFuture().syncUninterruptibly();
    }

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
	    RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
	    redisTemplate.setConnectionFactory(redisConnectionFactory);
	    redisTemplate.setKeySerializer(new StringRedisSerializer());
	    redisTemplate.setValueSerializer(new StringRedisSerializer());
	    redisTemplate.afterPropertiesSet();
	    return redisTemplate;
    }
}
