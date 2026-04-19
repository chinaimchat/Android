package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * GET /v1/wallet/recharge/applications 列表单项；字段名兼容常见后台命名。
 */
public class RechargeApplicationRecord {
    @JSONField(name = "application_no")
    public String applicationNo;
    public Double amount;
    @JSONField(name = "amount_u")
    public Double amountU;
    /** 审核状态：0 待审 1 通过 2 拒绝（与提现等常见约定一致） */
    @JSONField(name = "audit_status")
    public Integer auditStatus;
    @JSONField(name = "recharge_status")
    public Integer rechargeStatus;
    public Integer status;
    @JSONField(name = "admin_remark")
    public String adminRemark;
    @JSONField(name = "created_at")
    public String createdAt;

    public String getApplicationNo() {
        return applicationNo != null ? applicationNo : "";
    }

    /**
     * 解析审核状态；无法识别时按待审核(0)处理。
     */
    public int resolveAuditStatus() {
        if (auditStatus != null) {
            return auditStatus;
        }
        if (rechargeStatus != null) {
            return rechargeStatus;
        }
        if (status != null) {
            return status;
        }
        return 0;
    }
}
