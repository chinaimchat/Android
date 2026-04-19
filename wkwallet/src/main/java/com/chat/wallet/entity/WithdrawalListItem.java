package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 用户端 {@code GET /v1/wallet/withdrawal/list} 列表项（与管理端列表字段对齐的常见子集）。
 */
public class WithdrawalListItem {
    public Long id;
    @JSONField(name = "withdrawal_no", alternateNames = {"withdrawalNo"})
    public String withdrawalNo;
    public String uid;
    public Double amount;
    public Double fee;
    /**
     * 到账金额（USDT）= 申请数量 - 手续费；旧字段 {@code total_freeze} 仅作兼容解析。
     */
    @JSONField(name = "actual_amount", alternateNames = {"actualAmount", "total_freeze", "totalFreeze"})
    public Double actual_amount;
    public Integer status;
    @JSONField(name = "withdrawal_status", alternateNames = {"withdrawalStatus"})
    public Integer withdrawal_status;
    @JSONField(name = "status_text", alternateNames = {"statusText"})
    public String statusText;
    public String address;
    public String remark;
    @JSONField(name = "admin_remark", alternateNames = {"adminRemark"})
    public String adminRemark;
    @JSONField(name = "created_at", alternateNames = {"createdAt"})
    public String createdAt;
    @JSONField(name = "updated_at", alternateNames = {"updatedAt"})
    public String updatedAt;

    /** 审核状态：0 待审 1 通过 2 拒绝；缺省按待审。 */
    public int resolveAuditStatus() {
        if (status != null) {
            return status;
        }
        if (withdrawal_status != null) {
            return withdrawal_status;
        }
        return 0;
    }
}
