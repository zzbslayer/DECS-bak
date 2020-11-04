package com.example.edgenode.controller;

import com.example.edgenode.service.BasicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BasicController {

    @Autowired
    private BasicService basicService;

    /**
     * 获取空闲的利用率
     * @return
     */
    @GetMapping("/freeRatio")
    public ResponseEntity<Double> getFreeRatio() throws Exception {
        double freeRatio = basicService.getFreeRatio();
        ResponseEntity<Double> res = ResponseEntity
                .ok()
                .contentLength(String.valueOf(freeRatio).length())
                .contentType(MediaType.APPLICATION_JSON)
                .body(freeRatio);
        return res;
    }

    @GetMapping("/freeRealTimeRatio")
    public Map<String,List<String>> getReealTimeFreeRatio() throws Exception {
        return basicService.getReealTimeFreeRatio();
    }

}
