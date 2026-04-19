package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * GET /v1/manager/wallet/customer_service/list 列表项；兼容常见字段命名。
 */
public class CustomerService {
    public long id;
    @JSONField(alternateNames = {"user_uid", "user_id", "userId"})
    public String uid;
    @JSONField(alternateNames = {"nickname", "user_name", "userName"})
    public String name;
    @JSONField(alternateNames = {"avatar_url", "avatarUrl"})
    public String avatar;
    @JSONField(alternateNames = {"desc", "remark"})
    public String description;
}
