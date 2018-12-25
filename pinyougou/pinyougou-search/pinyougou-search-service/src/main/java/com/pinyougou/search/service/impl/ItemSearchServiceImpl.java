package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Override
    public Map<String, Object> search(Map<String, Object> searchMap) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //处理搜索关键字中的空格
        if (!StringUtils.isEmpty(searchMap.get("keywords"))) {
            searchMap.put("keywords", searchMap.get("keywords").toString().replaceAll(" ", ""));
        }

        //创建查询对象
        //SimpleQuery query = new SimpleQuery();
        //创建高亮的查询对象
        SimpleHighlightQuery query = new SimpleHighlightQuery();

        //设置查询条件 is 会对搜索关键字进行分词
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //设置高亮的域名和起始、结束标签
        HighlightOptions highlightOptions = new HighlightOptions();
        //高亮域名
        highlightOptions.addField("item_title");
        //设置高亮的起始标签
        highlightOptions.setSimplePrefix("<font style='color:red'>");
        //设置高亮的结束标签
        highlightOptions.setSimplePostfix("</font>");

        query.setHighlightOptions(highlightOptions);

        //1. 根据商品分类进行商品分类过滤条件的设置；
        if(!StringUtils.isEmpty(searchMap.get("category"))){
            //创建过滤查询条件对象
            Criteria categoryCriteria = new Criteria("item_category").is(searchMap.get("category"));
            SimpleFilterQuery categoryFilterQuery = new SimpleFilterQuery(categoryCriteria);
            query.addFilterQuery(categoryFilterQuery);
        }
        //2. 根据品牌进行品牌过滤条件的设置；
        if(!StringUtils.isEmpty(searchMap.get("brand"))){
            //创建过滤查询条件对象
            Criteria brandCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            SimpleFilterQuery brandFilterQuery = new SimpleFilterQuery(brandCriteria);
            query.addFilterQuery(brandFilterQuery);
        }

        //3. 根据规格进行规格过滤条件的设置；
        if(searchMap.get("spec") != null){

            //创建过滤查询条件对象
            Map<String, String> specMap = (Map<String, String>) searchMap.get("spec");
            Set<Map.Entry<String, String>> entries = specMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                Criteria specCriteria = new Criteria("item_spec_" + entry.getKey()).is(entry.getValue());
                SimpleFilterQuery specFilterQuery = new SimpleFilterQuery(specCriteria);
                query.addFilterQuery(specFilterQuery);
            }

        }
        //4. 根据价格进行价格过滤条件的设置；
        if(!StringUtils.isEmpty(searchMap.get("price"))){
            //价格的值可以能为：0-500 或者 3000-*
            String[] prices = searchMap.get("price").toString().split("-");

            //创建过滤查询条件对象
            Criteria startCriteria = new Criteria("item_price").greaterThanEqual(prices[0]);
            SimpleFilterQuery startFilterQuery = new SimpleFilterQuery(startCriteria);
            query.addFilterQuery(startFilterQuery);

            if (!"*".equals(prices[1])) {
                Criteria endCriteria = new Criteria("item_price").lessThanEqual(prices[1]);
                SimpleFilterQuery endFilterQuery = new SimpleFilterQuery(endCriteria);
                query.addFilterQuery(endFilterQuery);
            }
        }


        //设置分页信息
        //页号
        int pageNo = 1;
        if(!StringUtils.isEmpty(searchMap.get("pageNo"))){
            pageNo = Integer.parseInt(searchMap.get("pageNo").toString());
        }
        //页大小
        int pageSize = 20;
        if(!StringUtils.isEmpty(searchMap.get("pageSize"))){
            pageSize = Integer.parseInt(searchMap.get("pageSize").toString());
        }
        query.setOffset((pageNo - 1) * pageSize);
        query.setRows(pageSize);

        //处理排序
        if(!StringUtils.isEmpty(searchMap.get("sortField"))&&!StringUtils.isEmpty(searchMap.get("sort"))){
            //排序的域
            String sortField = searchMap.get("sortField").toString();
            //排序的顺序，ASC 或者 DESC
            String sortOrder = searchMap.get("sort").toString();
            //参数1：排序的顺序，参数2：排序的域名
            Sort sort = new Sort("DESC".equals(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, "item_" + sortField);
            query.addSort(sort);
        }


        //查询
        //ScoredPage<TbItem> scoredPage = solrTemplate.queryForPage(query, TbItem.class);
        HighlightPage<TbItem> highlightPage = solrTemplate.queryForHighlightPage(query, TbItem.class);

        //处理高亮的返回结果
        List<HighlightEntry<TbItem>> highlighted = highlightPage.getHighlighted();
        if (highlighted != null && highlighted.size() > 0) {
            for (HighlightEntry<TbItem> entry : highlighted) {
                List<HighlightEntry.Highlight> highlights = entry.getHighlights();
                if (highlights != null && highlights.size() > 0
                        && highlights.get(0).getSnipplets() != null && highlights.get(0).getSnipplets().size() > 0) {
                    //第一个get(0)为获取第一个域，第二个get(0)获取该域第一个高亮的字符串
                    String title = highlights.get(0).getSnipplets().get(0).toString();
                    entry.getEntity().setTitle(title);
                }
            }
        }


        //记录列表
        resultMap.put("rows", highlightPage.getContent());
        //总页数
        resultMap.put("totalPages", highlightPage.getTotalPages());
        //总记录数
        resultMap.put("total", highlightPage.getTotalElements());
        return resultMap;
    }

    @Override
    public void importItemList(List<TbItem> itemList) {
        if (itemList != null && itemList.size() > 0) {
            //1. 遍历每一个sku，将spec内容转换为一个map设置到specMap属性中；
            for (TbItem tbItem : itemList) {
                Map map = JSONObject.parseObject(tbItem.getSpec(), Map.class);
                tbItem.setSpecMap(map);
            }
            //2. 利用solrTemplate将商品列表保存到solr中
            solrTemplate.saveBeans(itemList);
            solrTemplate.commit();
        }
    }

    @Override
    public void deleteItemByGoodsIdList(List<Long> goodsIdList) {
        SimpleQuery query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(goodsIdList);
        query.addCriteria(criteria);

        solrTemplate.delete(query);
        solrTemplate.commit();
    }


}
