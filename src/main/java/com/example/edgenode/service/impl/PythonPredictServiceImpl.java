package com.example.edgenode.service.impl;

import com.example.edgenode.service.PythonPredictService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;


/**
 * 预测模块
 */
@Service
public class PythonPredictServiceImpl implements PythonPredictService {


    @Override
    public int getPredictedResult() throws IOException {
        Process process = Runtime.getRuntime().exec("python3 ./src/main/resources/lstm.py");
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        LineNumberReader lineNumberReader = new LineNumberReader(inputStreamReader);
        String result = lineNumberReader.readLine();
        inputStreamReader.close();
        System.out.println(result);
        return Integer.parseInt(result);
    }

}
