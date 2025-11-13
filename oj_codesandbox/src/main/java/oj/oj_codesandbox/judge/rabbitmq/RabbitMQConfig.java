package oj.oj_codesandbox.judge.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // 判题队列配置
    public static final String JUDGE_EXCHANGE = "judge.exchange";
    public static final String JUDGE_QUEUE = "judge.queue";
    public static final String JUDGE_ROUTING_KEY = "judge.routing.key";
    
    // 结果队列配置
    public static final String RESULT_EXCHANGE = "result.exchange";
    public static final String RESULT_QUEUE = "result.queue";
    public static final String RESULT_ROUTING_KEY = "result.routing.key";
    
    // 死信队列配置
    public static final String DLX_EXCHANGE = "judge.dlx";
    public static final String DLX_QUEUE = "judge.dlx.queue";

    @Bean
    public DirectExchange judgeExchange() {
        return ExchangeBuilder.directExchange(JUDGE_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue judgeQueue() {
        return QueueBuilder.durable(JUDGE_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .withArgument("x-dead-letter-exchange", "judge.dlx")
                .build();
    }

    @Bean
    public Binding judgeBinding() {
        return BindingBuilder.bind(judgeQueue())
                .to(judgeExchange())
                .with(JUDGE_ROUTING_KEY);
    }

    // 结果队列相关配置
    @Bean
    public DirectExchange resultExchange() {
        return ExchangeBuilder.directExchange(RESULT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .withArgument("x-message-ttl", 600000) // 10分钟TTL
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Binding resultBinding() {
        return BindingBuilder.bind(resultQueue())
                .to(resultExchange())
                .with(RESULT_ROUTING_KEY);
    }

    // 死信队列配置
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("judge.dlx.routing.key");
    }

    /**
     * 配置JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate使用JSON序列化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * 配置监听器容器工厂使用JSON序列化
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public Declarables declarables() {
        return new Declarables(
                judgeExchange(),
                judgeQueue(),
                judgeBinding(),
                resultExchange(),
                resultQueue(),
                resultBinding(),
                dlxExchange(),
                dlxQueue(),
                dlxBinding()
        );
    }
}