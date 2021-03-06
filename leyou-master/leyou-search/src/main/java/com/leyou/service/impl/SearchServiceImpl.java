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
 * Feature: ????????????
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
     * ??????????????????
     * @param spu
     * @return
     * @throws IOException
     */
    @Override
    public Goods buildGoods(Spu spu) throws IOException {
        // ??????goods??????
        Goods goods = new Goods();


        // ????????????
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // ??????????????????
        List<String> names =  this.categoryClient.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // ??????spu????????????sku
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        List<Long> prices = new ArrayList<>();
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        // ??????skus?????????????????????
        skus.forEach(sku ->{
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("price", sku.getPrice());
            skuMap.put("image", StringUtils.isNotBlank(sku.getImages()) ? StringUtils.split(sku.getImages(), ",")[0] : "");
            skuMapList.add(skuMap);
        });


        // ????????????????????????????????????
        List<SpecParam> params = this.specClient.queryParams(null, spu.getCid3(), null, true);
        // ??????spuDetail????????????????????????
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        // ???????????????????????????
        Map<String, Object> genericSpecMap = MAPPER.readValue(spuDetail.getSpecTemplate(), new TypeReference<Map<String, Object>>() {
        });
        // ???????????????????????????
        Map<String, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecifications(), new TypeReference<Map<String, List<Object>>>() {
        });
        // ??????map??????{?????????????????????????????????}
        Map<String, Object> paramMap = new HashMap<>();
        params.forEach(param -> {
            // ??????????????????????????????
            if (param.getGeneric()) {
                // ???????????????????????????
                String value = genericSpecMap.get(param.getId().toString()).toString();
                // ???????????????????????????
                if (param.getNumeric()){
                    // ?????????????????????????????????????????????????????????
                    value = chooseSegment(value, param);
                }
                // ????????????????????????????????????
                paramMap.put(param.getName(), value);
            } else {
                paramMap.put(param.getName(), specialSpecMap.get(param.getId().toString()));
            }

        });

        // ????????????
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
        String result = "??????";
        // ???????????????
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // ??????????????????
            double begin = org.apache.commons.lang3.math.NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = org.apache.commons.lang3.math.NumberUtils.toDouble(segs[1]);
            }
            // ????????????????????????
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "??????";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "??????";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * ??????
     * @param request
     * @return
     */
    @Override
    public SearchResult search(SearchRequest request) {

        String key = request.getKey();
        // ?????????????????????????????????????????????????????????null??????????????????????????????
        if (StringUtils.isBlank(key)) {
            return null;
        }

        // ??????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 1??????key????????????????????????
        QueryBuilder basicQuery = QueryBuilders.matchQuery("all", key).operator(Operator.AND);
        queryBuilder.withQuery(basicQuery);

        // 2?????????sourceFilter???????????????????????????,???????????????id???skus???subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));

        // 3?????????
        // ??????????????????
        int page = request.getPage();
        int size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page - 1, size));

        String categoryAggName = "category";
        String brandAggName = "brand";
        //???????????????????????????
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //?????????????????????
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        //?????????????????????
        AggregatedPage<Goods> pageInfo = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());
        List<Map<String,Object>> categories = getCategoryAggResult(pageInfo.getAggregation(categoryAggName));
        List<Brand> brands = getBrandAggResult(pageInfo.getAggregation(brandAggName));

        //????????????
         /* //??????????????????
        List<Map<String,Object>> specs = null;
        if (CollectionUtils.isEmpty(categories) || categories.size() == 1){
            //???????????????????????????????????????????????????????????????????????????????????????
            specs = getSpecAggResult((Long) categories.get(0).get("id"),basicQuery);
        }*/

        // ?????????????????????
        return new SearchResult(pageInfo.getTotalElements(), (long) pageInfo.getTotalPages(), pageInfo.getContent(), categories ,brands);
    }


    /**
     * ????????????
     * @param id
     */
    @Override
    public void createIndex(Long id) throws IOException {
        SpuBo spuBo = this.goodsClient.queryGoodsById(id);
        //????????????
        Goods goods = this.buildGoods(spuBo);

        //???????????????????????????
        this.goodsRepository.save(goods);
    }

    /**
     * ????????????
     * @param id
     */
    @Override
    public void deleteIndex(Long id) {

        this.goodsRepository.deleteById(id);
    }

    /**
     * ????????????????????????????????????
     * @param searchRequest
     * @return
     */
    private QueryBuilder buildBasicQueryWithFilter(SearchRequest searchRequest) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //??????????????????
        queryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));
        //?????????????????????
        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        //??????????????????
        Map<String,String> filter = searchRequest.getFilter();
        for (Map.Entry<String,String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String regex = "^(\\d+\\.?\\d*)-(\\d+\\.?\\d*)$";
            if (!"key".equals(key)) {
                if ("price".equals(key)){
                    if (!value.contains("?????????")) {
                        String[] nums = StringUtils.substringBefore(value, "???").split("-");
                        filterQueryBuilder.must(QueryBuilders.rangeQuery(key).gte(Double.valueOf(nums[0]) * 100).lt(Double.valueOf(nums[1]) * 100));
                    }else {
                        String num = StringUtils.substringBefore(value,"?????????");
                        filterQueryBuilder.must(QueryBuilders.rangeQuery(key).gte(Double.valueOf(num)*100));
                    }
                }else {
                    if (value.matches(regex)) {
                        Double[] nums = NumberUtils.searchNumber(value, regex);
                        //??????????????????????????????   lt:??????  gte:????????????
                        filterQueryBuilder.must(QueryBuilders.rangeQuery("specs." + key).gte(nums[0]).lt(nums[1]));
                    } else {
                        //????????????????????????????????????
                        if (key != "cid3" && key != "brandId") {
                            key = "specs." + key + ".keyword";
                        }
                        //????????????????????????term??????
                        filterQueryBuilder.must(QueryBuilders.termQuery(key, value));
                    }
                }
            } else {
                break;
            }
        }
        //??????????????????
        queryBuilder.filter(filterQueryBuilder);
        return queryBuilder;
    }

    /**
     * ??????????????????
     * @param
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getSpecAggResult(Long cid, QueryBuilder basicQuery) {
        // ??????????????????
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
     * ????????????interval
     * @param id
     * @param keySet
     * @return
     */
    private Map<String, Double> getNumberInterval(Long id, Set<String> keySet) {
        Map<String,Double> numbericalSpecs = new HashMap<>();
        //??????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //?????????????????????
        queryBuilder.withQuery(QueryBuilders.termQuery("cid3",id.toString())).withSourceFilter(new FetchSourceFilter(new String[]{""},null)).withPageable(PageRequest.of(0,1));
        //??????stats???????????????,????????????avg???max???min???sum???count???
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
     * ??????????????????????????????sum??????interval
     * @param min
     * @param max
     * @param sum
     * @return
     */
    private double getInterval(double min, double max, Double sum) {
        //??????7?????????
        double interval = (max - min) / 6.0d;
        //?????????????????????
        if (sum.intValue() == sum){
            //????????????????????????????????????
            int length = StringUtils.substringBefore(String.valueOf(interval),".").length();
            double factor = Math.pow(10.0,length - 1);
            return Math.round(interval / factor)*factor;
        }else {
            //????????????????????????????????????
            return NumberUtils.scale(interval,1);
        }
    }

    /**
     * ????????????????????????????????????????????????
     * @param strSpec
     * @param numericalInterval
     * @param numericalUnits
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> aggForSpec(Set<String> strSpec, Map<String, Double> numericalInterval, Map<String, String> numericalUnits, QueryBuilder basicQuery) {
        List<Map<String,Object>> specs = new ArrayList<>();
        //??????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        //??????????????????
        for (Map.Entry<String,Double> entry : numericalInterval.entrySet()) {
            queryBuilder.addAggregation(AggregationBuilders.histogram(entry.getKey()).field("specs." + entry.getKey()).interval(entry.getValue()).minDocCount(1));
        }
        //???????????????
        for (String key :strSpec){
            queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs."+key+".keyword"));
        }

        //??????????????????
        Map<String,Aggregation> aggregationMap = this.elasticsearchTemplate.query(queryBuilder.build(), SearchResponse :: getAggregations).asMap();

        //??????????????????
        for (Map.Entry<String,Double> entry :numericalInterval.entrySet()){
            Map<String,Object> spec = new HashMap<>();
            String key = entry.getKey();
            spec.put("k",key);
            spec.put("unit",numericalUnits.get(key));
            //??????????????????
            InternalHistogram histogram = (InternalHistogram) aggregationMap.get(key);
            spec.put("options",histogram.getBuckets().stream().map(bucket -> {
                Double begin = (Double) bucket.getKey();
                Double end = begin + numericalInterval.get(key);
                //???begin???end??????
                if (NumberUtils.isInt(begin) && NumberUtils.isInt(end)){
                    //??????????????????????????????
                    return begin.intValue() + "-" + end.intValue();
                }else {
                    //????????????2?????????
                    begin = NumberUtils.scale(begin,2);
                    end = NumberUtils.scale(end,2);
                    return begin + "-" + end;
                }
            }).collect(Collectors.toList()));
            specs.add(spec);
        }

        //?????????????????????
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
     * ????????????????????????
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        //??????????????????
        return terms.getBuckets().stream().map(bucket -> {
            return this.brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());

    }

    /**
     * ??????????????????????????????
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;
        //????????????????????????????????????List<Map<String,Object>>
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
     * ????????????????????????
     * @param queryBuilder
     * @param request
     */
    private void searchWithPageAndSort(NativeSearchQueryBuilder queryBuilder, SearchRequest request) {
        // ??????????????????
        int page = request.getPage();
        int size = request.getDefaultSize();

        // 1?????????
        queryBuilder.withPageable(PageRequest.of(page - 1, size));
        // 2?????????
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if (StringUtils.isNotBlank(sortBy)) {
            // ?????????????????????????????????
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc ? SortOrder.DESC : SortOrder.ASC));
        }
    }
}
