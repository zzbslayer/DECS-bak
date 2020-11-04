package com.example.edgenode.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public interface WriteService {
    String writeFile(MultipartFile file,long writeTime) throws Exception;
}
