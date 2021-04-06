package com.arcana.fischl.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "fischl-zk", fallback = FischlZkFallBackService.class)
@Service("fischlZkFeignService")
public interface FischlZkFeignService {
    @GetMapping(value = "/home/arcana", produces = {MediaType.APPLICATION_JSON_VALUE})
    String arcana();
}
