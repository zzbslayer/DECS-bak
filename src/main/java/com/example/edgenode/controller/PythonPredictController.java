package com.example.edgenode.controller;

import com.example.edgenode.service.PythonPredictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class PythonPredictController {

    @Autowired
    PythonPredictService pythonPredictService;

    @GetMapping("/getPredictedResult")
    public int getPredictedResult() throws IOException {
        return pythonPredictService.getPredictedResult();
    }
}
