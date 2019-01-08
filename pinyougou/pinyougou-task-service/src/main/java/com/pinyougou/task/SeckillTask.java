package com.pinyougou.task;

import com.pinyougou.mapper.SeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillTask {

    //秒杀商品列表在redis中key的名称
    public static final String SECKILL_GOODS = "SECKILL_GOODS";

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 将在redis中结束时间小于等于当前时间的秒杀商品需要从redis中移除并更新到mysql数据库中。
     * 删除秒杀商品
     */
    @Scheduled(cron = "0/2 * * * * ?")
    public void removeSeckillGoods() {
        //1、获取redis中的秒杀商品列表；
        List<TbSeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(SECKILL_GOODS).values();

        //2、遍历每个商品的结束时间，如果小于等于当前时间则需要移除并更新到mysql
        if (seckillGoodsList != null && seckillGoodsList.size() > 0) {
            for (TbSeckillGoods seckillGoods : seckillGoodsList) {
                System.out.println("时间差\t:---->"+(seckillGoods.getEndTime().getTime()-new Date().getTime()));
                if (seckillGoods.getEndTime().getTime() <= System.currentTimeMillis()) {
                    //更新到mysql
                    seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                    //移除出redis
                    redisTemplate.boundHashOps(SECKILL_GOODS).delete(seckillGoods.getId());
                    System.out.println("秒杀商品id为 " + seckillGoods.getId() + " 的商品已经被移除...");
                }
            }
        }
    }

    /**
     * 更新秒杀商品
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void refreshSeckillGoods() {
        //在redis中的那些秒杀商品id集合
        Set set = redisTemplate.boundHashOps(SECKILL_GOODS).keys();
        List<Long> seckillGoodsIds = new ArrayList<>(set);

        //1、查询数据
        //不在redis中的那些库存大于0，已审核，开始时间小于等于当前时间，结束时间大于当前时间的秒杀商品。
        //对应的数据库语句：select * from tb_seckill_goods where status='1'
        // and stock_count>0 and start_time<=? and end_time>? and id not in(redis中的秒杀商品id集合List)
        Example example = new Example(TbSeckillGoods.class);

        Example.Criteria criteria = example.createCriteria();

        //已审核
        criteria.andEqualTo("status", "1");
        //库存大于0
        criteria.andGreaterThan("stockCount", 0);

        //开始时间小于等于当前时间
        criteria.andLessThanOrEqualTo("startTime", new Date());
        //结束时间大于当前时间
        criteria.andGreaterThan("endTime", new Date());
        //排除在redis中的数据
        criteria.andNotIn("id", seckillGoodsIds);

        //查询
        List<TbSeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);
        //2、更新秒杀商品到redis
        if (seckillGoodsList != null && seckillGoodsList.size() > 0) {
            for (TbSeckillGoods seckillGoods : seckillGoodsList) {
                redisTemplate.boundHashOps(SECKILL_GOODS).put(seckillGoods.getId(), seckillGoods);
            }
            System.out.println("更新了" + seckillGoodsList.size() + " 个秒杀商品到缓存中... ");
        }
    }
}
