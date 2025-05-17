package com.ssafy.cholog.global.config;

import com.ssafy.cholog.domain.user.enums.Provider;
import com.ssafy.cholog.domain.user.oauth.strategy.OAuthStrategy;
import com.ssafy.cholog.domain.user.oauth.strategy.SsafyOAuthStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class OAuthConfig {
    @Bean
    public Map<Provider, OAuthStrategy> oAuthStrategyMap(
            SsafyOAuthStrategy ssafyStrategy
    ) {
        Map<Provider, OAuthStrategy> strategyMap = new EnumMap<>(Provider.class);
        strategyMap.put(Provider.SSAFY, ssafyStrategy);
        return strategyMap;
    }
}

