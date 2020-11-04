package com.example.edgenode.service;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MetaService {

    // 获取存活的机器--不包含自己
    List<InetSocketAddress> getAliveNodes();

    // 获取存活的机器-包含自己
    List<InetSocketAddress> getAllNodes();

    void updataMetadata(String value);

    Map<String,String> getMetadata();

    Map<String,Map<Long,Integer>> getAccessNum();

    List<String> getAllFiles(String ipAndPort);

    Map<String, Map<String,Map<Integer, Integer>>> getFilesToIpAcc(List<String> files);

    Map<String,Set<String>> getFilesToIp(List<String> files);

    // 更新谁写的文件
    void updateWhoStoreTheFile(String fileName, String ipFrom);
}
