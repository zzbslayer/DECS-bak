package com.example.edgenode.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.example.edgenode.service.MetaService;
import com.example.edgenode.service.TradeOffService;
import com.example.edgenode.service.WriteService;
import com.example.edgenode.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;


/**
 * 数据写模块
 */
@Service
public class WriteFileServiceImpl implements WriteService {

    @Value("${eureka.instance.hostname}")
    private String hostname;

    @Value("${server.port}")
    private String port;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MetaService metaService;

    @Autowired
    private TradeOffService tradeOffService;

    @Override
    public String writeFile(MultipartFile file,long writeTime) throws Exception {
        if (file.isEmpty()) {
            return "上传失败，请选择文件";
        }

        long tc = System.currentTimeMillis()-writeTime;

        String fileName = file.getOriginalFilename();
        String filePath = "/Users/dawnchau/";
        File dest = new File(filePath + fileName);
        file.transferTo(dest);

        // 获取写文件的人的ip
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipFrom = Utils.getIpAddr(request);
        request.getContentLength();
        updateWhoStoreTheFile(fileName,ipFrom);

        String target = tradeOffService.pickNodeWrite(file,tc);

        List<String> ips = null;
        String hostNameAndPort = hostname + ":" + port;
        // 记录到redis中 <文件名，List<ip>>
        String redisFileNameKey = fileName + "_store";

        // 将文件名到ip列表的映射存储到redis中
        recordRedisFileToIPList(redisFileNameKey, hostNameAndPort);

        // 将ip到文件名的映射存储到redis中
        recordRedisIpToFileName(hostNameAndPort,fileName);

        return "上传成功";
    }

    /**
     * 更新谁存储的文件
     * @param fileName
     * @param ipFrom
     */
    private void updateWhoStoreTheFile(String fileName, String ipFrom) {
        metaService.updateWhoStoreTheFile(fileName,ipFrom);
    }


    /**
     * 将ip到文件名的映射写入到redis中
     * @param hostNameAndPort ip
     * @param fileName 文件名
     */
    private void recordRedisIpToFileName(String hostNameAndPort, String fileName) {
        if(!redisTemplate.hasKey(hostNameAndPort)){
            // 如果redis中没有
            JSONArray array = new JSONArray();
            array.add(fileName);
            redisTemplate.opsForValue().set(hostNameAndPort,array.toString());
        }else{
            JSONArray array = JSONArray.parseArray((String) redisTemplate.opsForValue().get(hostNameAndPort));
            if(!array.contains(fileName)){
                array.add(fileName);
            }
            redisTemplate.opsForValue().set(hostNameAndPort,array.toString());
        }
    }

    /**
     * 将文件名到ip列表的信息写入到redis中
     * @param redisFileNameKey 改造后的文件名
     * @param hostNameAndPort  ip：端口
     */
    private void recordRedisFileToIPList(String redisFileNameKey, String hostNameAndPort) {
        if (redisTemplate.hasKey(redisFileNameKey)) {
            if (redisTemplate.opsForZSet().score(redisFileNameKey, hostname) == null) {
                // 不存在
                redisTemplate.opsForZSet().add(redisFileNameKey, hostNameAndPort, System.currentTimeMillis());
            }
        } else {
            redisTemplate.opsForZSet().add(redisFileNameKey, hostNameAndPort, System.currentTimeMillis());
        }
    }
}
