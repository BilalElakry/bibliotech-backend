package com.bibliotech;

import com.bibliotech.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(StorageProperties.class)
public class BibliotechApplication {
	public static void main(String[] args) {
		SpringApplication.run(BibliotechApplication.class, args);
	}
}