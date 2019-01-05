package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/pay")
@RestController
public class PayController {

    @Reference
    private OrderService orderService;

    @Reference
    private WeixinPayService weixinPayService;

    /**
     * 根据订单标号查询订单的支付状态
     * @param outTradeNo 订单号
     * @return 操作结果
     */
    @GetMapping("/queryPayStatus")
    private Result queryPayStatus(String outTradeNo){
        Result result=Result.fail("查询支付状态失败！");

        try {
            //3分钟内查询
            int count=0;
            while (true){
                //1.编写处理器方法无限循环去查询支付系统中订单的支付状态
                Map<String,String> map=weixinPayService.queryPayStatus(outTradeNo);
                if (map==null){
                    //2.如果查询失败则退出循环
                    break;
                }

                if ("SUCCESS".equals(map.get("trade_state"))){
                    //3.如果查询订单已经知道，调用页数方法更新订单状态，返回查询成功。
                    orderService.updateOrderStatus(outTradeNo,map.get("transaction_id"));
                    result=Result.ok("查询支付状态成功");
                    break;
                }
                count++;
                if (count>60){
                     result=Result.fail("支付超时");
                     break;
                }
                //没隔3秒
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取支付二维码链接地址等信息
     * @param outTradeNo 订单号
     * @return 支付二维码链接地址等信息
     */
    @GetMapping("createNative")
    public Map<String,String> createNative(String outTradeNo){

        try {
            //1.根据支付日志Id获取支付日志
            TbPayLog payLog = orderService.findPayLogByOutTradeNo(outTradeNo);
            //本次要支付的总金额
            String totalFee =payLog.getTotalFee()+"";

            //2.调用支付接口获取信息
            return weixinPayService.createNative(outTradeNo,totalFee);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }
}
