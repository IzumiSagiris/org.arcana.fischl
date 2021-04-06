package com.arcana.fischl.feign;

import org.springframework.stereotype.Component;

@Component
public class FischlZkFallBackService implements FischlZkFeignService {
    public String arcana() {
        return "srroy,the zk-service is not avaliable";
    }
}
