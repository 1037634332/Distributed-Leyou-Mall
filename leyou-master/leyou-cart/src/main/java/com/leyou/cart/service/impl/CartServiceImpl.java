package com.leyou.cart.service.impl;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import com.leyou.item.pojo.Sku;
import com.leyou.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 98050
 * @Time: 2018-10-25 20:48
 * @Feature:
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private GoodsClient goodsClient;

    private static String KEY_PREFIX = "leyou:cart:uid:";

    private final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    /**
     * 添加购物车
     * @param cart
     */
    @Override
    public void addCart(Cart cart) {
        //1.获取用户
        UserInfo userInfo = LoginInterceptor.getLoginUser();
        //2.Redis的key
        String key = KEY_PREFIX + userInfo.getId();
        //3.获取hash操作对象
        BoundHashOperations<String,Object,Object> hashOperations = this.stringRedisTemplate.boundHashOps(key);
        //4.查询是否存在
        Long skuId = cart.getSkuId();
        Integer num = cart.getNum();
        Boolean result = hashOperations.hasKey(skuId.toString());
        if (result){
            //5.存在，获取购物车数据
            String json = hashOperations.get(skuId.toString()).toString();
            cart = JsonUtils.parse(json,Cart.class);
            //6.修改购物车数量
            cart.setNum(cart.getNum() + num);
        }else{
            //7.不存在，新增购物车数据
            cart.setUserId(userInfo.getId());
            //8.其他商品信息，需要查询商品微服务
            Sku sku = this.goodsClient.querySkuById(skuId);
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());
        }
        //9.将购物车数据写入redis
        hashOperations.put(cart.getSkuId().toString(),JsonUtils.serialize(cart));
    }

    public List<Cart> queryCartList() {
        // 获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();

        // 判断是否存在购物车
        String key = KEY_PREFIX + user.getId();
        if(!this.stringRedisTemplate.hasKey(key)){
            // 不存在，直接返回
            return null;
        }
        BoundHashOperations<String, Object, Object> hashOps = this.stringRedisTemplate.boundHashOps(key);
        List<Object> carts = hashOps.values();
        // 判断是否有数据
        if(CollectionUtils.isEmpty(carts)){
            return null;
        }
        // 查询购物车数据
        return carts.stream().map(o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());
    }

    public void updateCarts(Cart cart) {
        // 获取登陆信息
        UserInfo userInfo = LoginInterceptor.getLoginUser();
        String key = KEY_PREFIX + userInfo.getId();
        // 获取hash操作对象
        BoundHashOperations<String, Object, Object> hashOperations = this.stringRedisTemplate.boundHashOps(key);
        // 获取购物车信息
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
        // 更新数量
        cart1.setNum(cart.getNum());
        // 写入购物车
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart1));
    }

    public void deleteCart(String skuId) {
        // 获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
        String key = KEY_PREFIX + user.getId();
        BoundHashOperations<String, Object, Object> hashOps = this.stringRedisTemplate.boundHashOps(key);
        hashOps.delete(skuId);
    }
}
