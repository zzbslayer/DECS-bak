package com.example.edgenode.controller;

import com.example.edgenode.service.MetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@RestController
public class MetaController {
    @Autowired
    private MetaService metaService;

    @GetMapping("/services")
    public List<InetSocketAddress> getServices(){
        return metaService.getAliveNodes();
    }

    @GetMapping("/getMetadata")
    public Map<String,String> getMetaData(){
        return metaService.getMetadata();
    }

    @GetMapping("/updateMetadata/{value}")
    public void updataMetadata(@PathVariable("value") String value){
        metaService.updataMetadata(value);
    }

    @GetMapping("/accessNum")
    public Map<String,Map<Long,Integer>> getAccessNum(){
        return metaService.getAccessNum();
    }
}
