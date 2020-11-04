package com.example.edgenode.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.edgenode.service.MetaService;
import com.example.edgenode.service.ReadService;
import com.example.edgenode.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * 数据读模块
 */
@Service
public class ReadServiceImpl implements ReadService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MetaService metaService;

    @Value("${eureka.instance.hostname}")
    private String hostname;

    @Value("${server.port}")
    private String port;

    public ResponseEntity<InputStreamResource> read(String fileName) throws IOException, URISyntaxException {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String uri = request.getRequestURI();
        String hostAndPort = hostname + ":" + port;


        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        String fileNameStoreKey = fileName + "_store";
        if (redisTemplate.opsForZSet().score(fileNameStoreKey, hostAndPort) == null) {

            // 转发给有资源的机器
            ResponseEntity<Resource> res = forwordToOthers(uri, fileNameStoreKey);
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(res.getBody().contentLength())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(new InputStreamResource(res.getBody().getInputStream()));
        }


        if(!handleForword(request, fileName)){
            // 如果不是来自于其他机器的转发，则属于本地的访问记录，也要记录到redis中
            recordInRedis(fileName,hostAndPort);
        }



        // 读取本机资源
        String filePath = "/Users/dawnchau/" + fileName;
        FileSystemResource file = new FileSystemResource(filePath);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(file.contentLength())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new InputStreamResource(file.getInputStream()));
    }

    /**
     * 如果本机上没有，则转发给其他节点
     * @param uri 资源
     * @param fileNameStoreKey redis中的key
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private ResponseEntity<Resource> forwordToOthers(String uri, String fileNameStoreKey) throws IOException, URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();
        // TODO：默认转发给第一个
        Set<String> hostNameWithFile = redisTemplate.opsForZSet().range(fileNameStoreKey, 0, 0);
        List<String> hosts = new ArrayList<>(hostNameWithFile);
        String hostAndPortWithFile = hosts.get(0);
        RequestEntity requestEntity = RequestEntity.get(
                new URI("http://" + hostAndPortWithFile + uri))
                .header("port",String.valueOf(port))
                .build();
        ResponseEntity<Resource> res = restTemplate.exchange(requestEntity, Resource.class);
        return res;
    }


    /**
     * 判断是从哪个机器上转发过来的，并且将访问记录计入到redis中
     *
     * @param request       ： 转发过来的请求request
     * @param fileName 访问的文件名
     */
    private boolean handleForword(HttpServletRequest request, String fileName) {
        // 不是来自转发
        if(request.getHeader("port")==null){
            return false;
        }
        String ip = Utils.getIpAddr(request);
        int remotePort = Integer.parseInt(request.getHeader("port"));
        String remoteHostAndPort = ip+":"+remotePort;
        List<InetSocketAddress> liveNodes = metaService.getAliveNodes();
        // 遍历所有存活节点
        for (InetSocketAddress inetSocketAddress : liveNodes) {
            String hostTmp = inetSocketAddress.getHostString();
            if (remoteHostAndPort.equals(hostTmp)) {
                recordInRedis(fileName,remoteHostAndPort);
                return true;
            }
        }
        return false;
    }


    /**
     * 将访问记录保存到redis中
     * @param fileName 文件名
     * @param host 访问ip
     */
    private void recordInRedis(String fileName,String host) {
        Long hour = System.currentTimeMillis() / 1000 / (60 * 60);
        String fileNameAccKey = fileName + "_acc";
        if (redisTemplate.hasKey(fileNameAccKey)) {
            // 如果有该文件的访问记录
            // 修改JSON字符串
            String jsonStr = (String) redisTemplate.opsForValue().get(fileNameAccKey);
            JSONObject object = JSON.parseObject(jsonStr);
            if (object.containsKey(host)) {
                // 如果有远程host的访问记录
                JSONObject accessTotal = (JSONObject) object.get(host);
                if (accessTotal.containsKey(String.valueOf(hour))) {
                    // 如果有这个时刻的记录
                    int accessThisHour = (int) accessTotal.get(String.valueOf(hour));
                    accessTotal.replace(String.valueOf(hour), accessThisHour + 1);
                } else {
                    // 如果没有这个时刻的记录
                    accessTotal.put(String.valueOf(hour), 1);
                }
                object.replace(host, accessTotal);
                redisTemplate.opsForValue().set(fileNameAccKey, object.toString());
            } else {
                // 如果没有该ip的远程访问记录
                JSONObject accessThisHour = new JSONObject();
                accessThisHour.put(String.valueOf(hour), 1);
                object.put(host, accessThisHour);
                redisTemplate.opsForValue().set(fileNameAccKey, object.toString());
            }
        } else {
            // 如果没有该文件的访问记录
            JSONObject object = new JSONObject();
            JSONObject accessThisHour = new JSONObject();
            accessThisHour.put(String.valueOf(hour), 1);
            object.put(host, accessThisHour);
            redisTemplate.opsForValue().set(fileNameAccKey, object.toString());
        }
    }




}
