package com.leyou.vo;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.Brand;
import com.leyou.pojo.Goods;

import java.util.List;
import java.util.Map;

/**
 * @Author: 98050
 * Time: 2018-10-12 21:06
 * Feature: 搜索结果存储对象
 */
public class SearchResult extends PageResult<Goods> {

    /**
     * 分类的集合
     */
    private List<Map<String,Object>> categories;

    /**
     * 品牌的集合
     */
    private List<Brand> brands;

    /**
     * 规格参数的过滤条件
     */
    private List<Map<String,Object>> spec;

    public SearchResult(){
    }

    public SearchResult(List<Map<String, Object>> categories, List<Brand> brands) {
        this.categories = categories;
        this.brands = brands;
    }

    public SearchResult(Long total, List<Goods> items, List<Map<String, Object>> categories, List<Brand> brands) {
        super(total, items);
        this.categories = categories;
        this.brands = brands;
    }

    public SearchResult(Long total, Long totalPage, List<Goods> items, List<Map<String, Object>> categories, List<Brand> brands) {
        super(total, totalPage, items);
        this.categories = categories;
        this.brands = brands;
    }

    public SearchResult(List<Map<String, Object>> categories, List<Brand> brands, List<Map<String, Object>> spec) {
        this.categories = categories;
        this.brands = brands;
        this.spec = spec;
    }

    public SearchResult(Long total, List<Goods> items, List<Map<String, Object>> categories, List<Brand> brands, List<Map<String, Object>> spec) {
        super(total, items);
        this.categories = categories;
        this.brands = brands;
        this.spec = spec;
    }

    public SearchResult(Long total, Long totalPage, List<Goods> items, List<Map<String, Object>> categories, List<Brand> brands, List<Map<String, Object>> spec) {
        super(total, totalPage, items);
        this.categories = categories;
        this.brands = brands;
        this.spec = spec;
    }

    public List<Map<String, Object>> getSpec() {
        return spec;
    }

    public void setSpec(List<Map<String, Object>> spec) {
        this.spec = spec;
    }

    public List<Brand> getBrands() {
        return brands;
    }

    public void setBrands(List<Brand> brands) {
        this.brands = brands;
    }

    public List<Map<String, Object>> getCategories() {
        return categories;
    }

    public void setCategories(List<Map<String, Object>> categories) {
        this.categories = categories;
    }

    //    public List<Category> getCategories() {
//        return categories;
//    }

/*    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }*/


   /* public List<Map<String, Object>> getSpecs() {
        return specs;
    }

    public void setSpecs(List<Map<String, Object>> specs) {
        this.specs = specs;
    }*/

/*    public SearchResult(List<Category> categories, List<Brand> brands, List<Map<String,Object>> specs){
        this.categories = categories;
        this.brands = brands;
        this.specs = specs;
    }

    public SearchResult(Long total, List<Goods> item,List<Category> categories, List<Brand> brands, List<Map<String,Object>> specs){
        super(total,item);
        this.categories = categories;
        this.brands = brands;
        this.specs = specs;
    }*/

   /* public SearchResult(Long total,Long totalPage, List<Goods> item,List<Category> categories, List<Brand> brands){
        super(total,totalPage,item);
        this.categories = categories;
        this.brands = brands;
    }*/


    /* public SearchResult(Long total,Long totalPage, List<Goods> item,List<Category> categories,
                        List<Brand> brands, List<Map<String,Object>> specs){
        super(total,totalPage,item);
        this.categories = categories;
        this.brands = brands;
        this.specs = specs;
    }*/
}
