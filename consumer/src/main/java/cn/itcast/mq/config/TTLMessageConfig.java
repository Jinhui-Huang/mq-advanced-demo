package cn.itcast.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description: TTLMessageConfig
 * <br></br>
 * className: TTLMessageConfig
 * <br></br>
 * packageName: cn.itcast.mq.config
 *
 * @author jinhui-huang
 * @version 1.0
 * @email 2634692718@qq.com
 * @Date: 2023/11/26 21:06
 */
@Configuration
public class TTLMessageConfig {

    /**
     * Description: ttlDirectExchange 指定声明ttl交换机
     * @return org.springframework.amqp.core.DirectExchange
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Bean
    public DirectExchange ttlDirectExchange() {
        return new DirectExchange("ttl.direct");
    }

    /**
     * Description: ttlQueue 声明ttl队列, 并指定死信交换机
     * @return org.springframework.amqp.core.Queue
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Bean
    public Queue ttlQueue() {
        return QueueBuilder
                .durable("ttl.queue")
                .ttl(10000)
                .deadLetterExchange("dl.direct")
                .deadLetterRoutingKey("dl")
                .build();
    }

    @Bean
    public Binding ttlBinding() {
        return BindingBuilder.bind(ttlQueue()).to(ttlDirectExchange()).with("ttl");
    }
}
