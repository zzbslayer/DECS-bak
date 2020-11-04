package com.example.edgenode.service;

import org.springframework.web.multipart.MultipartFile;

import java.net.URISyntaxException;
import java.text.ParseException;

public interface TradeOffService {

    // 选择一个节点来写入
    String pickNodeWrite(MultipartFile file, long tc) throws Exception;
}
