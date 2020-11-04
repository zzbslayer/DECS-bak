package com.example.edgenode.service.impl;


import com.example.edgenode.service.BasicService;
import com.example.edgenode.service.TradeOffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


/**
 * 权衡模块
 */
@Service
public class TradeOffServiceImpl implements TradeOffService{

    @Autowired
    private BasicService basicService;


    /**
     * 挑选一个节点来写入文件
     * @param file
     * @return
     */
    @Override
    public String pickNodeWrite(MultipartFile file, long tc) throws Exception {
        Map<String,List<String>> freeRatios = basicService.getReealTimeFreeRatio();
        String target = null;
        double maxScore = Double.MIN_VALUE;
        for(Map.Entry<String,List<String>> entry:freeRatios.entrySet()){
            double fileSizeMB = file.getSize()*1.0/1024/1024;
            long estimateTime = (long) (fileSizeMB/Double.parseDouble(entry.getValue().get(1))*1000);
            double score = Double.parseDouble(entry.getValue().get(0))/(1+estimateTime*1.0/tc);
            if(score>maxScore){
                maxScore = score;
                target = entry.getKey();
            }
        }
        return target;
    }
}
