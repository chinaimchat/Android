package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * GET /v1/manager/wallet/withdrawal/list 列表项。
 * 审核接口使用的 {@code id} 为列表返回的内部主键，非 {@code withdrawal_no}。
 */
public class ManagerWithdrawalRecord {
    public Long id;
    @JSONField(name = "withdrawal_no")
    public String withdrawalNo;
    /**
     * 0 待审核；非 0 表示已处理（具体取值以后台为准）。
     */
    public Integer status;
    @JSONField(name = "withdrawal_status")
    public Integer withdrawalStatus;
    public Double amount;
    @JSONField(name = "handling_fee")
    public Double handlingFee;
    @JSONField(name = "fee")
    public Double fee;
    @JSONField(name = "actual_amount", alternateNames = {"actualAmount", "total_freeze", "totalFreeze"})
    public Double actual_amount;
    public String uid;
    @JSONField(name = "user_uid")
    public String userUid;
    @JSONField(name = "admin_remark")
    public String adminRemark;
    @JSONField(name = "created_at")
    public String createdAt;

    public long getId() {
        return id != null ? id : 0L;
    }

    public int resolveStatus() {
        if (status != null) {
            return status;
        }
        if (withdrawalStatus != null) {
            return withdrawalStatus;
        }
        return 0;
    }
}
