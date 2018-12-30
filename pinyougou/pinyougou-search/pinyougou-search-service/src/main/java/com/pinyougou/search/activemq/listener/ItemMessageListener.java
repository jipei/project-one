package com.pinyougou.search.activemq.listener;

import com.alibaba.fastjson.JSONArray;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.List;

/**
 * 需要接受来自运营商管理系统发送过来的商品sku json格式字符串，并调用商品搜索业务对象同步数据到solr中。
 */
public class ItemMessageListener extends AbstractAdaptableMessageListener {

    @Autowired
    private ItemSearchService itemSearchService;
    @Override
    public void onMessage(Message message, Session session) throws JMSException {

        //1.接受并转换消息
       TextMessage textMessage = (TextMessage) message;
        List<TbItem> itemList = JSONArray.parseArray(textMessage.getText(), TbItem.class);
        //2.同步solr数据
        itemSearchService.importItemList(itemList);
        System.out.println("同步索引库数据完成。");
    }
}
