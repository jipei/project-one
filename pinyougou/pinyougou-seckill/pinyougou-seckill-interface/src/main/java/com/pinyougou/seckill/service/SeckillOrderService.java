package com.pinyougou.seckill.service;

import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

public interface SeckillOrderService extends BaseService<TbSeckillOrder> {

    PageResult search(Integer page, Integer rows, TbSeckillOrder seckillOrder);

    /**
     * 根据秒杀商品id生成秒杀订单
     * @param seckillId 秒杀商品id
     * @return 操作结果
     */
    String submitOrder(Long seckillId, String username) throws InterruptedException;

    /**
     * 保存秒杀订单到mysql数据库中
     * @param outTradeNo 订单号
     * @param transaction_id 微信订单号
     */
    void saveSeckillOrderInRedisToDb(String outTradeNo, String transaction_id);

    /**
     * 将redis中的订单删除并更新秒杀商品剩余库存
     * @param outTradeNo 订单号
     */
    void removeSeckillOrderInRedis(String outTradeNo) throws InterruptedException;

    /**
     * 根据订单号获取秒杀订单
     * @param outTradeNo 订单号
     * @return 秒杀订单
     */
    TbSeckillOrder findSeckillOrderInRedisByOutTradeNo(String outTradeNo);
}