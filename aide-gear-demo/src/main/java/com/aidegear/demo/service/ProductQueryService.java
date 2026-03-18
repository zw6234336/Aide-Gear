package com.aidegear.demo.service;

import com.aidegear.common.annotation.AiAbility;
import com.aidegear.common.annotation.AiAction;
import com.aidegear.common.annotation.AiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例：产品查询服务 - 展示纯 CONVERSATION 来源的参数使用方式。
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Service
@AiAbility(name = "产品查询服务", description = "提供保险产品信息查询能力")
public class ProductQueryService {

    @AiAction(name = "查询产品详情", desc = "根据产品编码查询产品详细信息",
              returnDesc = "返回产品名称、保费、保障范围等详细信息")
    public Map<String, Object> getProductDetail(
            @AiParam(value = "产品编码", example = "RISK001") String riskCode
    ) {
        log.info("查询产品详情: {}", riskCode);

        Map<String, Object> product = new HashMap<>();
        product.put("riskCode", riskCode);
        product.put("productName", "某某终身寿险");
        product.put("minPremium", 1000);
        product.put("maxAge", 65);
        product.put("coverageDesc", "提供身故/全残保障");
        return product;
    }

    @AiAction(name = "产品对比", desc = "对比两个产品的核心差异")
    public Map<String, Object> compareProducts(
            @AiParam(value = "产品A编码", example = "RISK001") String riskCodeA,
            @AiParam(value = "产品B编码", example = "RISK002") String riskCodeB
    ) {
        log.info("对比产品: {} vs {}", riskCodeA, riskCodeB);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("productA", riskCodeA);
        comparison.put("productB", riskCodeB);
        comparison.put("priceDiff", "产品A更优惠");
        comparison.put("coverageDiff", "产品B保障范围更广");
        return comparison;
    }
}
