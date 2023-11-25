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
  - 消息未投递到交换机, 返回ack
- publisher-return, 发送者回执
  - 消息投递到交换机了, 但是没有路由到队列. 返回ACK, 及路由失败原因.

**注意:** 确认机制发送消息时, 需要给每个消息设置一个全局唯一id, 以区分不同消息, 避免ack冲突



#### SpringAMQP实现生产者确认

1. 在publisher微服务的application.yml中添加配置:

``````yaml
``````



配置说明:

- publish-confirm-type: 开启publisher-confirm, 支持两种类型:
  - simple: 同步等待confirm结果, 直到超时
  - correlated: 异步回调, 定义ConfirmCallback, MQ返回结果时会回调这个ConfirmCallback
- publsih-returns: 开启publsih-return功能, 同样是基于callback机制, 不过是定义ReturnCallback
- templae.mandatory: 定义消息路由失败时的策略. true, 则调用ReturnCallback; false: 则直接丢弃消息



2. 每个RabbitTemplate只能配置一个ReturnCallback, 因此需要在项目启动过程中配置:

``````java
``````





### (3). 消息持久化





### (4).  消费者消息确认





### (5). 消费失败重试机制





## 三. 死信交换机





## 四. 惰性队列





## 五. MQ集群



