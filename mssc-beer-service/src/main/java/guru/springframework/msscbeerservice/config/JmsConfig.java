package guru.springframework.msscbeerservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@RequiredArgsConstructor
public class JmsConfig {
    public static final String BREWING_REQUEST_QUEUE = "brewing-request";
    public static final String NEW_INVENTORY_QUEUE = "new-inventory";

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setTargetType(MessageType.TEXT);
        messageConverter.setTypeIdPropertyName("_type");
        messageConverter.setObjectMapper(objectMapper);
        return messageConverter;
    }
}
