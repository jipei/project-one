package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.common.util.IdWorker;
import com.pinyougou.common.util.RedisLock;
import com.pinyougou.mapper.SeckillGoodsMapper;
import com.pinyougou.mapper.SeckillOrderMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service(interfaceClass = SeckillOrderService.class)
public class SeckillOrderServiceImpl extends BaseServiceImpl<TbSeckillOrder> implements SeckillOrderService {

    //秒杀商品订单在redis中的key的名称
    private static final String SECKILL_ORDERS = "SECKILL_ORDERS";
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Override
    public PageResult search(Integer page, Integer rows, TbSeckillOrder seckillOrder) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbSeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(seckillOrder.get***())){
            criteria.andLike("***", "%" + seckillOrder.get***() + "%");
        }*/

        List<TbSeckillOrder> list = seckillOrderMapper.selectByExample(example);
        PageInfo<TbSeckillOrder> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public String submitOrder(Long seckillId, String username) throws InterruptedException {
        //加分布式锁
        RedisLock redisLock = new RedisLock(redisTemplate);
        if (redisLock.lock(seckillId.toString())){
            //1.判断商品存在并且库存大于0
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(SeckillGoodsServiceImpl.SECKILL_GOODS).get(seckillId);
            if (seckillGoods==null){
                throw new RuntimeException("秒杀商品不存在！");
            }
            if (seckillGoods.getStockCount()==0){
                throw new RuntimeException("商品已经秒杀完!");
            }
            //2.江商品的库存减1
            seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
            if (seckillGoods.getStockCount()>0){
                //2.1 库存大于0；更新redis中商品库存
                redisTemplate.boundHashOps(SeckillGoodsServiceImpl.SECKILL_GOODS).put(seckillId,seckillGoods);
            }else {
                //2.2 库存等于0；更新秒杀商品到mysql,删除redis中的秒杀商品
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);

                redisTemplate.boundHashOps(SeckillGoodsServiceImpl.SECKILL_GOODS).delete(seckillId);
            }
            //释放分布式锁
            redisLock.unlock(seckillId.toString());

            //3.创建生成秒杀订单，并存入redis中
            TbSeckillOrder seckillOrder = new TbSeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            //未支付
            seckillOrder.setStatus("0");
            //秒杀价
            seckillOrder.setMoney(seckillGoods.getCostPrice());
            seckillOrder.setSellerId(seckillGoods.getSellerId());
            seckillOrder.setUserId(username);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setSeckillId(seckillId);

            redisTemplate.boundHashOps(SECKILL_ORDERS).put(seckillOrder.getId().toString(), seckillOrder);
            //
            //4. 返回订单编号
            return seckillOrder.getId().toString();
        }
        return null;
    }


public TbSeckillOrder findSeckillOrderInRedisByOutTradeNo(String outTradeNo) {
        return (TbSeckillOrder) redisTemplate.boundHashOps(SECKILL_ORDERS).get(outTradeNo);
    }

    @Override
    public void saveSeckillOrderInRedisToDb(String outTradeNo, String transaction_id) {
        //1.获取redis中的订单
        TbSeckillOrder seckillOrder = findSeckillOrderInRedisByOutTradeNo(outTradeNo);
        //2.修改订单的支付状态和其他信息；
        seckillOrder.setPayTime(new Date());
        seckillOrder.setStatus("1");
        seckillOrder.setTransactionId(transaction_id);
        //3.保存到Mysql
        seckillOrderMapper.insertSelective(seckillOrder);
        //4.将订单从redis中删除
        redisTemplate.boundHashOps(SECKILL_ORDERS).delete(outTradeNo);
    }




    @Override
    public void removeSeckillOrderInRedis(String outTradeNo) throws InterruptedException {
        //1.获取订单判断订单的存在；
        TbSeckillOrder seckillOrder = findSeckillOrderInRedisByOutTradeNo(outTradeNo);
        //加分布式锁
        RedisLock redisLock = new RedisLock(redisTemplate);
        if (redisLock.lock(seckillOrder.getSeckillId().toString())){
            //2.根据秒杀商品Id获取在redis中的商品；如果不存在则到mysql中查询该商品并更新库存后更新到redis中；
            TbSeckillGoods seckillGoods= (TbSeckillGoods) redisTemplate.boundHashOps(SeckillGoodsServiceImpl.SECKILL_GOODS).get(seckillOrder.getSeckillId());
            if (seckillGoods==null){
                seckillGoods=seckillGoodsMapper.selectByPrimaryKey(seckillOrder.getSeckillId());
            }
            //加回剩余库存
            seckillGoods.setStockCount(seckillGoods.getStockCount()+1);
            //更新回redis
            redisTemplate.boundHashOps(SeckillGoodsServiceImpl.SECKILL_GOODS).put(seckillGoods.getId(),seckillGoods);

            //释放分布式锁
            redisLock.unlock(seckillOrder.getSeckillId().toString());
            //3.删除在redis中的订单
            redisTemplate.boundHashOps(SECKILL_ORDERS).delete(outTradeNo);
        }
    }
}
