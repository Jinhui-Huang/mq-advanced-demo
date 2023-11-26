package cn.itcast.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {

    /**
     * Description: simpleDirect 声明持久化交换机
     * @return org.springframework.amqp.core.TopicExchange
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public TopicExchange simpleDirect() {
        return new TopicExchange("simple.topic", true, false);
    }

    /**
     * Description: simpleQueue 声明持久化消队列
     * @return org.springframework.amqp.core.Queue
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public Queue simpleQueue() {
        return QueueBuilder.durable("simple.queue").build();
    }

    /**
     * Description: simpleBind 绑定队列和交换机, 并制定routingKey
     * @return org.springframework.amqp.core.Binding
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Bean
    public Binding simpleBind() {
        return BindingBuilder.bind(simpleQueue()).to(simpleDirect()).with("simple.#");
    }

}
