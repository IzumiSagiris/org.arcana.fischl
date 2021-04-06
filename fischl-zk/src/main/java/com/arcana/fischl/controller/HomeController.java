package com.arcana.fischl.controller;

import com.arcana.fischl.service.ArcanaHomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/home")
public class HomeController {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ArcanaHomeService arcanaHomeService;

    public String serviceUrl() {
        List<ServiceInstance> list = discoveryClient.getInstances("fischl-generate");
        if (list != null && list.size() > 0) {
            return list.get(0).getUri().toString();
        }
        return null;
    }

    @ResponseBody
    @GetMapping(value = "/arcana", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String arcana() throws Exception {
        String res = "this is home -> arcana.";
        System.out.println("here");
        Thread.sleep(2000);
        res += arcanaHomeService.zkservice();
        return res;
    }
}