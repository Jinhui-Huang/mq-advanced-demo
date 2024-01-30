package cn.itcast.mq.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description: ClusterConfig
 * <br></br>
 * className: ClusterConfig
 * <br></br>
 * packageName: cn.itcast.mq.config
 *
 * @author jinhui-huang
 * @version 1.0
 * @email 2634692718@qq.com
 * @Date: 2023/11/27 22:04
 */
@Configuration
public class ClusterConfig {

    @Bean
    public Queue quorumQueue() {
        return QueueBuilder.durable("quorum.queue").quorum().build();
    }

}
