package com.pinyougou.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.common.util.IdWorker;
import com.pinyougou.mapper.OrderItemMapper;
import com.pinyougou.mapper.OrderMapper;
import com.pinyougou.mapper.PayLogMapper;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.Cart;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl extends BaseServiceImpl<TbOrder> implements OrderService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private PayLogMapper payLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    //所有用户的购物车数据对应在redis中的key名称
    private static final String CART_LIST = "CART_LIST";

    @Override
    public PageResult search(Integer page, Integer rows, TbOrder order) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbOrder.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(order.get***())){
            criteria.andLike("***", "%" + order.get***() + "%");
        }*/

        List<TbOrder> list = orderMapper.selectByExample(example);
        PageInfo<TbOrder> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public String addOrder(TbOrder order) {
        String outTradeNo="";

        //1.获取到再redis中当前用户的购物车列表
        List<Cart> cartList= (List<Cart>) redisTemplate.boundHashOps(CART_LIST).get(order.getUserId());

        //2.遍历购物车列表，每一个cart就是一个订单
        if (cartList !=null && cartList.size()>0){
            //本次支付总金额
            double totalFee=0.0;
            //订单的支付总金额
            double payment=0.0;
            //本次交易的所有订单
            String orderIds="";
            for (Cart cart : cartList) {
                TbOrder tbOrder = new TbOrder();
                //3.再遍历每个Cart中的orderItemList订单商品列表并保存每一个订单商品OrderItem;
                tbOrder.setOrderId(idWorker.nextId());
                tbOrder.setSourceType(order.getSourceType());
                tbOrder.setUserId(order.getUserId());
                //1.未支付，2.已支付 3.未发货 4.已发货 5.交易成功 6.交易关闭 7.待评价
                tbOrder.setStatus("1");
                tbOrder.setPaymentType(order.getPaymentType());
                tbOrder.setReceiver(order.getReceiver());
                tbOrder.setReceiverAreaName(order.getReceiverMobile());
                tbOrder.setReceiverMobile(order.getReceiverMobile());
                tbOrder.setCreateTime(new Date());
                tbOrder.setUpdateTime(tbOrder.getCreateTime());
                tbOrder.setSellerId(cart.getSellerId());

                for (TbOrderItem orderItem : cart.getOrderItemList()) {
                    orderItem.setId(idWorker.nextId());
                    orderItem.setOrderId(tbOrder.getOrderId());

                    //累计订单的总金额
                    payment+=orderItem.getTotalFee().doubleValue();

                    //保存订单明细
                    orderItemMapper.insertSelective(orderItem);
                }

                //本笔订单的支付总金额=订单的所有商品的总价之和
                tbOrder.setPayment(new BigDecimal(payment));

                //累计本次支付的总金额
                totalFee+=payment;

                //累加订单的id
                if (orderIds.length()>0){
                    orderIds+=","+tbOrder.getOrderId();
                }else {
                    orderIds=tbOrder.getOrderId().toString();
                }
                orderMapper.insertSelective(tbOrder);
            }

            //4.如果是微信付款的话那么保存支付日志信息到数据库中；
            if ("1".equals(order.getPaymentType())){
                TbPayLog payLog = new TbPayLog();
                outTradeNo = idWorker.nextId()+"";
                payLog.setOutTradeNo(outTradeNo);
                //未支付
                payLog.setTradeState("0");
                payLog.setCreateTime(new Date());
                payLog.setUserId(order.getUserId());
                payLog.setPayType(order.getPaymentType());
                //本次要支付的总金额=所有订单的总金额之和
                //微信那边要求支付的总金额必须为长整形，单位精确到分
                payLog.setTotalFee((long)(totalFee*100));

                //本次支付的所有订单，使用，分隔
                payLog.setOrderList(orderIds);

                payLogMapper.insertSelective(payLog);
            }
            //5.情况redis中用户对应的购物车列表
            redisTemplate.boundHashOps(CART_LIST).delete(order.getUserId());
        }
        //6.如果是微信付款则返回支付日志id否则返回空字符串
        return outTradeNo;
    }

    @Override
    public TbPayLog findPayLogByOutTradeNo(String outTradeNo) {
        return payLogMapper.selectByPrimaryKey(outTradeNo);
    }

    @Override
    public void updateOrderStatus(String outTradeNo, String transaction_id) {
        //1.支付日志信息的支付状态更新为已支付1；
        TbPayLog payLog = findPayLogByOutTradeNo(outTradeNo);
        payLog.setPayTime(new Date());
        payLog.setTradeState("1");
        payLog.setTransactionId(transaction_id);
        payLogMapper.updateByPrimaryKeySelective(payLog);

        //2.本次支付对应的所有订单的状态修改为已支付2
        //要跟新的数据
        TbOrder order = new TbOrder();
        order.setUpdateTime(new Date());
        //已支付
        order.setStatus("2");

        //更新条件
        //获取本次的所有订单id数组
        String[] orderIds = payLog.getOrderList().split(",");
        Example example = new Example(TbOrder.class);
        example.createCriteria().andIn("orderId", Arrays.asList(orderIds));

        orderMapper.updateByExampleSelective(order,example);
    }


}
