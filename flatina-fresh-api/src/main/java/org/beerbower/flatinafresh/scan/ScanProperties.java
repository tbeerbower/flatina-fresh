package org.beerbower.flatinafresh.scan;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scan")
public record ScanProperties(String anthropicApiKey, String model, int maxImagePx, int storedImagePx) {
}
