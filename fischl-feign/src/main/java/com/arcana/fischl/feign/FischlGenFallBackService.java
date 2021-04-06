package com.arcana.fischl.feign;

import org.springframework.stereotype.Component;

@Component
public class FischlGenFallBackService implements FischlGenFeignService {
    @Override
    public String generate() {
        return "srroy, the gen-service is not avaliable";
    }
}
