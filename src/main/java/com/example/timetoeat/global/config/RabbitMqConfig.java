package com.example.timetoeat.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private int port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;

    public static final String EXCHANGE_NAME = "post-event-exchange";
    private static final String NOTIFICATION_QUEUE_NAME = "notification-queue";
    private static final String MATCHING_QUEUE_NAME = "matching-queue";
    public static final String ROUTING_KEY = "post.event.#";

    @Bean
    public TopicExchange exchange() { return new TopicExchange(EXCHANGE_NAME); }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE_NAME);
    }

    @Bean
    public Queue matchingQueue() {
        return new Queue(MATCHING_QUEUE_NAME);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        // notification-queue는 "post.event.*" 패턴의 메시지만 받도록 설정
        return BindingBuilder.bind(notificationQueue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding matchingBinding(Queue matchingQueue, TopicExchange exchange) {
        // matching-queue도 동일하게 "post.event.*" 패턴의 메시지만 받도록 설정
        return BindingBuilder.bind(matchingQueue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
