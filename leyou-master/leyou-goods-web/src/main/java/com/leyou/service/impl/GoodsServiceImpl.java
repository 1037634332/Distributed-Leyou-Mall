package com.leyou.service.impl;

import com.leyou.client.SpecClient;
import com.leyou.item.pojo.*;
import com.leyou.service.GoodsService;
import com.leyou.client.BrandClient;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @Author: 98050
 * Time: 2018-10-17 19:40
 * Feature:商品详情页信息
 */
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecClient specificationClient;

    public Map<String, Object> loadModel(Long spuId){
        Map<String, Object> map = new HashMap<>();

        // 根据id查询spu对象
        Spu spu = this.goodsClient.querySpuById(spuId);

        // 查询spudetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);

        // 查询sku集合
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spuId);

        // 查询分类
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNameByIds(cids);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", cids.get(i));
            categoryMap.put("name", names.get(i));
            categories.add(categoryMap);
        }

        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询规格参数组
        List<SpecGroup> groups = this.specificationClient.queryGroupsWithCid(spu.getCid3());

        // 查询特殊的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), false ,null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });

        // 封装spu
        map.put("spu", spu);
        // 封装spuDetail
        map.put("spuDetail", spuDetail);
        // 封装sku集合
        map.put("skus", skus);
        // 分类
        map.put("categories", categories);
        // 品牌
        map.put("brand", brand);
        // 规格参数组
        map.put("groups", groups);
        // 查询特殊规格参数
        map.put("paramMap", paramMap);
        return map;
    }

   /* @Override
    public Map<String, Object> loadModel(Long spuId) {

        //查询商品信息
        SpuBo spuBo = this.goodsClient.queryGoodsById(spuId);

        //查询商品详情
        SpuDetail spuDetail = spuBo.getSpuDetail();

        //查询skus
        List<Sku> skuList = spuBo.getSkus();

        //查询分类信息
        List<Long> ids = new ArrayList<>();
        ids.add(spuBo.getCid1());
        ids.add(spuBo.getCid2());
        ids.add(spuBo.getCid3());
        List<Category> categoryList = this.categoryClient.queryCategoryByIds(ids);

        //查询品牌信息
        Brand brand = this.brandClient.queryBrandByIds(Collections.singletonList(spuBo.getBrandId())).get(0);


        *//**
         * 对于规格属性的处理需要注意以下几点：
         *      1. 所有规格都保存为id和name形式
         *      2. 规格对应的值保存为id和value形式
         *      3. 都是map形式
         *      4. 将特有规格参数单独抽取
         *//*

        //获取所有规格参数，然后封装成为id和name形式的数据
        String allSpecJson = spuDetail.getSpecifications();
        List<Map<String,Object>> allSpecs = JsonUtils.nativeRead(allSpecJson, new TypeReference<List<Map<String, Object>>>() {
        });
        Map<Integer,String> specName = new HashMap<>();
        Map<Integer,Object> specValue = new HashMap<>();
        this.getAllSpecifications(allSpecs,specName,specValue);


        //获取特有规格参数
        String specTJson = spuDetail.getSpecTemplate();
        Map<String,String[]> specs = JsonUtils.nativeRead(specTJson, new TypeReference<Map<String, String[]>>() {
        });
        Map<Integer,String> specialParamName = new HashMap<>();
        Map<Integer,String[]> specialParamValue = new HashMap<>();
        this.getSpecialSpec(specs,specName,specValue,specialParamName,specialParamValue);

        //按照组构造规格参数
        List<Map<String,Object>> groups = this.getGroupsSpec(allSpecs,specName,specValue);



        Map<String,Object> map = new HashMap<>();
        map.put("spu",spuBo);
        map.put("spuDetail",spuDetail);
        map.put("skus",skuList);
        map.put("brand",brand);
        map.put("categories",categoryList);
        map.put("specName",specName);
        map.put("specValue",specValue);
        map.put("groups",groups);
        map.put("specialParamName",specialParamName);
        map.put("specialParamValue",specialParamValue);

        return map;*/


    private List<Map<String, Object>> getGroupsSpec(List<Map<String, Object>> allSpecs, Map<Integer, String> specName, Map<Integer, Object> specValue) {
        List<Map<String, Object>> groups = new ArrayList<>();
        int i = 0;
        int j = 0;
        for (Map<String,Object> spec :allSpecs){
            List<Map<String, Object>> params = (List<Map<String, Object>>) spec.get("params");
            List<Map<String,Object>> temp = new ArrayList<>();
            for (Map<String,Object> param :params) {
                for (Map.Entry<Integer, String> entry : specName.entrySet()) {
                    if (entry.getValue().equals(param.get("k").toString())) {
                        String value = specValue.get(entry.getKey()) != null ? specValue.get(entry.getKey()).toString() : "无";
                        Map<String, Object> temp3 = new HashMap<>(16);
                        temp3.put("id", ++j);
                        temp3.put("name", entry.getValue());
                        temp3.put("value", value);
                        temp.add(temp3);
                    }
                }
            }
            Map<String,Object> temp2 = new HashMap<>(16);
            temp2.put("params",temp);
            temp2.put("id",++i);
            temp2.put("name",spec.get("group"));
            groups.add(temp2);
        }
        return groups;
    }

    private void getSpecialSpec(Map<String, String[]> specs, Map<Integer, String> specName, Map<Integer, Object> specValue, Map<Integer, String> specialParamName, Map<Integer, String[]> specialParamValue) {
        if (specs != null) {
            for (Map.Entry<String, String[]> entry : specs.entrySet()) {
                String key = entry.getKey();
                for (Map.Entry<Integer,String> e : specName.entrySet()) {
                    if (e.getValue().equals(key)){
                        specialParamName.put(e.getKey(),e.getValue());
                        //因为是放在数组里面，所以要先去除两个方括号，然后再以逗号分割成数组
                        String  s = specValue.get(e.getKey()).toString();
                        String result = StringUtils.substring(s,1,s.length()-1);
                        specialParamValue.put(e.getKey(), result.split(","));
                    }
                }
            }
        }
    }

    private void getAllSpecifications(List<Map<String, Object>> allSpecs, Map<Integer, String> specName, Map<Integer, Object> specValue) {
        String k = "k";
        String v = "v";
        String unit = "unit";
        String numerical = "numerical";
        String options ="options";
        int i = 0;
        if (allSpecs != null){
            for (Map<String,Object> s : allSpecs){
                List<Map<String, Object>> params = (List<Map<String, Object>>) s.get("params");
                for (Map<String,Object> param :params){
                    String result;
                    if (param.get(v) == null){
                        result = "无";
                    }else{
                        result = param.get(v).toString();
                    }
                    if (param.containsKey(numerical) && (boolean) param.get(numerical)) {
                        if (result.contains(".")){
                            Double d = Double.valueOf(result);
                            if (d.intValue() == d){
                                result = d.intValue()+"";
                            }
                        }
                        i++;
                        specName.put(i,param.get(k).toString());
                        specValue.put(i,result+param.get(unit).toString());
                    } else if (param.containsKey(options)){
                        i++;
                        specName.put(i,param.get(k).toString());
                        specValue.put(i,param.get(options));
                    }else {
                        i++;
                        specName.put(i,param.get(k).toString());
                        specValue.put(i,param.get(v));
                    }
                }
            }
        }
    }

}
