package com.bibliotech.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bibliotech.storage")
public class StorageProperties {
    private String uploadDir;
}