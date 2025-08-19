package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constant.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderDelayMessageListener {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private PayClient payClient;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = MQConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void listenDelayOrderMessage(Long orderId){
        //1.获取订单信息
        Order order = orderService.getById(orderId);
        //2.创建支付单,判断是否已经支付
        if(order == null || order.getStatus() == 1){
            //订单不存在或者订单状态不是未付款，直接返回
            return;
        }
        //3.没有的话去支付服务查询到底是否支付
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);
        //4.订单支付成功，修改订单状态
        if(payOrder != null && payOrder.getStatus() == 3){
            orderService.markOrderPaySuccess(orderId);
        }else{
            orderService.cancelOrder(orderId);
        }
        //5.订单支付失败，关闭订单
    }

}
