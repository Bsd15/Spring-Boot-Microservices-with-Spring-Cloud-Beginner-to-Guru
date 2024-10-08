package guru.sfg.beer.order.service.config;

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

    public static final String VALIDATE_ORDER = "validate-order";
    public static final String VALIDATE_ORDER_RESULT = "validate-order-result";
    public static final String ALLOCATE_ORDER_QUEUE = "allocate-order";
    public static final String ALLOCATE_ORDER_RESULT = "allocate-order-result";
    public static final String ALLOCATE_FAILURE_QUEUE = "allocation-failure";
    public static final String DEALLOCATE_ORDER_QUEUE = "deallocate-order" ;


    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setTargetType(MessageType.TEXT);
        messageConverter.setTypeIdPropertyName("_type");
        messageConverter.setObjectMapper(objectMapper);
        return messageConverter;
    }
}
