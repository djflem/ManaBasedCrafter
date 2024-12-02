package com.smeej.manabasedcrafter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ManaBasedCrafterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManaBasedCrafterApplication.class, args);
	}

}
