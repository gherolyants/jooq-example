package org.gh;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqExampleConfiguration {

    @Bean
    public Settings settings() {
        return new Settings()
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderFormatted(true);
    }

    @Bean
    public DefaultConfigurationCustomizer configurationCustomizer(CustomLoggerListener customLoggerListener) {
        return (DefaultConfiguration c) -> {
            c.setSQLDialect(SQLDialect.MYSQL);
            c.setExecuteListener(customLoggerListener);
        };
    }
}
