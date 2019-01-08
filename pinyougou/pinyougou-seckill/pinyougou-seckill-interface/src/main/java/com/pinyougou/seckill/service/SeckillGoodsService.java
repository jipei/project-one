package com.pinyougou.seckill.service;

import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

import java.util.List;

public interface SeckillGoodsService extends BaseService<TbSeckillGoods> {

    PageResult search(Integer page, Integer rows, TbSeckillGoods seckillGoods);

    /**
     * 查询可以秒杀的商品列表
     * @return 秒杀商品列表
     */
    List<TbSeckillGoods> findList();

    /**
     * 根据秒杀商品id从redis中获取该商品
     * @param id 秒杀商品id
     * @return 秒杀商品
     */
    TbSeckillGoods findOneInRedisBySeckillId(Long id);
}