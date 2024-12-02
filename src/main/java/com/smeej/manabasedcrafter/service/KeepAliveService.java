package com.smeej.manabasedcrafter.service;

import com.austinv11.servicer.Service;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class KeepAliveService {
    @Scheduled(fixedRate = 1 * 1000 * 60)
    public void reportCurrentTime() {
        System.out.println(System.currentTimeMillis());
    }
}
