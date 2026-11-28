package com.example.timetoeat.global.config;

import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationRejectedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.domain.post.domain.event.PostClosedEvent;
import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.post.domain.event.PostEvent;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

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
    // R18-1: 기본값(amqp-client 60초·핸드셰이크 10초)이면 브로커가 사라졌을 때 발행 스레드가 오래 묶인다
    @Value("${app.rabbitmq.connection-timeout-ms:2000}")
    private int connectionTimeoutMs;
    @Value("${app.rabbitmq.handshake-timeout-ms:3000}")
    private int handshakeTimeoutMs;

    public static final String DOMAIN_EVENTS_EXCHANGE = "domain-events-exchange";
    private static final String NOTIFICATION_QUEUE = "notification-queue";
    private static final String MATCHING_QUEUE = "matching-queue";

    private static final String POST_EVENT_PATTERN = "post.event.#";
    private static final String INVITATION_EVENT_PATTERN = "invitation.event.#";
    private static final String INVITATION_ACCEPTED_KEY = "invitation.event.accepted";

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE);
    }

    @Bean
    public Queue matchingQueue() {
        return new Queue(MATCHING_QUEUE);
    }

    @Bean
    public Binding notificationBindingForPosts(Queue notificationQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(domainEventsExchange).with(POST_EVENT_PATTERN);
    }

    @Bean
    public Binding notificationBindingForInvitations(Queue notificationQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(domainEventsExchange).with(INVITATION_EVENT_PATTERN);
    }

    @Bean
    public Binding matchingBindingForPosts(Queue matchingQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(matchingQueue).to(domainEventsExchange).with(POST_EVENT_PATTERN);
    }

    @Bean
    public Binding matchingBindingForInvitations(Queue matchingQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(matchingQueue).to(domainEventsExchange).with(INVITATION_ACCEPTED_KEY);
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setConnectionTimeout(connectionTimeoutMs);
        connectionFactory.getRabbitConnectionFactory().setHandshakeTimeout(handshakeTimeoutMs);
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        ObjectMapper configuredObjectMapper = objectMapper.copy();
        configuredObjectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(configuredObjectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("post-closed", PostClosedEvent.class);
        idClassMapping.put("post-expired", PostExpiredEvent.class);
        idClassMapping.put("post-completed", PostCompletedEvent.class);
        idClassMapping.put("invitation-sent", InvitationSentEvent.class);
        idClassMapping.put("invitation-accepted", InvitationAcceptedEvent.class);
        idClassMapping.put("invitation-rejected", InvitationRejectedEvent.class);
        typeMapper.setIdClassMapping(idClassMapping);
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        // 재시도 템플릿은 두지 않는다: 발행 실패는 AfterCommitPublisher가 로그로 남기고 입장 API가 복구한다
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
