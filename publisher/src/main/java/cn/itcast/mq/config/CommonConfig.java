package cn.itcast.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CommonConfig implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        /*获取RabbitTemplate对象*/
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        /*配置ReturnCallback*/
        rabbitTemplate.setReturnsCallback(reMsg -> {
            /*判断是不是延迟消息, 如果是延迟消息则可以忽略*/
            if (reMsg.getMessage().getMessageProperties().getReceivedDelay() > 0) {
                /*是延迟消息, 忽略这个错误*/
                return;
            }
            /*记录日志*/
            log.error("消息发送到队列失败, 响应码: {}, 失败原因: {}, 交换机: {}, 路由key: {}, 消息: {}",
                    reMsg.getReplyCode(),
                    reMsg.getReplyText(),
                    reMsg.getExchange(),
                    reMsg.getRoutingKey(),
                    reMsg.getMessage());
            /*如果有必要的话, 需要重发消息*/
        });

    }
}
