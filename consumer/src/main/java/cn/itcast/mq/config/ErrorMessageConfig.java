package cn.itcast.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description: ErrorMessageConfig 失败消息的处理机制
 * <br></br>
 * className: ErrorMessageConfig
 * <br></br>
 * packageName: cn.itcast.mq.config
 *
 * @author jinhui-huang
 * @version 1.0
 * @email 2634692718@qq.com
 * @Date: 2023/11/25 20:36
 */
@Configuration
public class ErrorMessageConfig {

    /**
     * Description: errorMessageExchange 失败消息交换机
     * @return org.springframework.amqp.core.DirectExchange
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public DirectExchange errorMessageExchange() {
        return new DirectExchange("error.direct");
    }

    /**
     * Description: errorQueue 存放失败消息的队列
     * @return org.springframework.amqp.core.Queue
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public Queue errorQueue() {
        return new Queue("error.queue");
    }

    /**
     * Description: errorMessageBinding 绑定队列和交换机
     * @return org.springframework.amqp.core.Binding
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public Binding errorMessageBinding() {
        return BindingBuilder.bind(errorQueue()).to(errorMessageExchange()).with("error");
    }

    /**
     * Description: republishMessageRecoverer 失败消息的重试机制
     * @return org.springframework.amqp.rabbit.retry.MessageRecoverer
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public MessageRecoverer republishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", "error");
    }
}
