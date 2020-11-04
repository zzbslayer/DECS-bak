package com.example.edgenode.service;


import java.util.List;
import java.util.Map;

public interface BasicService {

    /**
     * 获取实时空闲率
     * @return
     */
    Map<String,List<String>> getReealTimeFreeRatio() throws Exception;

    /**
     * 获取本机的空间率
     * @return
     */
    double getFreeRatio() throws Exception;


    /**
     * 获取实时发送的字节数
     * @return
     */
    List<Long> getBytesSent();

}
