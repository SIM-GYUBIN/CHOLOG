package com.example.logserver.config;

import com.example.logserver.model.LogEntry;
import com.example.logserver.util.LogEntryDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Jackson JSON 설정을 위한 구성 클래스
 */
@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    /**
     * 기본 ObjectMapper 설정
     * 
     * @return 설정된 ObjectMapper 인스턴스
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // null 필드 제외 설정
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // LogEntry 역직렬화를 위한 모듈 등록
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LogEntry.class, new LogEntryDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }
    
    /**
     * null 값을 포함하는 ObjectMapper 설정
     * 
     * @return 설정된 ObjectMapper 인스턴스
     */
    @Bean(name = "objectMapperWithNulls")
    public ObjectMapper objectMapperWithNulls() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // null 필드 포함 설정
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // LogEntry 역직렬화를 위한 모듈 등록
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LogEntry.class, new LogEntryDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }
    
    /**
     * HTTP 응답에서 null 필드를 항상 포함하도록 하는 MessageConverter 설정
     * 
     * @return MappingJackson2HttpMessageConverter 인스턴스
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS); // null 값 항상 포함
        
        // LogEntry 역직렬화를 위한 모듈 등록
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LogEntry.class, new LogEntryDeserializer());
        mapper.registerModule(module);
        
        converter.setObjectMapper(mapper);
        return converter;
    }
} 