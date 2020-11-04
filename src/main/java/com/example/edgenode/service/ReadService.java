package com.example.edgenode.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ReadService {
    ResponseEntity<InputStreamResource> read(String fileName) throws IOException, URISyntaxException;
}
