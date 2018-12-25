package com.pinyougou.search.service;

import com.pinyougou.pojo.TbItem;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {

    /**
     * 根据搜索关键字搜索商品列表
     * @param searchMap 搜索条件
     * @return 搜索结果
     */
    Map<String, Object> search(Map<String, Object> searchMap);

    /**
     * 批量导入商品列表到solr索引库
     * @param itemList 商品列表
     */
    void importItemList(List<TbItem> itemList);

    /**
     * 根据goodsId商品id集合个删除其对应在solr中商品数据
     * @param goodsIdList goodsId商品id集合
     */
    void deleteItemByGoodsIdList(List<Long> goodsIdList);
}
