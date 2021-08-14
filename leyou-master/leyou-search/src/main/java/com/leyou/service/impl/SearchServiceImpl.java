package com.leyou.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.client.BrandClient;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecClient;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.*;
import com.leyou.repository.GoodsRepository;
import com.leyou.service.SearchService;
import com.leyou.bo.SearchRequest;


import com.leyou.utils.JsonUtils;
import com.leyou.utils.NumberUtils;
import com.leyou.vo.SearchResult;
import com.leyou.pojo.Goods;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.UnmappedTerms;
import org.elasticsearch.search.aggregations.metrics.stats.InternalStats;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: 98050
 * Time: 2018-10-11 22:59
 * Feature: 搜索功能
 */
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecClient specClient;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 查询商品信息
     * @param spu
     * @return
     * @throws IOException
     */
    @Override
    public Goods buildGoods(Spu spu) throws IOException {
        // 创建goods对象
        Goods goods = new Goods();


        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询分类名称
        List<String> names =  this.categoryClient.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 查询spu下的所有sku
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        List<Long> prices = new ArrayList<>();
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        // 遍历skus，获取价格集合
        skus.forEach(sku ->{
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("price", sku.getPrice());
            skuMap.put("image", StringUtils.isNotBlank(sku.getImages()) ? StringUtils.split(sku.getImages(), ",")[0] : "");
            skuMapList.add(skuMap);
        });


        // 查询出所有的搜索规格参数
        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), null, true);
        // 查询spuDetail。获取规格参数值
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        // 获取通用的规格参数
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getSpecTemplate(), new TypeReference<Map<String, Object>>() {
        });
        // 获取特殊的规格参数
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecifications(), new TypeReference<Map<String, List<Object>>>() {
        });
        // 定义map接收{规格参数名，规格参数值}
        Map<String, Object> paramMap = new HashMap<>();
        params.forEach(param -> {
            // 判断是否通用规格参数
            if (param.getGeneric()) {
                // 获取通用规格参数值
                String value = genericSpecMap.get(param.getId().toString()).toString();
                // 判断是否是数值类型
                if (param.getNumeric()){
                    // 如果是数值的话，判断该数值落在那个区间
                    value = chooseSegment(value, param);
                }
                // 把参数名和值放入结果集中
                paramMap.put(param.getName(), value);
            } else {
                paramMap.put(param.getName(), specialSpecMap.get(param.getId().toString()));
            }

        });

        // 设置参数
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        goods.setAll(spu.getTitle() + brand.getName() + StringUtils.join(names, " "));
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(paramMap);

        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = org.apache.commons.lang3.math.NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = org.apache.commons.lang3.math.NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = org.apache.commons.lang3.math.NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 搜索
     * @param request
     * @return
     */
    @Override
    public SearchResult search(SearchRequest request) {

        String key = request.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }

        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 1、对key进行全文检索查询
        QueryBuilder basicQuery = QueryBuilders.matchQuery("all", key).operator(Operator.AND);
        queryBuilder.withQuery(basicQuery);

        // 2、通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));

        // 3、分页
        // 准备分页参数
        int page = request.getPage();
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page - 1, size));

        String categoryAggName = "category";
        String brandAggName = "brand";
        //对商品分类进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //对品牌进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        //查询，获取结果
        AggregatedPage<Goods> pageInfo = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());
        List<Map<String,Object>> categories = getCategoryAggResult(pageInfo.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(pageInfo.getAggregation(brandAggName));

        //没做完成
         /* //处理规格参数
        List<Map<String,Object>> specs = null;
        if (CollectionUtils.isEmpty(categories) || categories.size() == 1){
            //如果商品分类只有一个进行聚合，并根据分类与基本查询条件聚合
            specs = getSpecAggResult((Long) categories.get(0).get("id"),basicQuery);
        }*/

        // 封装结果并返回
        return new SearchResult(pageInfo.getTotalElements(), (long) pageInfo.getTotalPages(), pageInfo.getContent(), categories ,brands);
    }


    /**
     * 创建索引
     * @param id
     */
    @Override
    public void createIndex(Long id) throws IOException {
        SpuBo spuBo = this.goodsClient.queryGoodsById(id);
        //构建商品
        Goods goods = this.buildGoods(spuBo);

        //保存数据到索引库中
        this.goodsRepository.save(goods);
    }

    /**
     * 删除索引
     * @param id
     */
    @Override
    public void deleteIndex(Long id) {

        this.goodsRepository.deleteById(id);
    }

    /**
     * 构建带过滤条件的基本查询
     * @param searchRequest
     * @return
     */
    private QueryBuilder buildBasicQueryWithFilter(SearchRequest searchRequest) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //基本查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));
        //过滤条件构造器
        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        //整理过滤条件
        Map<String,String> filter = searchRequest.getFilter();
        for (Map.Entry<String,String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String regex = "^(\\d+\\.?\\d*)-(\\d+\\.?\\d*)$";
            if (!"key".equals(key)) {
                if ("price".equals(key)){
                    if (!value.contains("元以上")) {
                        String[] nums = StringUtils.substringBefore(value, "元").split("-");
                        filterQueryBuilder.must(QueryBuilders.rangeQuery(key).gte(Double.valueOf(nums[0]) * 100).lt(Double.valueOf(nums[1]) * 100));
                    }else {
                        String num = StringUtils.substringBefore(value,"元以上");
                        filterQueryBuilder.must(QueryBuilders.rangeQuery(key).gte(Double.valueOf(num)*100));
                    }
                }else {
                    if (value.matches(regex)) {
                        Double[] nums = NumberUtils.searchNumber(value, regex);
                        //数值类型进行范围查询   lt:小于  gte:大于等于
                        filterQueryBuilder.must(QueryBuilders.rangeQuery("specs." + key).gte(nums[0]).lt(nums[1]));
                    } else {
                        //商品分类和品牌要特殊处理
                        if (key != "cid3" && key != "brandId") {
                            key = "specs." + key + ".keyword";
                        }
                        //字符串类型，进行term查询
                        filterQueryBuilder.must(QueryBuilders.termQuery(key, value));
                    }
                }
            } else {
                break;
            }
        }
        //添加过滤条件
        queryBuilder.filter(filterQueryBuilder);
        return queryBuilder;
    }

    /**
     * 聚合规格参数
     * @param
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getSpecAggResult(Long cid, QueryBuilder basicQuery) {
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        List<SpecParam> params=this.specClient.queryParams(null,cid,null,true);
        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs."+param.getName()+".key"));
        });
        queryBuilder.withSourceFilter(new FetchSourceFilter(new  String[]{},null));
        AggregatedPage<Goods> goodsPage= (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());
        List<Map<String, Object>> specs=new ArrayList<>();
        Map<String,Aggregation> aggregationMap=goodsPage.getAggregations().asMap();
        for (Map.Entry<String,Aggregation> entry : aggregationMap.entrySet()){
            Map<String,Object> map=new HashMap<>();
            System.out.println(entry.toString());
            map.put("k",entry.getKey());
            List<String> options=new ArrayList<>();
            StringTerms terms= (StringTerms) entry.getValue();
            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options",options);
            specs.add(map);
        }
        return specs;

    }

    /**
     * 聚合得到interval
     * @param id
     * @param keySet
     * @return
     */
    private Map<String, Double> getNumberInterval(Long id, Set<String> keySet) {
        Map<String,Double> numbericalSpecs = new HashMap<>();
        //准备查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //不查询任何数据
        queryBuilder.withQuery(QueryBuilders.termQuery("cid3",id.toString())).withSourceFilter(new FetchSourceFilter(new String[]{""},null)).withPageable(PageRequest.of(0,1));
        //添加stats类型的聚合,同时返回avg、max、min、sum、count等
        for (String key : keySet){
            queryBuilder.addAggregation(AggregationBuilders.stats(key).field("specs." + key));
        }
        Map<String,Aggregation> aggregationMap = this.elasticsearchTemplate.query(queryBuilder.build(),
                searchResponse -> searchResponse.getAggregations().asMap()
        );
        for (String key : keySet){
            InternalStats stats = (InternalStats) aggregationMap.get(key);
            double interval = this.getInterval(stats.getMin(),stats.getMax(),stats.getSum());
            numbericalSpecs.put(key,interval);
        }
        return numbericalSpecs;
    }

    /**
     * 根据最小值，最大值，sum计算interval
     * @param min
     * @param max
     * @param sum
     * @return
     */
    private double getInterval(double min, double max, Double sum) {
        //显示7个区间
        double interval = (max - min) / 6.0d;
        //判断是否是小数
        if (sum.intValue() == sum){
            //不是小数，要取整十、整百
            int length = StringUtils.substringBefore(String.valueOf(interval),".").length();
            double factor = Math.pow(10.0,length - 1);
            return Math.round(interval / factor)*factor;
        }else {
            //是小数的话就保留一位小数
            return NumberUtils.scale(interval,1);
        }
    }

    /**
     * 根据规格参数，聚合得到过滤属性值
     * @param strSpec
     * @param numericalInterval
     * @param numericalUnits
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> aggForSpec(Set<String> strSpec, Map<String, Double> numericalInterval, Map<String, String> numericalUnits, QueryBuilder basicQuery) {
        List<Map<String,Object>> specs = new ArrayList<>();
        //准备查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        //聚合数值类型
        for (Map.Entry<String,Double> entry : numericalInterval.entrySet()) {
            queryBuilder.addAggregation(AggregationBuilders.histogram(entry.getKey()).field("specs." + entry.getKey()).interval(entry.getValue()).minDocCount(1));
        }
        //聚合字符串
        for (String key :strSpec){
            queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs."+key+".keyword"));
        }

        //解析聚合结果
        Map<String,Aggregation> aggregationMap = this.elasticsearchTemplate.query(queryBuilder.build(), SearchResponse :: getAggregations).asMap();

        //解析数值类型
        for (Map.Entry<String,Double> entry :numericalInterval.entrySet()){
            Map<String,Object> spec = new HashMap<>();
            String key = entry.getKey();
            spec.put("k",key);
            spec.put("unit",numericalUnits.get(key));
            //获取聚合结果
            InternalHistogram histogram = (InternalHistogram) aggregationMap.get(key);
            spec.put("options",histogram.getBuckets().stream().map(bucket -> {
                Double begin = (Double) bucket.getKey();
                Double end = begin + numericalInterval.get(key);
                //对begin和end取整
                if (NumberUtils.isInt(begin) && NumberUtils.isInt(end)){
                    //确实是整数，直接取整
                    return begin.intValue() + "-" + end.intValue();
                }else {
                    //小数，取2位小数
                    begin = NumberUtils.scale(begin,2);
                    end = NumberUtils.scale(end,2);
                    return begin + "-" + end;
                }
            }).collect(Collectors.toList()));
            specs.add(spec);
        }

        //解析字符串类型
        strSpec.forEach(key -> {
            Map<String,Object> spec = new HashMap<>();
            spec.put("k",key);
            StringTerms terms = (StringTerms) aggregationMap.get(key);
            spec.put("options",terms.getBuckets().stream().map((Function<StringTerms.Bucket, Object>) StringTerms.Bucket::getKeyAsString).collect(Collectors.toList()));
            specs.add(spec);
        });
        return specs;
    }

    /**
     * 解析品牌聚合结果
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        //获取聚合的桶
        return terms.getBuckets().stream().map(bucket -> {
            return this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());

    }

    /**
     * 解析商品分类聚合结果
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        //获取聚合的桶集合，转化为List<Map<String,Object>>
        return terms.getBuckets().stream().map(bucket -> {
            Map<String,Object> map=new HashMap<>();
            Long id = bucket.getKeyAsNumber().longValue();
            List<String> names= this.categoryClient.queryNameByIds(Arrays.asList(id));
            map.put("id",id);
            map.put("name",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 构建基本查询条件
     * @param queryBuilder
     * @param request
     */
    private void searchWithPageAndSort(NativeSearchQueryBuilder queryBuilder, SearchRequest request) {
        // 准备分页参数
        int page = request.getPage();
        int size = request.getDefaultSize();

        // 1、分页
        queryBuilder.withPageable(PageRequest.of(page - 1, size));
        // 2、排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if (StringUtils.isNotBlank(sortBy)) {
            // 如果不为空，则进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc ? SortOrder.DESC : SortOrder.ASC));
        }
    }
}
