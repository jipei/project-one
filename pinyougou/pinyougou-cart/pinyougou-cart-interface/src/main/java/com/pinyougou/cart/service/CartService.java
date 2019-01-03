package com.pinyougou.cart.service;

import com.pinyougou.vo.Cart;

import java.util.List;

public interface CartService {

    /**
     * 根据商品Id查询商品和购买数量加入到cartList
     * @param cartList 购物车列表
     * @param itemId 商品id
     * @param num 购买数量
     * @return 购物车列表
     */
    List<Cart> addItemToCartList(List<Cart> cartList, Long itemId, Integer num);

    /**
     * 将用户对应的购物车存入redis
     * @param newCartList 购物车列表
     * @param username 用户名
     */
    void saveCartListInRedisByUsername(List<Cart> newCartList, String username);

    /**
     *
     * 根据用户名到redis中查询其对应的购物车列表
     * @param username 用户名
     * @return  购物车列表
     */
    List<Cart> findCartListInRedisByUsername(String username);

    /**
     * 将cookie中的购物车与redis中的购物车列表进行合并到一个新列表
     * @param cookieCartList 购物车列表
     * @param redisCartList 购物车列表
     * @return 合并后的购物车列表
     */
    List<Cart> mergeCartList(List<Cart> cookieCartList, List<Cart> redisCartList);
}
