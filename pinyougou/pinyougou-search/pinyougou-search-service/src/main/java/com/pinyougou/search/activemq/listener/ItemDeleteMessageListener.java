package com.pinyougou.search.activemq.listener;

import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.Arrays;

public class ItemDeleteMessageListener extends AbstractAdaptableMessageListener {

    @Autowired
    private ItemSearchService itemSearchService;
    @Override
    public void onMessage(Message message, Session session) throws JMSException {

        //1.接受并转换消息
        ObjectMessage objectMessage= (ObjectMessage) message;
        Long[] goodsIds= (Long[]) objectMessage.getObject();

        //2.同步sorl数据
        itemSearchService.deleteItemByGoodsIdList(Arrays.asList(goodsIds));
    }
}
