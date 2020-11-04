package com.example.edgenode.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.edgenode.service.MetaService;
import com.example.edgenode.service.ReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.*;


/**
 * 该模块负责与进行查阅
 */
@Service
public class MetaServiceImpl implements MetaService {

    @Value("${application.name}")
    private String applicationName;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${eureka.instance.hostname}")
    private String host;

    @Value("${server.port}")
    private int port;

    @Value("${eureka.instance.metadata-map.nettyport}")
    private int nettyPort;


//    @Autowired
//    private EurekaClient eurekaClient;
//
//    @Autowired
//    private ApplicationInfoManager applicationInfoManager;

    @Autowired
    private ReadService readService;

    private Set<String> aliveNodes = new HashSet<>();

    /**
     * 获取所有的存活节点，不包括自身
     * @return
     */
    @Override
    public List<InetSocketAddress> getAliveNodes() {
        RestTemplate restTemplate = new RestTemplate();
        String res = restTemplate.getForObject("http://localhost:8761/eureka/apps", String.class);
        JSONObject object = JSON.parseObject(res);
        List<InetSocketAddress> liveApplications = new ArrayList<>();
        JSONObject applications = (JSONObject) object.get("applications");
        List<JSONObject> apps = (List<JSONObject>) applications.get("application");
        for (JSONObject application : apps) {
            if (application.get("name").equals(applicationName.toUpperCase())) {
                continue;
            }
            List<JSONObject> instances = (List<JSONObject>) application.get("instance");
            for (JSONObject instance : instances) {
                InetSocketAddress socketAddress = new InetSocketAddress((
                        (JSONObject) instance.get("metadata")).getString("nettyip")
                        + ":" + Integer.parseInt(((JSONObject) instance.get("metadata")).getString("serverport")),
                        Integer.parseInt(((JSONObject) instance.get("metadata")).getString("nettyport")));
                liveApplications.add(socketAddress);
            }
        }
        return liveApplications;
    }

    /**
     * 获取所有的节点，包含自身
     * @return
     */
    @Override
    public List<InetSocketAddress> getAllNodes() {
        List<InetSocketAddress> allNodes = getAliveNodes();
        InetSocketAddress socketAddress = new InetSocketAddress(host+":"+port,nettyPort);
        allNodes.add(socketAddress);
        return allNodes;
    }

    @Override
    public Map<String, String> getMetadata() {
//        return this.eurekaClient.getApplication(applicationName).getInstances().get(0).getMetadata();
        return null;
    }

    @Override
    public Map<String, Map<Long, Integer>> getAccessNum() {
        return null;
    }

    /**
     * 获取机器上的所有文件
     *
     * @param ipAndPort
     * @return
     */
    @Override
    public List<String> getAllFiles(String ipAndPort) {
        return JSONArray.parseArray((String) redisTemplate.opsForValue().get(ipAndPort)).toJavaList(String.class);
    }

    /**
     * 获取一个文件对每个ip的每个小时的访问量
     *
     * @param files
     * @return
     */
    @Override
    public Map<String, Map<String, Map<Integer, Integer>>> getFilesToIpAcc(List<String> files) {
        Map<String, Map<String, Map<Integer, Integer>>> res = new HashMap<>();
        for (String fileName : files) {
            String newFileName = fileName + "_acc";
            String jsonStr = (String) redisTemplate.opsForValue().get(newFileName);
            JSONObject object = JSON.parseObject(jsonStr);
            if (object == null) {
                // 该文件暂没有访问记录
                res.put(fileName, null);
                continue;
            }
            Set<String> ips = object.keySet();
            Map<String, Map<Integer, Integer>> ipToAccessNums = new HashMap<>();
            for (String ip : ips) {
                Map<Integer, Integer> treeMap = new TreeMap<>();
                JSONObject accessNums = (JSONObject) object.get(ip);
                Set<String> accessNumPairs = accessNums.keySet();
                for (String hour : accessNumPairs) {
                    int accInThisHour = (int) accessNums.get(hour);
                    treeMap.put(Integer.parseInt(hour), accInThisHour);
                }
                ipToAccessNums.put(ip, treeMap);
            }
            res.put(fileName, ipToAccessNums);
        }
        return res;
    }

    /**
     * 获取文件被哪些ip访问过
     * @param files
     * @return
     */
    @Override
    public Map<String, Set<String>> getFilesToIp(List<String> files) {
        Map<String, Set<String>> res = new HashMap<>();
        for (String fileName : files) {
            String newFileName = fileName + "_acc";
            String jsonStr = (String) redisTemplate.opsForValue().get(newFileName);
            JSONObject object = JSON.parseObject(jsonStr);
            if (object == null) {
                // 该文件暂没有访问记录
                res.put(fileName, null);
                continue;
            }
            Set<String> ips = object.keySet();
            res.put(fileName, ips);
        }
        return res;
    }

    /**
     * 更新谁写的文件
     * @param fileName
     * @param ipFrom
     */
    @Override
    public void updateWhoStoreTheFile(String fileName, String ipFrom) {
        ipFrom = ipFrom+"_dev";
        if(redisTemplate.hasKey(ipFrom)){
            // 该IP之前存过文件
            JSONArray array = JSONArray.parseArray((String) redisTemplate.opsForValue().get(ipFrom));
            array.add(fileName);
            redisTemplate.opsForValue().set(ipFrom,array.toString());
            return;
        }
        JSONArray array = new JSONArray();
        array.add(fileName);
        redisTemplate.opsForValue().set(ipFrom,array.toString());
    }

    @Override
    public void updataMetadata(String value) {
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put("zfx", value);
//        applicationInfoManager.registerAppMetadata(metadata);
    }


}
