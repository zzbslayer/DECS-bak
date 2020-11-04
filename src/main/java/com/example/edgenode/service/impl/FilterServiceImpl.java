package com.example.edgenode.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.example.edgenode.service.FilterService;
import com.example.edgenode.service.MetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FilterServiceImpl implements FilterService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MetaService metaService;

    @Value("${eureka.instance.hostname}")
    private String host;

    @Value("${server.port}")
    private int port;

    /**
     * 计算一条数据针对于每个ip的热度
     *
     * @return
     */
    @Override
    public Map<String, Map<String, Double>> calcPopularity() {
        Long hour = System.currentTimeMillis() / 1000 / (60 * 60);
        Map<String, Map<String, Double>> res = new HashMap<String, Map<String, Double>>();
        String hostAndPort = host + ":" + port;
        JSONArray array = JSONArray.parseArray((String) redisTemplate.opsForValue().get(hostAndPort));
        // 获取该机器上的所有文件
        List<String> filesInThisMachine = metaService.getAllFiles(hostAndPort);
        //获取每个文件的对每个ip的每个小时的访问量访问量
        Map<String, Map<String, Map<Integer, Integer>>> filesToIpAcc = metaService.getFilesToIpAcc(filesInThisMachine);

        // 遍历每个文件
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> file : filesToIpAcc.entrySet()) {
            Map<String, Double> popularityToIp = new HashMap<>();

            // 没有访问记录
            if (file.getValue() == null) {
                res.put(file.getKey(), null);
                continue;
            }
            // 遍历每个ip
            for (Map.Entry<String, Map<Integer, Integer>> ip : file.getValue().entrySet()) {
                // 遍历每个小时，计算热度
                double popularity = 0;
                TreeMap<Integer, Integer> treeMap = (TreeMap<Integer, Integer>) ip.getValue();
                int start = treeMap.firstKey();
                // 统计到上个小时
                int last = ((int) (System.currentTimeMillis() / 1000 / (60 * 60))) - 1;
                int size = last - start + 1;
                for (int i = start; i <= last; i++) {
                    if (treeMap.containsKey(i)) {
                        int index = i - start;
                        // 这里感觉不用除以3600，不用计算频率
                        popularity += (treeMap.get(i) * 1.0) * Math.pow(Math.E, (index - size));
                    }
                }
                popularityToIp.put(ip.getKey(), popularity);
                // 将热度，ip，小时，记录到元数据中
                redisTemplate.opsForValue().set(file.getKey() + "_" + ip.getKey() + "_" + hour, String.valueOf(popularity), 1, TimeUnit.HOURS);
            }
            res.put(file.getKey(), popularityToIp);
        }
        return res;
    }


    /**
     * 正则化
     *
     * @return
     */
    @Override
    public Map<String, Map<String, Double>> calcPopularityNormal() {
        double max = 0;
        double min = 1;
        Map<String, Map<String, Double>> res = new HashMap<>();
        Map<String, Map<String, Double>> popularity = getPopularity();
        if (popularity.size() == 0) {
            return popularity;
        }
        // 获取最大值和最小值
        Iterator<Map.Entry<String, Map<String, Double>>> iterator = popularity.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Map<String, Double>> file = iterator.next();
            if (file.getValue() == null) {
                continue;
            }
            for (Map.Entry<String, Double> ip : file.getValue().entrySet()) {
                double value = ip.getValue();
                max = value > max ? value : max;
                min = value < min ? value : min;
            }
        }

        for (Map.Entry<String, Map<String, Double>> file : popularity.entrySet()) {
            Map<String, Double> ipToPopularityNormal = new HashMap<>();
            if (file.getValue() == null) {
                continue;
            }
            for (Map.Entry<String, Double> ips : file.getValue().entrySet()) {
                double value = ips.getValue();
                String ip = ips.getKey();
                // 如果所有的热度都相同
                if (max == min) {
                    ipToPopularityNormal.putIfAbsent(ip, 1.0);
                } else {
                    double score = (value - min) / (max - min);
                    ipToPopularityNormal.putIfAbsent(ip, score);
                }
            }
            res.put(file.getKey(), ipToPopularityNormal);
        }
        return res;
    }

    /**
     * 获取本机上的所有文件对应的ip对应的访问热度
     *
     * @return
     */
    private Map<String, Map<String, Double>> getPopularity() {
        Long hour = System.currentTimeMillis() / 1000 / (60 * 60);
        Map<String, Map<String, Double>> res = new HashMap<>();
        String hostAndPort = host + ":" + port;
        List<String> files = metaService.getAllFiles(hostAndPort);
        Map<String, Set<String>> fileToIps = metaService.getFilesToIp(files);

        for (Map.Entry<String, Set<String>> entry : fileToIps.entrySet()) {
            if (entry.getValue() == null) {
                // 该文件没有访问记录
                continue;
            }
            Map<String, Double> ipToPopularity = new HashMap<>();
            for (String ipAndHost : entry.getValue()) {
                String key = entry.getKey() + "_" + ipAndHost + "_" + hour;
                if (redisTemplate.opsForValue().get(key) == null) {
                    continue;
                }
                ipToPopularity.put(ipAndHost, Double.valueOf((String) redisTemplate.opsForValue().get(key)));
            }
            res.put(entry.getKey(), ipToPopularity);
        }
        return res;
    }


    /**
     * 获取绝对门槛-一定要等每台机器都计算完自己的热度之后才调用
     *
     * @return
     */
    @Override
    public Double getAbsoluteThreshold() {
        Long hour = System.currentTimeMillis() / 1000 / (60 * 60);
        //直接判断redis中有没有存储的绝对门槛，如果有，则直接从redis中读取
        if (redisTemplate.hasKey("threshold_" + hour)) {
            return Double.valueOf((String) redisTemplate.opsForValue().get("threshold_" + hour));
        }
        // 第一步，获取所有存活的机器
        List<InetSocketAddress> allNodes = metaService.getAllNodes();
        // 第二步，获取每个机器上的文件
        double totalPopularity = 0.0;

        int count = 0;
        for (InetSocketAddress socketAddress : allNodes) {
            String tempHostAndPort = socketAddress.getHostName();
            List<String> files = metaService.getAllFiles(tempHostAndPort);
            Map<String, Set<String>> fileToIps = metaService.getFilesToIp(files);
            //第三步，获取每个文件都有谁访问过

            for (Map.Entry<String, Set<String>> entry : fileToIps.entrySet()) {
                if (entry.getValue() == null) {
                    // 该文件没有访问记录
                    continue;
                }
                for (String ipAndHost : entry.getValue()) {
                    String key = entry.getKey() + "_" + ipAndHost + "_" + hour;
                    if (redisTemplate.opsForValue().get(key) == null) {
                        continue;
                    }
                    totalPopularity += Double.valueOf((String) redisTemplate.opsForValue().get(key));
                    count++;
                }
            }
        }
        double threshold = totalPopularity / count;
        redisTemplate.opsForValue().set("threshold_" + hour, String.valueOf(threshold), 1, TimeUnit.HOURS);
        return threshold;
    }

    /**
     * 真正的过滤操作，返回的结果是<文件名，ip>
     *
     * @return
     */
    @Override
    public Map<String, Set<String>> filter() {
        Map<String, Set<String>> res = new HashMap<>();
        double threshold = getAbsoluteThreshold();
        Map<String, Map<String, Double>> normal = calcPopularityNormal();
        Map<String, Map<String, Double>> popularity = getPopularity();

        // 相对过滤
        for (Map.Entry<String, Map<String, Double>> normalTemp : normal.entrySet()) {
            Set<String> ips = new HashSet<>();
            for (Map.Entry<String, Double> normalInnerTemp : normalTemp.getValue().entrySet()) {
                double score = normalInnerTemp.getValue();
                double popularityTemp = popularity.get(normalTemp.getKey()).get(normalInnerTemp.getKey());
                if (score >= 0.7 && popularityTemp >= threshold) {
                    ips.add(normalInnerTemp.getKey());
                }
            }
            res.put(normalTemp.getKey(),ips);
        }
        return res;
    }

}
