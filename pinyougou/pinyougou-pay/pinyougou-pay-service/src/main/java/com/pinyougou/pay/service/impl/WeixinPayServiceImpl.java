package com.pinyougou.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.WXPayUtil;
import com.pinyougou.common.util.HttpClient;
import com.pinyougou.pay.service.WeixinPayService;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service(interfaceClass = WeixinPayService.class)
public class WeixinPayServiceImpl implements WeixinPayService {

    @Value("${appid}")
    private String appid;
    @Value("${partner}")
    private String partner;
    @Value("${partnerkey}")
    private String partnerkey;
    @Value("${notifyurl}")
    private String notifyurl;

    @Override
    public Map<String, String> createNative(String outTradeNo, String totalFee) {
        Map<String,String> returnMap=new HashMap<>();

        try {
            //1.封装微信需要的参数：
            Map<String,String> param =new HashMap<>();
            //公众账号ID
            param.put("appid",appid);
            //商户号
            param.put("mch_id",partner);
            //随机字符串；可以利用微信提供的工具类生成
            param.put("nonce_str", WXPayUtil.generateNonceStr());
            //签名；可以在发送的时候利用工具类生成
            //param.put("sign", null);
            //商品描述
            param.put("body","ee95品优购");
            //商户订单号
            param.put("out_trade_no",outTradeNo);
            //标价金额
            param.put("total_fee",totalFee);
            //终端Ip
            param.put("spbill_create_ip","127.0.0.1");
            //通知地址
            param.put("notify_url",notifyurl);
            //交易类型
            param.put("trade_type","NATIVE");

            //2.发送请求到微信支付系统
            String signedXml = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("发送到微信支付系统 统一下单 的数据为："+signedXml);

            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setXmlParam(signedXml);
            httpClient.isHttps();
            httpClient.post();

            //3.处理返回结果
            String content = httpClient.getContent();
            System.out.println("发送到微信支付系统 统一下单 的返回数据为："+content);

            Map<String,String> resultMap=WXPayUtil.xmlToMap(content);

            returnMap.put("outTradeNo",outTradeNo);
            returnMap.put("totalFee",totalFee);
            returnMap.put("result_code",resultMap.get("result_code"));
            returnMap.put("code_url",resultMap.get("code_url"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnMap;
    }

    @Override
    public Map<String, String> queryPayStatus(String outTradeNo) {

        try {
            //1.封装微信需要的参数
            Map<String,String> param=new HashMap<>();
            //公众账号ID
            param.put("appid",appid);
            //商户号
            param.put("mch_id",partner);
            //随机字符串；可以利用微信提供 的工具类生成
            param.put("nonce_str",WXPayUtil.generateNonceStr());
            //签名；可以在发送的时候利用工具类生成
            //param.put("sign",null);
            //商户订单号
            param.put("out_trade_no",outTradeNo);

            //2.发送请求到微信支付系统
            String signedXml = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("发送到微信支付系统 查询订单 的数据为："+signedXml);

            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            httpClient.setXmlParam(signedXml);
            httpClient.isHttps();
            httpClient.post();

            //3.处理返回结果
            String content = httpClient.getContent();
            System.out.println("发送到微信支付系统 查询订单 的返回数据为："+ content);

            return WXPayUtil.xmlToMap(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
