package com.arcana.fischl.controller;

import com.arcana.fischl.feign.FischlZkFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/gen")
public class HomeController {
    @Autowired
    FischlZkFeignService fischlZkFeignService;

    @ResponseBody
    @GetMapping(value = "/generate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String arcana() {
        String res = "this is gen -> generate.";
        res += fischlZkFeignService.arcana();
        return res;
    }
}