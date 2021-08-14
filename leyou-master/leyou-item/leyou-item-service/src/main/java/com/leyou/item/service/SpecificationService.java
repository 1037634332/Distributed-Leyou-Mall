package com.leyou.item.service;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.pojo.Specification;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Author: 98050
 * Time: 2018-08-14 15:26
 * Feature:
 */
public interface SpecificationService {
    /**
     * 根据分类id查询分组
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsByCid(Long cid);

    List<SpecParam> queryParams(Long gid,Long cid, Boolean generic, Boolean searching);

    /**
     * 根据category id查询规格参数模板
     * @param id
     * @return
     */
    String queryById(Long id);

    List<SpecGroup> queryGroupsWithCid(Long cid);
}
