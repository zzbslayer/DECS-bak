package com.example.edgenode.controller;

import com.example.edgenode.service.ReadService;
import com.example.edgenode.service.WriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
public class RWController {
    @Autowired
    private ReadService readService;

    @Autowired
    private WriteService writeService;

    @GetMapping(value = "/readFile/{fileName}")
    public ResponseEntity<InputStreamResource> readFile(@PathVariable("fileName") String fileName) throws IOException, URISyntaxException {
        return readService.read(fileName);
    }

    @PostMapping("/writeFile")
    public String wirteFile(@RequestParam("file") MultipartFile file, @RequestParam("time") long time) throws Exception {
        return writeService.writeFile(file,time);
    }


}
