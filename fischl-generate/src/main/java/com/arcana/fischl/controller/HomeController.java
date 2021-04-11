package com.arcana.fischl.controller;

import com.arcana.fischl.feign.FischlZkFeignService;
import com.arcana.fischl.pool.ZookeeperConnComponent;
import com.arcana.fischl.pool.ZookeeperConnectionPool;
import com.google.gson.JsonObject;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/gen")
public class HomeController {
    @Autowired
    FischlZkFeignService fischlZkFeignService;

    @Autowired
    ZookeeperConnComponent zookeeperConnComponent;

    @GetMapping(value = "/generate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> arcana() throws Exception{
        ZookeeperConnectionPool pool = zookeeperConnComponent.getZookeeperConnectionPool();
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = pool.getResource();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("version", "1");

            zooKeeper.create("/gen/generate/", jsonObject.toString().getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);

            String res = "this is gen -> generate.";
            res += fischlZkFeignService.arcana();
            jsonObject.addProperty("result", res);

            return ResponseEntity.ok(jsonObject.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(zooKeeper != null) {
                pool.release(zooKeeper);
            }
        }

        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}