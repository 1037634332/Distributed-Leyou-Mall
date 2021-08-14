package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Author: 98050
 * Time: 2018-10-11 20:05
 * Feature:
 */
@RequestMapping("spec")
public interface SpecApi {
    /**
     * 查询商品分类对应的规格参数模板
     * @param
     * @return
     */
    @GetMapping("params")
    public List<SpecParam> queryParams(@RequestParam(value = "gid", required = false) Long gid,
                                                       @RequestParam(value = "cid", required = false) Long cid,
                                                       @RequestParam(value = "generic", required = false) Boolean generic,
                                                       @RequestParam(value = "searching", required = false) Boolean searching);


    /**
     * 查询商品分类对应的规格参数模板
     * @param id
     * @return
     */
    @GetMapping("{id}")
    String querySpecificationByCategoryId(@PathVariable("id") Long id);

    /**
     * 根据分类id查询分组及分组详情
     * @param cid
     * @return
     */
    @GetMapping("groups/param/{cid}")
    public List<SpecGroup> queryGroupsWithCid(@PathVariable("cid")Long cid);

}
