package cn.itcast.mq.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringRabbitListener {


    /**
     * Description: listenSimpleQueue 声明队列和交换机, 同时绑定routingKey, 交换机模式为topic
     *
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     */
    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueue(String msg) {
        System.out.println("消费者接收到simple.queue的消息：【" + msg + "】");
        // System.out.println(1 / 0); /*测试异常*/
        log.info("消费者处理消息成功");
    }

    /**
     * Description: listenDlQueue 声明死信交换机并监听
     *
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "dl.queue", durable = "true"),
            exchange = @Exchange(name = "dl.direct"),
            key = "dl"
    ))
    public void listenDlQueue(String msg) {
        log.info("接收到了dl.queue的未消费延迟消息 ==> " + msg);
    }

    /**
     * Description: listenTTLQueue 监听ttl消息
     *
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     */
    @RabbitListener(queues = "ttl.queue")
    public void listenTTLQueue(String msg) {
        log.info("监听到了ttl消息 ==> " + msg);
    }

    /**
     * Description: listenDelayQueue 监听延迟队列
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "delay.queue", durable = "true"),
            exchange = @Exchange(name = "delay.direct", delayed = "true"),
            key = "delay"
    ))
    public void listenDelayQueue(String msg) {
        log.info("接收到了delay.queue的延迟消息 ==> " + msg);
    }

    /**
     * Description: listenNormalQueue 处理普通队列的堆积消息, 请在测试时打开注解
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @RabbitListener(queues = "normal.queue")
    public void listenNormalQueue(String msg) {
        log.info("接收到了normal.queue的消息 ==> " + msg);
    }

    /**
     * Description: listenLazyQueue 处理惰性队列的堆积消息, 请子啊测试时打开注解
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @RabbitListener(queues = "lazy.queue")
    public void listenLazyQueue(String msg) {
        log.info("接收到了lazy.queue的消息 ==> " + msg);
    }


}
