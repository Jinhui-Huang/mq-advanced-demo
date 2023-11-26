package cn.itcast.mq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description: LazyConfig
 * <br></br>
 * className: LazyConfig
 * <br></br>
 * packageName: cn.itcast.mq.config
 *
 * @author jinhui-huang
 * @version 1.0
 * @email 2634692718@qq.com
 * @Date: 2023/11/26 22:38
 */
@Configuration
public class LazyConfig {

    /**
     * Description: lazyQueue 声明惰性队列
     * @return org.springframework.amqp.core.Queue
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Bean
    public Queue lazyQueue() {
        return QueueBuilder.durable("lazy.queue")
                .lazy()
                .build();
    }

    /**
     * Description: normalQueue 普通队列
     * @return org.springframework.amqp.core.Queue
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Bean
    public Queue normalQueue() {
        return QueueBuilder.durable("normal.queue")
                .build();
    }

}
