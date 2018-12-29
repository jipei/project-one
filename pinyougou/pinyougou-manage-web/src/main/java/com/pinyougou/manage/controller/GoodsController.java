package com.pinyougou.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import com.pinyougou.sellergoods.service.GoodsService;
import com.pinyougou.vo.Goods;
import com.pinyougou.vo.PageResult;
import com.pinyougou.vo.Result;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.jms.*;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/goods")
@RestController
public class GoodsController {

    @Reference
    private GoodsService goodsService;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ActiveMQQueue itemSolrQueue;

    @Autowired
    private ActiveMQQueue itemSolrDeleteQueue;

    @Autowired
    private ActiveMQTopic itemTopic;

    @Autowired
    private ActiveMQTopic itemDeleteTopic;

    @RequestMapping("/findAll")
    public List<TbGoods> findAll() {
        return goodsService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {
        return goodsService.findPage(page, rows);
    }

    /**
     * 保存商品数据（基本、描述、sku列表）
     * @param goods 基本、描述、sku列表
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Goods goods) {
        try {
            //获取当前登录用户名
            String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
            //商品的商家
            goods.getGoods().setSellerId(sellerId);
            //商品的状态为 0 未审核
            goods.getGoods().setAuditStatus("0");

            goodsService.addGoods(goods);
            return Result.ok("增加商品成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("增加商品失败");
    }

    @GetMapping("/findOne")
    public Goods findOne(Long id) {
        return goodsService.findGoodsById(id);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Goods goods) {
        try {
            //校验商家
            TbGoods o1dGoods = goodsService.findOne(goods.getGoods().getId());
            String sellerId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!sellerId.equals(o1dGoods.getSellerId())|| !sellerId.equals(
                    goods.getGoods().getSellerId())){
                return Result.fail("操纵非法");
            }
            goodsService.updateGoods(goods);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            goodsService.deleteGoodsByIds(ids);
            //删除solr中对应商品索引数据
            sendMQMsg(itemSolrDeleteQueue,ids);

            //发送主题消息
            sendMQMsg(itemDeleteTopic,ids);
            return Result.ok("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("删除失败");
    }

    private void sendMQMsg(Destination destination , Long[] ids) throws JMSException{
        jmsTemplate.send(destination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                ObjectMessage objectMessage = session.createObjectMessage();
                objectMessage.setObject(ids);
                return objectMessage;
            }
        });
    }

    /**
     * 分页查询列表
     * @param goods 查询条件
     * @param page 页号
     * @param rows 每页大小
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody  TbGoods goods, @RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {

        return goodsService.search(page, rows, goods);
    }

    //更新商品状态
    @GetMapping("/updateStatus")
    public Result updateStatus(Long[] ids, String status){
        try {
            goodsService.updateStatus(ids, status);

            if ("2".equals(status)) {
                //如果审核通过则需要更新
                //根据商品spu id 数组查询已经启用的那些sku商品列表itemList
                List<TbItem> itemList = goodsService.findItemListByGoodsIdsAndStatus(ids, "1");
                jmsTemplate.send(itemSolrQueue, new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        TextMessage textMessage = session.createTextMessage();
                        textMessage.setText(JSON.toJSONString(itemList));
                        return textMessage;
                    }
                });
                sendMQMsg(itemTopic,ids);
            }
            return Result.ok("修改商品状态成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("修改商品状态失败");
    }

}
