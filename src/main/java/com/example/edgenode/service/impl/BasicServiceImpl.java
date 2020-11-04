package com.example.edgenode.service.impl;

import com.example.edgenode.service.BasicService;
import com.example.edgenode.service.MetaService;
import com.example.edgenode.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实现基础服务
 */
@Service
public class BasicServiceImpl implements BasicService{
    @Autowired
    MetaService metaService;

    private Long lastTime = 0L;
    private Long currentTime = 0L;
    private Long lastSent = 0L;
    private Long currentSend = 0L;


    @Value("${eureka.instance.hostname}")
    private String hostname;

    @Value("${server.port}")
    private String port;

    /**
     * 获取实时空闲率以及实时网速
     * @return
     */
    @Override
    public Map<String, List<String>> getReealTimeFreeRatio() throws Exception {
        List<InetSocketAddress> liveNodes = metaService.getAliveNodes();
        RestTemplate restTemplate = new RestTemplate();
        Map<String,List<String>> res = new HashMap<>();
        String hostAndPort = hostname + ":" + port;
        for(InetSocketAddress nodes:liveNodes){
            long sendTime = System.currentTimeMillis();
            RequestEntity requestEntity = RequestEntity.get(
                    new URI("http://"+nodes.getHostName()+"/freeRatio"))
                    .build();
            ResponseEntity<Double> freeRatio = restTemplate.exchange(requestEntity,Double.class);
            long recieveTime = System.currentTimeMillis();
            List<String> args = new ArrayList<>();
            args.add(String.valueOf(freeRatio.getBody()));
            long consumingTime = recieveTime-sendTime;
            System.out.println("====响应时间："+consumingTime);
            int size = (int) (freeRatio.getHeaders().getContentLength()+freeRatio.getHeaders().toString().length()+16);
            args.add(String.valueOf(((size*1.0)/1024/1024)/(consumingTime*1.0/1000)));
            res.putIfAbsent(nodes.getHostName(),args);
        }
        // 加上自己的空闲利用率
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(getFreeRatio()));
        res.putIfAbsent(hostAndPort,args);
        return res;
    }

    /**
     * 获取本机的空闲率
     * @return
     * @throws Exception
     */
    @Override
    public double getFreeRatio() throws Exception {
        return Utils.getAvailRation();
    }

    /**
     * 获取实时的发送字节数
     * @return
     */
    //@Scheduled(cron = "0/5 * * * * ?")
    @Override
    public List<Long> getBytesSent() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        NetworkIF[] networkIFS = hal.getNetworkIFs();
        List<Long> list = new ArrayList<>();
        for(NetworkIF net : networkIFS){
            if(net.getName().equals("awdl0")){
                continue;
            }
            // 第一次
            if(lastTime == 0L){
                lastTime = net.getTimeStamp();
                lastSent = net.getBytesSent();
                currentTime = net.getTimeStamp();
                currentSend = net.getBytesSent();
            }else{
                // 不是第一次
                currentTime = net.getTimeStamp();
                currentSend = net.getBytesSent();
                long timeDiff = currentTime - lastTime;
                long bytesDiff = currentSend - lastSent;
                double speed = bytesDiff/timeDiff;
                lastTime = currentTime;
                lastSent = currentSend;
                System.out.println(speed);

            }
            list.add(net.getTimeStamp());
            list.add(net.getBytesSent());
        }
        return list;
    }

}
