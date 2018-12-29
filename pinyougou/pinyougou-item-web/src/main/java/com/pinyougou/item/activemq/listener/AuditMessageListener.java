package com.pinyougou.item.activemq.listener;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.sellergoods.service.GoodsService;
import com.pinyougou.sellergoods.service.ItemCatService;
import com.pinyougou.vo.Goods;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.FileWriter;

import java.util.HashMap;

/**
 * 根据商品spu id 数组生成商品详情静态页面
 */
public class AuditMessageListener extends AbstractAdaptableMessageListener {

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Value("${ITEM_HTML_PATH}")
    private String ITEM_HTML_PATH;

    @Reference
    private GoodsService goodsService;

    @Reference
    private ItemCatService itemCatService;

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        //1.接受并转换消息
        ObjectMessage objectMessage= (ObjectMessage) message;
        Long[] goodsIds= (Long[]) objectMessage.getObject();

        //2.遍历数组并生成商品的静态页面
        if (goodsIds!=null&&goodsIds.length>0){
            for (Long goodsId : goodsIds) {
                genHtml(goodsId);
            }
        }
    }

    private void genHtml(Long goodsId) {
        try {
            //获取模板
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            Template template = configuration.getTemplate("item.ftl");

            //获取数据
            HashMap<String, Object> dataModel = new HashMap<>();
            //据商品的spu id 查询商品信息（基本、描述、已启用的sku列表）
            Goods goods = goodsService.findGoodsByGoodsIdAndStatus(goodsId, "1");

            //goods 商品基本信息
            dataModel.put("goods",goods.getGoods());
            //goodsDesc 商品描述信息
            dataModel.put("goodsDesc",goods.getGoodsDesc());
            //itemList 商品sku列表
            dataModel.put("itemList",goods.getItemList());
            //itemList 1级商品分类中文名称
            TbItemCat itemCat1 = itemCatService.findOne(goods.getGoods().getCategory1Id());
            dataModel.put("itemCat1",itemCat1.getName());
            //itemCat2 2级商品分类中文名称
            TbItemCat itemCat2 = itemCatService.findOne(goods.getGoods().getCategory2Id());
            dataModel.put("itemCat2",itemCat2.getName());
            //itemCat3 3级商品分类中文名称
            TbItemCat itemCat3 = itemCatService.findOne(goods.getGoods().getCategory3Id());
            dataModel.put("itemCat3",itemCat3.getName());

            //指定静态文件存放的路径
            String fileName = ITEM_HTML_PATH + goodsId + ".html";
            FileWriter fileWriter = new FileWriter(fileName);
            //输出
            template.process(dataModel,fileWriter);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
