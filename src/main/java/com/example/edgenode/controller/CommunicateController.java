package com.example.edgenode.controller;

import com.example.edgenode.service.CommunicateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommunicateController {

    @Autowired
    private CommunicateService communicateService;

    @GetMapping("/broadcast/{msg}")
    public void broadCast(@PathVariable("msg") String msg) throws InterruptedException {
        communicateService.broadCast(msg);
    }
}
