package com.pinyougou.seckill.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
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
    private SeckillOrderService seckillOrderService;
    @Reference
    private WeixinPayService weixinPayService;

    /**
     * 根据订单编号查询订单的支付状态
     * @param outTradeNo 订单号
     * @return 操作结果
     */
    @GetMapping("/queryPayStatus")
    public Result queryPayStatus(String outTradeNo){
        Result result=Result.fail("查询支付状态失败！");

        try {
            //1分钟内查询
            int count=0;
            while (true){
                //1.编写处理器方法无限循环去查询支付系统中订单的支付状态；
                Map<String,String> map=weixinPayService.queryPayStatus(outTradeNo);
                if (map==null){
                    //2.如果查询失败则退出循环
                    break;
                }

                if ("SUCCESS".equals(map.get("trade_state"))){
                    //3.如果查询订单已经支付，调用业务方法更新订单状态，返回查询成功。
                    seckillOrderService.saveSeckillOrderInRedisToDb(outTradeNo,map.get("transaction_id"));
                    result=Result.ok("查询支付状态成");
                    break;
                }
                count++;
                if (count>20){
                    //1.关闭微信中对应的订单
                    Map<String,String> resultMap =weixinPayService.closeOrder(outTradeNo);

                    if (resultMap !=null && "ORDERPAID".equals(resultMap.get("err_code"))){
                        //关闭订单的过程中被支付了；则需要更新订单到Mysql
                        seckillOrderService.saveSeckillOrderInRedisToDb(outTradeNo,map.get("transaction_id"));
                        result=Result.ok("查询支付状态成功");
                        break;
                    }
                    //关闭订单成功；将redis中的订单删除并更新秒杀商品剩余库存
                    seckillOrderService.removeSeckillOrderInRedis(outTradeNo);
                    result=Result.fail("支付超时");
                    break;
                }
                //每隔3秒
                Thread.sleep(300);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/createNative")
    public Map<String,String> createNative(String outTradeNo){
        //1.根据订单标号获取秒杀订单
        TbSeckillOrder seckillOrder =seckillOrderService.findSeckillOrderInRedisByOutTradeNo(outTradeNo);
        //本次要支付的总金额
        String totalFee = (long)(seckillOrder.getMoney().doubleValue() * 100) + "";

        //2.调用支付接口获取信息
        try {
            return weixinPayService.createNative(outTradeNo,totalFee);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

}
