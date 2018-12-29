package com.pinyougou.item.activemq.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.io.File;

/**
 * 根据商品spu id数组删除商品详情静态页面
 */
public class ItemDeleteMessageListener extends AbstractAdaptableMessageListener {

    @Value("$(ITEM_HTML_PATH)")
    private String ITEM_HTML_PATH;
    @Override
    public void onMessage(Message message, Session session) throws JMSException {

        //1.接受并转换消息
        ObjectMessage objectMessage= (ObjectMessage) message;
        Long[] goodsIds = (Long[]) objectMessage.getObject();

        //2.遍历数组并删除商品的静态页面
        if (goodsIds !=null && goodsIds.length>0){
            for (Long goodsId : goodsIds) {
                File file = new File(ITEM_HTML_PATH + goodsId + ".html");
                if (file.exists()){
                    file.delete();
                }
            }
        }

    }
}
