package com.pinyougou.pay.service;

import java.util.Map;

public interface WeixinPayService {

    /**
     * 根据订单标号查询订单的支付状态
     * @param outTradeNo 交易订单号
     * @return 支付信息
     */
    Map<String, String> queryPayStatus(String outTradeNo);

    /**
     *到支付系统获取支付二维码链接地址等信息
     * @param outTradeNo 交易订单号
     * @param totalFee 支付金额
     * @return 操作结果（二维码链接地址code_url、交易订单号、本次支付的总金额totalFee、result_code业务结果（SUCCESS表示成功））
     */
    Map<String, String> createNative(String outTradeNo, String totalFee);

    /**
     * 根据订单号关闭在微信支付系统的订单
     * @param outTradeNo 订单号
     * @return 操作结果
     */
    Map<String, String> closeOrder(String outTradeNo);
}
