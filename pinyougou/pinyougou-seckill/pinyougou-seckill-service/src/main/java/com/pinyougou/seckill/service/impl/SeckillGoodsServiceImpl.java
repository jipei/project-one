package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.SeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.seckill.service.SeckillGoodsService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service(interfaceClass = SeckillGoodsService.class)
public class SeckillGoodsServiceImpl extends BaseServiceImpl<TbSeckillGoods> implements SeckillGoodsService {

    public static final String SECKILL_GOODS = "SECKILL_GOODS";
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageResult search(Integer page, Integer rows, TbSeckillGoods seckillGoods) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbSeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(seckillGoods.get***())){
            criteria.andLike("***", "%" + seckillGoods.get***() + "%");
        }*/

        List<TbSeckillGoods> list = seckillGoodsMapper.selectByExample(example);
        PageInfo<TbSeckillGoods> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public List<TbSeckillGoods> findList() {
        List<TbSeckillGoods> seckillGoodsList=null;
        try {
            //1.1从redis中获取到秒杀商品列表，如果存在则直接返回；
            seckillGoodsList=redisTemplate.boundHashOps(SECKILL_GOODS).values();
            if (seckillGoodsList!=null && seckillGoodsList.size()>0){
                return seckillGoodsList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //1.2 到数据库中查询秒杀商品列表
        //剩余库存大于0，已审核，开始时间小于等于当前时间，结束时间大于当前时间；并且按照开始时间升序排序
        Example example = new Example(TbSeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();

        //已审核
        criteria.andEqualTo("status","1");
        //库存大于0
        criteria.andGreaterThan("stockCount",0);

        //开始时间小于等于当前时间
        criteria.andLessThanOrEqualTo("startTime",new Date());
        //结束时间大于当前时间
        criteria.andGreaterThan("endTime",new Date());
        //开始时间升序排序
        example.orderBy("startTime");

        //查询
        seckillGoodsList=seckillGoodsMapper.selectByExample(example);

        try {
            //1.3 遍历秒杀商品列表逐个逐个地将秒杀商品存入到redis中（以hash结构存储）
            if (seckillGoodsList !=null && seckillGoodsList.size()>0){
                for (TbSeckillGoods seckillGoods : seckillGoodsList) {
                    redisTemplate.boundHashOps(SECKILL_GOODS).put(seckillGoods.getId(),seckillGoods);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return seckillGoodsList;
    }

    @Override
    public TbSeckillGoods findOneInRedisBySeckillId(Long id){
        return (TbSeckillGoods) redisTemplate.boundHashOps(SECKILL_GOODS).get(id);
    }
}
