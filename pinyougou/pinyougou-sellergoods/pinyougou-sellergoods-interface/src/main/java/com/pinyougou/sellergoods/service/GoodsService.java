package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.Goods;
import com.pinyougou.vo.PageResult;

import java.util.List;

public interface GoodsService extends BaseService<TbGoods> {

    PageResult search(Integer page, Integer rows, TbGoods goods);

    void addGoods(Goods goods);

    Goods findGoodsById(Long id);

    void updateGoods(Goods goods);

    void updateStatus(Long[] ids, String status);

    void deleteGoodsByIds(Long[] ids);

    void updateMarketable(Long[] ids, String marketable);

    /**
     * 根据商品Spu id 集合和状态查询这些商品对应的sku商品列表
     * @param ids 商品SPU id集合
     * @param status sku商品状态
     */
    List<TbItem> findItemListByGoodsIdsAndStatus(Long[] ids, String status);

    /**
     * 根据商品id查询商品基本、描述、启用的sku列表
     * @param goodsId 商品id
     * @param itemStatus 是否启用
     * @return 商品
     */
    Goods findGoodsByGoodsIdAndStatus(Long goodsId, String itemStatus);
}