package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Author: 98050
 * Time: 2018-10-11 20:04
 * Feature:品牌服务接口
 */
@RequestMapping("brand")
public interface BrandApi {
    /**
     * 根据品牌id结合，查询品牌信息
     * @param ids
     * @return
     */
    @GetMapping("list")
    List<Brand> queryBrandByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("{id}")
    public Brand queryBrandById(@PathVariable("id") Long id);
}
