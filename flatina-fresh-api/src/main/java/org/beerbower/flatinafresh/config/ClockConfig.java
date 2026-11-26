package org.beerbower.flatinafresh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Configuration
public class ClockConfig {
    /** The system clock, or a fixed day when app.today is set (tests). */
    @Bean
    Clock clock(@Value("${app.today:}") String today) {
        if (StringUtils.hasText(today)) {
            return Clock.fixed(LocalDate.parse(today).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
        return Clock.systemUTC();
    }
}
