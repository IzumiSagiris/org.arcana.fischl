package com.arcana.fischl.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "fischl-generate", fallback = FischlGenFallBackService.class)
@Service("fischlGenFeignService")
public interface FischlGenFeignService {
    @GetMapping(value = "/gen/generate", produces = {MediaType.APPLICATION_JSON_VALUE})
    String generate();
}
