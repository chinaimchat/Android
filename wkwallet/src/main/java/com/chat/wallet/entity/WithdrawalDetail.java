package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 用户端 {@code GET /v1/wallet/withdrawal/detail/:withdrawal_no} 详情。
 * 兼容根对象或 {@code data} 内嵌；{@code status} 与 {@code withdrawal_status} 均可表示审核状态（0 待审 1 通过 2 拒绝）。
 */
public class WithdrawalDetail {
    public Long id;
    @JSONField(name = "withdrawal_no", alternateNames = {"withdrawalNo"})
    public String withdrawal_no;
    public Double amount;
    public Double fee;
    @JSONField(name = "actual_amount", alternateNames = {"actualAmount", "total_freeze", "totalFreeze"})
    public Double actual_amount;
    /**
     * 审核状态：0 待审 1 通过 2 拒绝。接口也可能只返回字段名 {@code status}（在已解包后的详情对象上）。
     */
    @JSONField(name = "withdrawal_status", alternateNames = {"status"})
    public Integer withdrawal_status;
    @JSONField(name = "status_text", alternateNames = {"statusText"})
    public String status_text;
    public String uid;
    public String address;
    public String remark;
    @JSONField(name = "admin_remark", alternateNames = {"adminRemark"})
    public String admin_remark;
    @JSONField(name = "created_at", alternateNames = {"createdAt"})
    public String created_at;
    @JSONField(name = "updated_at", alternateNames = {"updatedAt"})
    public String updated_at;

    /** UI 用审核状态：0 待审 1 通过 2 拒绝；缺省 0 */
    public int resolveWithdrawalStatus() {
        if (withdrawal_status != null) {
            return withdrawal_status;
        }
        return 0;
    }
}
