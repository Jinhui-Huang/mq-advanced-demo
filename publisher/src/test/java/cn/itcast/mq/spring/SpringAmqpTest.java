package cn.itcast.mq.spring;

import cn.itcast.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testLazyQueue() {
        for (int i = 0; i < 1000000; i++) {
            /*1. 准备消息*/
            Message message = MessageBuilder.withBody(("hello, spring --" + i).getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            /*2. 发送消息*/
            rabbitTemplate.convertAndSend("lazy.queue", message);
        }
    }

    @Test
    public void testNormalQueue() {
        for (int i = 0; i < 1000000; i++) {
            /*1. 准备消息*/
            Message message = MessageBuilder.withBody(("hello, spring --" + i).getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            /*2. 发送消息*/
            rabbitTemplate.convertAndSend("normal.queue", message);
        }
    }

    /**
     * Description: testTTLMessage 发送至ttl交换机
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Test
    public void testTTLMessage() {
        /*1. 准备消息*/
        Message message = MessageBuilder.withBody("hello, ttl message".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setExpiration("5000") /*5秒钟延迟消息*/
                .build();
        /*2. 发送消息*/
        rabbitTemplate.convertAndSend("ttl.direct", "ttl", message);
        /*3. 记录日志*/
        log.info("消息成功发送!");
    }

    /**
     * Description: testDurableMessage 直接发送至消息队列
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @Test
    public void testDurableMessage() {
        /*1. 准备消息*/
        Message message = MessageBuilder.withBody("hello, spring".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        /*2. 发送消息*/
        rabbitTemplate.convertAndSend("simple.queue", message);
    }

    @Test
    public void testSendMessage2SimpleQueue() {
        /*1. 准备消息和routingKey*/
        String routingKey = "simple.queue";
        User user = new User();
        user.setUsername("张三");
        user.setPassword("123456");
        user.setPhone("00-123456789");
        user.setAge(22);

        /*2. 准备CorrelationData, 指定发送失败或成功时的回调消息和消息id*/
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        correlationData.getFuture().addCallback(result -> {
            assert result != null;
            if (result.isAck()) {
                /*ACK*/
                log.debug("消息成功投递到交换机! 消息ID: {}", correlationData.getId());
            } else {
                /*NACK*/
                log.error("消息投递到交换机失败! 消息ID: {}", correlationData.getId());
                /*消息重发*/
            }
        }, ex -> {
            /*记录日志*/
            log.error("消息发送失败", ex);
            /*重发消息*/
        });
        rabbitTemplate.convertAndSend("simple.topic", routingKey, user, correlationData);
    }

    @Test
    public void testSendDelayMessage() {
        /*1. 准备消息*/
        /*1. 准备消息*/
        Message message = MessageBuilder.withBody("hello, delay message".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("x-delay", 5000)
                .build();

        /*2. 准备CorrelationData, 指定发送失败或成功时的回调消息和消息id*/
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        /*3. 发送消息*/
        rabbitTemplate.convertAndSend("delay.direct", "delay", message, correlationData);

        log.info("发送消息成功");
    }

    @Test
    public void testSendQuorumMessage() {
        /*1. 准备消息*/
        /*1. 准备消息*/
        /*Message message = MessageBuilder.withBody("hello, delay message".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("x-delay", 5000)
                .build();*/

        /*3. 发送消息*/
        rabbitTemplate.convertAndSend("canal.exchange", "canal-routingkey", "hello canal");

        log.info("发送消息成功");
    }
}
