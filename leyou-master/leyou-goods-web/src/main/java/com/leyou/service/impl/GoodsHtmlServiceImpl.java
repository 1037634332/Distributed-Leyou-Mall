package com.leyou.service.impl;

import com.leyou.service.GoodsHtmlService;
import com.leyou.service.GoodsService;


import com.leyou.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @Author: 98050
 * @Time: 2018-10-19 09:46
 * @Feature: 实现页面静态化接口
 */
@Service
public class GoodsHtmlServiceImpl implements GoodsHtmlService {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private TemplateEngine templateEngine;

    private static final Logger LOGGER = LoggerFactory.getLogger(GoodsHtmlService.class);


    @Override
    public void createHtml(Long spuId) {
        System.out.println("静态化："+spuId);
        PrintWriter writer = null;

        //获取页面数据
        Map<String,Object> spuMap = this.goodsService.loadModel(spuId);
        //创建Thymeleaf上下文对象
        Context context = new Context();
        //把数据放入上下文对象
        context.setVariables(spuMap);

        try {
            //创建输出流
            File file = new File("D:\\program-tools\\nginx-1.14.0\\html\\item\\"+spuId+".html");
            writer = new PrintWriter(file);

            //执行页面静态化方法
            templateEngine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            LOGGER.error("页面静态化出错：{}"+e,spuId);
        }finally {
            if (writer != null){
                writer.close();
            }
        }
    }

    /**
     * 新建线程处理页面静态化
     * @param spuId
     */
    @Override
    public void asyncExecute(Long spuId) {
        ThreadUtils.execute(() ->createHtml(spuId));
    }

    @Override
    public void deleteHtml(Long id) {
        File file = new File("D:\\program-tools\\nginx-1.14.0\\html\\item\\"+id+".html");
        file.deleteOnExit();
    }
}
