package com.example.edgenode.controller;

import com.example.edgenode.service.FilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
public class FilterController{

    @Autowired
    private FilterService filterService;

    @GetMapping("/getPopularity")
    public Map<String,Map<String,Double>> getPopularity(){
        return filterService.calcPopularity();
    }

    @GetMapping("/getPopularityNormal")
    public Map<String,Map<String,Double>> getPopularityNormal(){
        return filterService.calcPopularityNormal();
    }

    @GetMapping("/getAbsoluteThreshold")
    public double getAbsoluteThreshold(){
        return filterService.getAbsoluteThreshold();
    }

    @GetMapping("/filter")
    public Map<String,Set<String>> filter(){
        return filterService.filter();
    }
}
