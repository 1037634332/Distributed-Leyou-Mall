package com.leyou.order.service;

import com.leyou.LeyouOrderApplication;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.utils.IdWorker;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes= LeyouOrderApplication.class)
public class OrderServiceTest extends TestCase {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private OrderService orderService;

    @Test
    public void testCreateOrder() {
        /*OrderDetail orderDetail=new OrderDetail(3893493L,1,"苹果（Apple）iPhone 6 (A1586) 16GB 金色 移动联通电信4G手机3",236800L,"{\"机身颜色\":\"钻雕蓝\",\"内存\":\"4GB\",\"机身存储\":\"64GB\"}","http://image.leyou.com/images/9/4/1524297342728.jpg");
        List<OrderDetail> orderDetails=new ArrayList<>();
        orderDetails.add(orderDetail);
        Order order=new Order(236800L,236800L,2,0,null,"huge","锋哥","15800000000","上海","上海","浦东新签","航头镇航头路18号传智播客3号楼","210000",2,0,orderDetails);
        Long orderId=this.orderService.createOrder(order);
        System.out.println(orderId);*/
    }

    @Test
    public void testQueryById() {
        /*Order order = this.orderService.queryById(1426154617934319616L);
        System.out.println(order);*/
    }

    public void testQueryUserOrderList() {
    }

    public void testUpdateStatus() {
    }
}