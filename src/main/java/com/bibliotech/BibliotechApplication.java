package com.bibliotech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.bibliotech.storage.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class BibliotechApplication {
	public static void main(String[] args) {
		SpringApplication.run(BibliotechApplication.class, args);
	}
}