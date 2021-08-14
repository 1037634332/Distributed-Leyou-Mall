package com.leyou.cart.service;

import com.leyou.cart.pojo.Cart;

import java.util.List;

/**
 * @Author: 98050
 * @Time: 2018-10-25 20:47
 * @Feature:
 */
public interface CartService {
    /**
     * 添加购物车
     * @param cart
     */
    void addCart(Cart cart);

    List<Cart> queryCartList();

    void updateCarts(Cart cart);

    void deleteCart(String skuId);
}
