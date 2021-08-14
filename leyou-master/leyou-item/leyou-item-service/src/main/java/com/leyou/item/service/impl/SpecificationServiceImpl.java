package com.leyou.item.service.impl;

import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.mapper.SpecificationMapper;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.pojo.Specification;
import com.leyou.item.service.SpecificationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecificationServiceImpl implements SpecificationService {

    @Autowired
    private SpecGroupMapper groupMapper;
    @Autowired
    private SpecParamMapper paramMapper;
    @Autowired
    private SkuMapper skuMapper;

    /**
     * 根据分类id查询分组
     * @param cid
     * @return
     */
    @Override
    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        return this.groupMapper.select(specGroup);
    }



    /**
     * 根据条件查询规格参数
     * @param gid
     * @return
     */
    @Override
    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setSearching(searching);
        param.setCid(cid);
        param.setGeneric(generic);
        return this.paramMapper.select(param);
    }

    @Override
    public String queryById(Long id) {
        String sku=this.skuMapper.querySpecificationByCategoryId(id);
        return sku;
    }

    @Override
    public List<SpecGroup> queryGroupsWithCid(Long cid) {
        List<SpecGroup> groups = this.queryGroupsByCid(cid);
        groups.forEach(group -> {
            List<SpecParam> params = this.queryParams(null,group.getCid(),null,null);
            group.setParams(params);
        });
        return groups;
    }


}
