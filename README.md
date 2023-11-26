# RabbitMQ高级特性

## 一. MQ的一些问题

- **消息可靠性问题**
  - 如何确保发送的消息至少被消费一次
- **延迟消息问题**
  - 如何实现消息的延迟投递
- **高可用问题**
  - 如何避免单点的MQ故障而导致的不可用问题
- **消息堆积问题**
  - 如何解决数百万消息堆积, 无法及时消费的问题



## 二. 消息可靠性

### (1). 消息可靠性问题

消息从生产者发送到exchange, 再到queue, 再到消费者, 有那些导致消息丢失的可能性

- 发送时丢失:
  - 生产者发送的消息未送达exchange
  - 消息到达exchange后未到达queue
- MQ宕机, queue将消息丢失
- consumer接受到消息后未消费就宕机

![image-20231125153030021](/home/huian/.config/Typora/typora-user-images/image-20231125153030021.png)

### (2). 生产者消息确认

**生产者确认机制**

RabbitMQ提供了publisher confirm机制来避免消息发送到MQ过程中丢失. 消息发送到MQ以后, 会返回一个结果给发送者, 标示消息是否处理成功, 结果有两种请求:

- publisher-confirm, 发送者确认
  - 消息成功投递到交换机, 返回ack
  - 消息未投递到交换机, 返回nack
- publisher-return, 发送者回执
  - 消息投递到交换机了, 但是没有路由到队列. 返回ACK, 及路由失败原因.

**注意:** 确认机制发送消息时, 需要给每个消息设置一个全局唯一id, 以区分不同消息, 避免ack冲突



#### SpringAMQP实现生产者确认

1. 在publisher微服务的application.yml中添加配置:

``````yaml
spring:
  rabbitmq:
    host: 192.168.43.33 # rabbitMQ的ip地址
    port: 5672 # 端口
    username: itcast
    password: 123321
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
``````



配置说明:

- publish-confirm-type: 开启publisher-confirm, 支持两种类型:
  - simple: 同步等待confirm结果, 直到超时
  - correlated: 异步回调, 定义ConfirmCallback, MQ返回结果时会回调这个ConfirmCallback
- publsih-returns: 开启publsih-return功能, 同样是基于callback机制, 不过是定义ReturnCallback
- templae.mandatory: 定义消息路由失败时的策略. true, 则调用ReturnCallback; false: 则直接丢弃消息



2. 每个RabbitTemplate只能配置一个ReturnCallback, 因此需要在项目启动过程中配置:

``````java
@Slf4j
@Configuration
public class CommonConfig implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        /*获取RabbitTemplate对象*/
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        /*配置ReturnCallback*/
        rabbitTemplate.setReturnsCallback(reMsg -> {
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
``````



3. 发送消息, 指定消息ID, 消息ConfirmCallback

``````java
@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage2SimpleQueue() {
        /*1. 准备消息和routingKey*/
        String routingKey = "simple";
        String message = "hello, spring amqp!";
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
        rabbitTemplate.convertAndSend("amq.topic", routingKey, message, correlationData);
    }
}
``````

**SpringAMQP中处理消息确认的几种情况:**

- publisher-confirm:
  - 消息成功发送到exchange, 返回ack
  - 消息发送失败, 没有到达交换机, 返回nack
  - 消息发送过程中出现异常, 没有收到回执
- 消息成功发送到exchange, 但没有路由到queue
  - 调用ReturnCallback



### (3). 消息持久化

MQ默认是内存存储消息, 开启持久化功能可以确保缓存在MQ中的消息不丢失.

1. 交换机持久化

``````java
@Configuration
public class CommonConfig {

    /**
     * Description: simpleDirect 声明持久化交换机
     * @return org.springframework.amqp.core.DirectExchange
     * @author jinhui-huang
     * @Date 2023/11/25
     * */
    @Bean
    public DirectExchange simpleDirect() {
        return new DirectExchange("simple.direct", true, false);
    }

}
``````



2. 队列持久化

``````java
@Configuration
public class CommonConfig {

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

}
``````



3. 消息持久化, SpringAMQP中的消息默认是持久的, 可以通过MessagePropertise中的DeliveryMode来指定的:

``````java
@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testDurableMessage() {
        /*1. 准备消息*/
        Message message = MessageBuilder.withBody("hello, spring".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        /*2. 发送消息*/
        rabbitTemplate.convertAndSend("simple.queue", message);
    }
}
``````

**注意:** 在SpringAMQP中交换机, 消息队列和消息都默认是持久化的



### (4).  消费者消息确认

RabbitMQ支持消费者确认机制, 即: 消费者处理消息后可以向MQ发送ack回执, MQ收到ack回执后才会删除该消息. 而SpringAMQP则允许配置三种确认模式:

- manual: 手动ack, 需要在业务代码结束后, 调用api发送ack.
- auto: 自动ack, 有Spring监测listener代码是否出现异常, 没有异常则返回ack; 抛出异常则返回nack
- none: 关闭ack, MQ假定消费者获取消息后会成功处理, 因此消息投递后立即被删除

配置:

``````yaml
spring:
  rabbitmq:
    host: 192.168.43.33 # rabbitMQ的ip地址
    port: 5672 # 端口
    username: itcast
    password: 123321
    virtual-host: /
    listener:
      simple:
        prefetch: 1
        acknowledge-mode: auto
``````



### (5). 消费失败重试机制

当消费者出现异常后, 消息会不断requeue(重新入队)到队列, 再重新发送给消费者, 然后再次异常, 再次requeue, 无限循环, 导致mq的消息处理飙升, 带来不必要的压力

我们可以利用Spring的retry机制, 在消费者出现异常时利用本地重试, 而不是无限制的requeue到mq队列.

``````yaml
spring:
  rabbitmq:
    host: 192.168.43.33 # rabbitMQ的ip地址
    port: 5672 # 端口
    username: itcast
    password: 123321
    virtual-host: /
    listener:
      simple:
        prefetch: 1
        acknowledge-mode: auto
        retry:
          enabled: true # 开启失败重试机制
          initial-interval: 1000 # 初始的失败等待时间
          multiplier: 3 # 下次失败的等待时长倍数, 下次等待时长 = multiplier * last-interval
          max-attempts: 4 # 最大重试次数
          stateless: true # true 无状态; false 有状态. 如果业务中包含业务, 这里改为false
``````



#### 消费者失败消息处理策略

在开启重试模式后, 重试次数耗尽, 如果消息依然失败, 则需要有MessageRecoverer借口来处理, 它包含三种不同的实现:

- RejectAndDontRequeueRecoverer: 重试耗尽后, 直接reject, 丢弃消息. 默认就是这种方式
- ImmediateRequeueMessageReciverer: 重试耗尽后, 返回nack, 消息重新入队
- RepublishMessageReciverer: 重试耗尽后, 将失败消息投递到指定的交换机

![image-20231125203230631](/home/huian/.config/Typora/typora-user-images/image-20231125203230631.png)

测试RepublishMessageReciverer处理模式:

- 首先, 定义接收失败消息的交换机, 队列及其绑定关系:

``````java
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
}

``````



- 然后, 定义RepublishMessageRecoverer

``````java
@Configuration
public class ErrorMessageConfig {
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

``````

处理结果: 

![image-20231125204508439](/home/huian/.config/Typora/typora-user-images/image-20231125204508439.png)

### (6). 总结(如何确保RabbitMQ消息的可靠性?):

- 开启生产者确认机制, 确保生产者的消息能到达队列
- 开启持久化功能, 确保消息未消费前在队列中不会丢失
- 开启消费者确认机制为auto, 由spring确认消息处理成功后完成ack
- 开启消费者失败重试机制, 并设置MessageRecoverer, 多次重试失败后将消息投递到异常交换机, 交由人工处理



## 三. 死信交换机

### 1. 初识死信交换机

当一个队列中的消息满足下列情况之一时, 可以成为 **死信 (dead letter)**

- 消费者使用basic.reject 或 basic.nack声明消费失败, 并且消息的requeue参数设置为false
- 消息是一个过期消息, 超时无人消费
- 要投递的队列消息堆积满了, 最早的消息可能成为死信

如果该队列配置了dead-letter-exchange属性, 指定了一个交换机, 那么队列中的死信就会投递到这个交换机, 而这个交换机称为死信交换机 (Dead Letter Exchange, 简称DLX). 给队列设置dead-letter-routing-key属性, 设置死信交换机与死信队列的RoutingKey

![image-20231126204439629](/home/huian/.config/Typora/typora-user-images/image-20231126204439629.png)



### 2. TTL

TTL, 也就是Time-To-Live.  如果一个队列中的消息TTL结束仍未消费, 则会变为死信, ttl超时分为两种情况:

- 消息所在队列设置了存活时间
- 消息本身设置了存活时间

![image-20231126205145635](/home/huian/.config/Typora/typora-user-images/image-20231126205145635.png)

声明死信交换机和队列, 基于注解的方式声明:

``````java
@Slf4j
@Component
public class SpringRabbitListener {
	/**
     * Description: listenDlQueue 声明死信交换机并监听
     * @return void
     * @author jinhui-huang
     * @Date 2023/11/26
     * */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "dl.queue", durable = "true"),
            exchange = @Exchange(name = "dl.direct"),
            key = "dl"
    ))
    public void listenDlQueue(String msg) {
        log.info("接收到了dl.queue的未消费延迟消息 ==> " + msg);
    }

}
``````



在给队列设置超时时间, 需要在声明队列的时候绑定死信交换机:

``````java
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
``````



编写测试代码:

``````java
@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

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
                .build();
        /*2. 发送消息*/
        rabbitTemplate.convertAndSend("ttl.direct", "ttl", message);
        /*3. 记录日志*/
        log.info("消息成功发送!");
    }
}
``````



发送消息时, 给消息本身设置超时时间

``````java
@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

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
}
``````



### 3. 延迟队列

利用TTL结合死信交换机, 我们实现了消息发出后, 消费者延迟收到消息的效果. 这种消息模式就称为**延迟队列 (Delay Queue)**模式

延迟队列的使用场景包括:

- 延迟发送短信
- 用户下单, 如果用户在15分钟内未支付, 则自动取消
- 预约工作会议, 20分钟后自动通知所有参会人员



安装延迟队列插件

[rabbitmq安装指南](src/main/resources/RabbitMQ部署指南.md)



**SpringAMQP使用延迟队列插件**

DelayExchange的本质还是官方的三种交换机, 只是添加了延迟功能, 因此使用时只需要声明一个交换机, 交换机的类型可以是任意类型, 然后设定delayed属性为true即可.

代码实现(基于注解的方式):

``````java
@Slf4j
@Component
public class SpringRabbitListener {

	@RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "delay.queue", durable = "true"),
            exchange = @Exchange(name = "delay.direct", delayed = "true"),
            key = "delay"
    ))
    public void listenDelayQueue(String msg) {
        log.info("接收到了delay.queue的延迟消息 ==> " + msg);
    }
}
``````



代码实现(基于java代码的方式):

``````java
@Configuration
public class DelayedMessageConfig {

    @Bean
    public DirectExchange delayedExchange() {
        return ExchangeBuilder
            	.directExchange("delay.direct"); /*指定交换机类型*/
        		.delayed() /*设置delay属性为true*/
                .durable(true)
                .build();
    }

    @Bean
    public Queue delayedQueue() {
        return new Queue("delay.queue");
    }

    @Bean
    public Binding delayedBinding() {
        return BindingBuilder.bind(delayedQueue()).to(delayedExchange()).with("delay");
    }
}
``````



然后向这个delay为true的交换机中发送消息, 一定要给消息添加一个header: x-delay, 值为延迟的时间, 单位为毫秒:

``````java
@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
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
}
``````



## 四. 惰性队列

### 1. 消息堆积问题

当生产着发送消息的速度超过了消费者处理消息的速度, 就会导致队列中的消息堆积, 知道队列存储消息达到上限. 最早接收到的消息, 可能就会称为死信, 会被丢弃, 这就是消息堆积问题.

解决消息堆积有三种思路:

- 增加更多消费者, 提高消费速度
- 在消费者内开启线程池加快消息处理速度
- 扩大队列的容积, 提高堆积上限



### 2. 惰性队列

从RabbitMq的3.6.0版本开始, 就增加了Lazy Queues的概念, 也就是惰性队列.

惰性队列的特征如下:

- 接收到消息后直接存入磁盘而非内存
- 消费者要消费消息时才会从磁盘中读取并加载到内存
- 支持数百万条的消息存储

而要设置一个队列为惰性队列, 只需要在声明队列时, 指定x-queue-mode属性为lazy即可. 可以通过命令行将一个运行中的队列修改为惰性队列:

``````bash
rabbitmqctl set_policy Lazy "^lazy-queue$" '{"queue-mode":"lazy"}' --aply-to queues
``````



**用SpringAMQP声明惰性队列分两种方式:**

![image-20231126223747934](/home/huian/.config/Typora/typora-user-images/image-20231126223747934.png)

java代码:

``````java
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
``````



测试代码:

``````java
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
}
``````



**惰性队列的优点:**

- 基于磁盘存储, 消息上限高
- 没有间歇性的page-out, 性能比较稳定



**惰性队列的缺点:**

- 基于磁盘存储, 消息时效性会降低
- 性能受限于磁盘IO



## 五. MQ集群

### 1. 集群分类

RabbitMQ的底层是基于Erlang语言编写的, 而Erlang又是一个面向并发的语言, 天然支持集群模式. RabbitMQ的集群有两种模式:

- 普通集群: 是一种分布式集群, 将队列分散到集群的各个节点, 从而提高整个集群的并发能力.
- 镜像集群: 是一种主从集群, 普通集群的基础上, 添加了主从备份功能, 提高集群的数据可用性.

镜像集群虽然支持主从, 但主从同步并不是强一致的, 某些情况下可能有数据丢失的风险. 因此在RabbitMQ的3.8版本以后, 推出了新的功能: 仲裁队列来代替镜像集群, 底层采用Raft协议确保主从的数据一致性



### 2. 普通集群



### 3. 镜像集群



### 4. 仲裁队列







