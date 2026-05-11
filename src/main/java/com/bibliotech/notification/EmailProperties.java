package com.bibliotech.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bibliotech.mail")
public class EmailProperties {
    private boolean enabled = true;
    private String from;
    private String appName = "BiblioTech";
}