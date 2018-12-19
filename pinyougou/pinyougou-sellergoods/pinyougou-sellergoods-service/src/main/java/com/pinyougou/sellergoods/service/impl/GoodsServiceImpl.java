package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.sellergoods.service.GoodsService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.Goods;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
@Transactional
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl extends BaseServiceImpl<TbGoods> implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsDescMapper goodsDescMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemCatMapper itemCatMapper;

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private BrandMapper brandMapper;


    @Override
    public PageResult search(Integer page, Integer rows, TbGoods goods) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();

        //不查询删除商品
        criteria.andNotEqualTo("isDelete","1");

        //商家限定
        if(!StringUtils.isEmpty(goods.getSellerId())){
            criteria.andEqualTo("sellerId", goods.getSellerId());
        }

        if(!StringUtils.isEmpty(goods.getAuditStatus())){
            criteria.andLike("auditStatus", goods.getAuditStatus());
        }

        if(!StringUtils.isEmpty(goods.getGoodsName())){
            criteria.andLike("goodsName", "%" + goods.getGoodsName() + "%");
        }

        List<TbGoods> list = goodsMapper.selectByExample(example);
        PageInfo<TbGoods> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public void addGoods(Goods goods) {
        //1、保存商品基本信息
        add(goods.getGoods());

        //2、保存商品描述信息
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
        goodsDescMapper.insertSelective(goods.getGoodsDesc());

        //3、保存商品sku列表信息
        saveItemList(goods);
    }

    @Override
    public Goods findGoodsById(Long id) {
        Goods goods = new Goods();
        //查询商品SPU
        TbGoods tbGoods = findOne(id);
        goods.setGoods(tbGoods);

        //查询商品描述
        TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
        goods.setGoodsDesc(tbGoodsDesc);

        //查询商品SKU列表
        TbItem item = new TbItem();
        item.setGoodsId(id);
        List<TbItem> itemList = itemMapper.select(item);
        goods.setItemList(itemList);
        return goods;
    }

    @Override
    public void updateGoods(Goods goods) {
        //更新商品基本信息
        update(goods.getGoods());

        //跟更新商品描述信息
        goodsDescMapper.updateByPrimaryKeySelective(goods.getGoodsDesc());

        //删除原有的SKU列表
        TbItem param = new TbItem();
        param.setGoodsId(goods.getGoods().getId());
        itemMapper.delete(param);

        //保存商品SKU
        saveItemList(goods);
    }

    @Override
    public void updateStatus(Long[] ids, String status) {
        //批量更新商品的状态
        //update tb_goods set audit_status=? where id in(?,...)
        TbGoods goods = new TbGoods();
        goods.setAuditStatus(status);

        Example example = new Example(TbGoods.class);
        example.createCriteria().andIn("id", Arrays.asList(ids));

        //批量更新商品的审核状态
        goodsMapper.updateByExampleSelective(goods,example);

        //如果是审核通过则将SKU设置为启用状态
        if ("2".equals(status)){
            //更新的内容
            //根据商品spu id数组修改对于的sku 的状态为1
            //update tb_item set status=1 where goods_id in (?,?,,,);
            TbItem item = new TbItem();
            item.setStatus("1");

            //更新条件
            Example itemExample = new Example(TbItem.class);
            itemExample.createCriteria().andIn("goodsId",Arrays.asList(ids));

            itemMapper.updateByExampleSelective(item,itemExample);
        }
    }

    @Override
    public void deleteGoodsByIds(Long[] ids) {
        TbGoods goods = new TbGoods();
        goods.setIsDelete("1");

        Example example = new Example(TbGoods.class);
        example.createCriteria().andIn("id",Arrays.asList(ids));

        //批量更新商品的删除状态为删除
        goodsMapper.updateByExampleSelective(goods,example);

    }

    /**
     * 保存商品sku列表
     * @param goods 商品信息（基本、描述、sku列表）
     */
    private void saveItemList(Goods goods) {
        //启用规格
        System.out.println(goods.getItemList().size());
        if(goods.getItemList() != null && goods.getItemList().size() > 0){
            for (TbItem item : goods.getItemList()) {

                //sku标题 = spu的名称+规格的名称拼接
                String title = goods.getGoods().getGoodsName();
                //获取sku的规格
                Map<String, String> map = JSON.parseObject(item.getSpec(), Map.class);
                Set<Map.Entry<String, String>> entries = map.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    title += " " + entry.getValue();
                }
                item.setTitle(title);

                setItemValue(goods, item);

                //保存sku
                itemMapper.insertSelective(item);
            }
        }
    }

    /**
     * 设置sku的值
     * @param goods 商品信息（基本、描述、sku列表）
     * @param item sku
     */
    private void setItemValue(Goods goods, TbItem item) {
        item.setGoodsId(goods.getGoods().getId());
        //sku的商品分类 = spu的第3级商品分类
        item.setCategoryid(goods.getGoods().getCategory3Id());
        //商品分类中文名称
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(item.getCategoryid());
        item.setCategory(itemCat.getName());

        //获取spu的第一张图片
        if (!StringUtils.isEmpty(goods.getGoodsDesc().getItemImages())) {
            List<Map> imageList = JSONArray.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
            //if (imageList.size()>0)
            item.setImage(imageList.get(0).get("url").toString());
        }

        item.setSellerId(goods.getGoods().getSellerId());
        TbSeller seller = sellerMapper.selectByPrimaryKey(item.getSellerId());
        item.setSeller(seller.getName());

        item.setCreateTime(new Date());
        item.setUpdateTime(item.getCreateTime());

        //品牌
        TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(brand.getName());
    }
}
