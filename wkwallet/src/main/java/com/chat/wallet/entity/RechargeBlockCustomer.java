package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 充值页各区块展示的客服信息（可由渠道列表项上的字段组装，或将来由独立配置接口解析为本类）。
 * <p>
 * TODO(后端文档待对齐): JSON 字段名、是否与 {@link RechargeChannel} 平铺字段合并或改为嵌套对象，以实际接口为准。
 */
public class RechargeBlockCustomer {

    /**
     * 占位：与后端约定后请改 {@link JSONField#name()}。
     */
    @JSONField(name = "customer_uid")
    public String uid;

    @JSONField(name = "customer_name")
    public String name;

    @JSONField(name = "customer_desc")
    public String description;

    public boolean hasUid() {
        return uid != null && !uid.trim().isEmpty();
    }
}
