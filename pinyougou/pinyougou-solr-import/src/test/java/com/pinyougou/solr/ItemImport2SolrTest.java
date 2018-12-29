package com.pinyougou.solr;

import com.alibaba.fastjson.JSONObject;
import com.pinyougou.mapper.ItemMapper;
import com.pinyougou.pojo.TbItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;

/**
 * 将已启用的sku商品列表导入到solr
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/applicationContext-*.xml")
public class ItemImport2SolrTest {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private ItemMapper itemMapper;

    @Test
    public void test(){
        //1. 查询已启用的sku列表；
        TbItem param = new TbItem();
        param.setStatus("1");

        List<TbItem> itemList = itemMapper.select(param);
        //2. 遍历每一个sku，将spec内容转换为一个map设置到specMap属性中；
        for (TbItem tbItem : itemList) {
            Map map = JSONObject.parseObject(tbItem.getSpec(), Map.class);
            tbItem.setSpecMap(map);
        }
        //3. 利用solrTemplate将商品列表保存到solr中
        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
    }
}
