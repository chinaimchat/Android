package com.chat.wallet.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 单条充值渠道，对应<b>用户端</b> {@code GET /v1/wallet/recharge/channels}（{@link com.chat.wallet.api.WalletService#getRechargeChannels}）返回的 {@code list}/{@code data} 数组元素。
 * <p>标准字段：{@link #pay_address}、{@link #qr_image_url}、{@link #icon}。{@code qr_image_url}/{@code icon} 须用接口返回的<b>完整 URL</b>，勿自拼文件域名、勿改用对象存储直链；约定见 {@link com.chat.wallet.api.WalletService#getRechargeChannels} 文档。
 * 勿使用管理端 {@code /v1/manager/pay/recharge-channels} 等接口的数据结构冒充用户端响应。</p>
 * <p>收款码图 URL 汇总见 {@link #getDepositQrImageUrlOrEmpty()}；无图时客户端按 {@link #getDepositAddressForDisplay()} 本地生成二维码。</p>
 */
public class RechargeChannel {
    public long id;
    public String title;
    public String name;
    public String channel_name;
    public String type;
    /**
     * 与库表 recharge_channel.pay_type 一致：2 支付宝 3 微信 4 U盾。
     * 使用 Object 以便 Fastjson 同时兼容 JSON 数字与字符串。
     */
    @JSONField(name = "pay_type")
    public Object pay_type;
    public String pay_type_name;
    public String icon;
    public String description;
    public double min_amount;
    public double max_amount;
    public int status;

    /** 提币手续费（USDT）；未下发时客户端默认 2。 */
    @JSONField(name = "withdraw_fee")
    public Double withdraw_fee;

    /** 该渠道关联的客服 UID（用于区块内「联系客服」） */
    @JSONField(name = "customer_service_uid")
    public String customer_service_uid;

    @JSONField(name = "customer_service_name")
    public String customer_service_name;

    @JSONField(name = "customer_service_desc")
    public String customer_service_desc;

    /**
     * 管理端「备注」；链上充值场景下后台可将收款地址填在此字段，客户端展示/二维码优先使用。
     */
    @JSONField(name = "remark")
    public String remark;

    /** U 币等线下转账类：展示用收款/充值地址（兼容旧接口，优先级低于 {@link #remark}） */
    @JSONField(name = "recharge_deposit_address")
    public String recharge_deposit_address;

    /**
     * 标准字段：收款/链上地址。管理端「链上地址」可能映射为 pay_address、chain_address 等。
     */
    @JSONField(name = "pay_address", alternateNames = {
            "chain_address", "on_chain_address", "chain_on_address", "deposit_address",
            "wallet_address", "receive_address", "collection_address", "recharge_address"
    })
    public String pay_address;

    /**
     * 管理端为 USDT/U 盾等上传的收款二维码图片 URL（与地址二选一或并存；有则客户端优先展示该图）。
     * 兼容多种后端字段名，见 {@link #getDepositQrImageUrlOrEmpty()}。
     */
    @JSONField(name = "qrcode_url", alternateNames = {
            "pay_qrcode_url", "payment_qrcode_url", "receive_qrcode_url", "receipt_qrcode_url",
            "collect_qrcode_url", "payment_qr_url", "recharge_qrcode_url", "qrcode_pic", "qr_pic_url"
    })
    public String qrcode_url;
    @JSONField(name = "qrcode_image", alternateNames = {"qrcode_img", "qr_image"})
    public String qrcode_image;
    @JSONField(name = "qr_code_url")
    public String qr_code_url;
    @JSONField(name = "recharge_qrcode")
    public String recharge_qrcode;
    @JSONField(name = "qr_image_url", alternateNames = {
            "qrcode_image_url", "pay_qrcode_image_url", "channel_qrcode_image_url",
            "recharge_qrcode_image_url", "upload_qrcode_url", "pay_channel_qrcode_url"
    })
    public String qr_image_url;
    /**
     * 多数后台把「扫码内容」放在此字段（链上地址等），与图片 URL 无关；{@link #getDepositQrImageUrlOrEmpty()} 仅在其像 URL 时才当作图链。
     */
    @JSONField(name = "qr_code")
    public String qr_code;
    @JSONField(name = "deposit_qr_url")
    public String deposit_qr_url;

    /**
     * 管理端上传的收款码图 URL（支付配置「图片」等）；与 {@link #icon} 区分。
     * 不限定 pay_type：微信/支付宝若只配了该字段也应能展示。
     */
    @JSONField(name = "image", alternateNames = {"pay_image", "upload_image", "channel_image", "payment_image", "qrcode_file"})
    public String image;

    /**
     * 后台约定：仅当 {@link #getPayTypeInt()}==4（U盾）时，{@code install_key} 表示汇率（如 "7.2" 即 1U=7.2 元）；
     * 其它支付类型下为业务配置，勿当作汇率。
     */
    @JSONField(name = "install_key")
    public String install_key;

    /**
     * 可选扩展字段；若已用 {@link #install_key} 作汇率则可不填。
     */
    @JSONField(name = "exchange_rate")
    public String exchange_rate;

    /**
     * 管理端是否启用该渠道。常见约定：1=启用，2=停用；未返回或为 0 时视为启用（兼容旧接口）。
     * TODO: 若后端对「单独开启 U 币」有独立字段，在此解析并与 {@link #status} 组合判断。
     */
    public boolean isChannelEnabled() {
        return status != 2;
    }

    public String getDisplayName() {
        if (title != null && !title.isEmpty()) return title;
        if (channel_name != null && !channel_name.isEmpty()) return channel_name;
        if (name != null && !name.isEmpty()) return name;
        return "";
    }

    public String getPayTypeName() {
        if (pay_type_name != null && !pay_type_name.isEmpty()) {
            return pay_type_name;
        }
        int code = getPayTypeInt();
        if (code == 2) {
            return "支付宝";
        }
        if (code == 3) {
            return "微信";
        }
        if (code == 4) {
            return "U盾";
        }
        if (pay_type instanceof String) {
            String s = ((String) pay_type).trim();
            return s.isEmpty() ? "" : s;
        }
        return "";
    }

    /**
     * 解析 pay_type 为整数，无法解析时返回 -1。
     */
    public int getPayTypeInt() {
        if (pay_type == null) {
            return -1;
        }
        if (pay_type instanceof Number) {
            return ((Number) pay_type).intValue();
        }
        if (pay_type instanceof String) {
            String s = ((String) pay_type).trim();
            if (s.isEmpty()) {
                return -1;
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public boolean hasCustomerServiceUid() {
        return customer_service_uid != null && !customer_service_uid.trim().isEmpty();
    }

    /**
     * 从本渠道构造区块客服展示模型（无 uid 时返回 null）。
     */
    public RechargeBlockCustomer toBlockCustomerOrNull() {
        if (!hasCustomerServiceUid()) {
            return null;
        }
        RechargeBlockCustomer c = new RechargeBlockCustomer();
        c.uid = customer_service_uid.trim();
        c.name = customer_service_name;
        c.description = customer_service_desc;
        return c;
    }

    /**
     * 链上/收款展示用地址。
     * <p>后台常把「链上地址」放在 {@link #pay_address} 或 {@link #qr_code}（扫码内容，非图片 URL）；二者都兼容。</p>
     */
    public String getDepositAddressForDisplay() {
        if (pay_address != null && !pay_address.trim().isEmpty()) {
            return pay_address.trim();
        }
        if (remark != null && !remark.trim().isEmpty()) {
            return remark.trim();
        }
        if (recharge_deposit_address != null && !recharge_deposit_address.trim().isEmpty()) {
            return recharge_deposit_address.trim();
        }
        if (qr_code != null && !qr_code.trim().isEmpty()) {
            String t = qr_code.trim();
            if (!looksLikeQrImageUrlOrPath(t)) {
                return t;
            }
        }
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return "";
    }

    /**
     * 充值页二维码图片来源：与 {@code GET /v1/wallet/recharge/channels} 一致；无则返回空串（客户端再按地址生成二维码）。
     * <p>标准字段 {@link #qr_image_url} 优先（已是完整 URL）；其它兼容字段兜底。</p>
     */
    public String getDepositQrImageUrlOrEmpty() {
        if (qr_image_url != null && !qr_image_url.trim().isEmpty()) {
            return qr_image_url.trim();
        }
        // 管理端「上传图片」常见只写入 image
        if (image != null && !image.trim().isEmpty()) {
            return image.trim();
        }
        if (qrcode_url != null && !qrcode_url.trim().isEmpty()) {
            return qrcode_url.trim();
        }
        if (qrcode_image != null && !qrcode_image.trim().isEmpty()) {
            return qrcode_image.trim();
        }
        if (qr_code_url != null && !qr_code_url.trim().isEmpty()) {
            return qr_code_url.trim();
        }
        if (recharge_qrcode != null && !recharge_qrcode.trim().isEmpty()) {
            return recharge_qrcode.trim();
        }
        if (deposit_qr_url != null && !deposit_qr_url.trim().isEmpty()) {
            return deposit_qr_url.trim();
        }
        if (qr_code != null && !qr_code.trim().isEmpty()) {
            String t = qr_code.trim();
            if (looksLikeQrImageUrlOrPath(t)) {
                return t;
            }
        }
        if (getPayTypeInt() == 4 && icon != null && !icon.trim().isEmpty()) {
            return icon.trim();
        }
        return "";
    }

    /** 后台常把链上地址写在 {@code qr_code}，仅当明显是图片地址时才用于 Glide。 */
    private static boolean looksLikeQrImageUrlOrPath(String t) {
        if (t == null || t.isEmpty()) {
            return false;
        }
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return true;
        }
        if (t.startsWith("data:image")) {
            return true;
        }
        return t.startsWith("/") && t.length() > 1;
    }

    /**
     * U盾（pay_type=4）下用于 UI「当前汇率」展示：优先 {@link #install_key}，其次 {@link #exchange_rate}。
     */
    public String getUcoinRateDisplayForLabel() {
        if (getPayTypeInt() != 4) {
            return null;
        }
        if (install_key != null && !install_key.trim().isEmpty()) {
            return install_key.trim();
        }
        if (exchange_rate != null && !exchange_rate.trim().isEmpty()) {
            return exchange_rate.trim();
        }
        return null;
    }

    /**
     * U盾汇率数值（优先 {@link #install_key}，否则 {@link #exchange_rate}）；无效时返回 NaN。
     */
    public double getUcoinRateMultiplier() {
        if (getPayTypeInt() != 4) {
            return Double.NaN;
        }
        String raw = null;
        if (install_key != null && !install_key.trim().isEmpty()) {
            raw = install_key.trim();
        } else if (exchange_rate != null && !exchange_rate.trim().isEmpty()) {
            raw = exchange_rate.trim();
        }
        if (raw == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * 买币页参考：每 1 USDT 对应的人民币（元），与 {@code GET /wallet/recharge/channels} 后台配置一致。
     * <ul>
     *   <li>U 盾（pay_type=4）：同 {@link #getUcoinRateMultiplier()}（优先 {@link #install_key}，再 {@link #exchange_rate}）。</li>
     *   <li>微信/支付宝等：不解析 {@link #install_key}；若配置了 {@link #exchange_rate} 则按「元/USDT」解析。</li>
     * </ul>
     */
    public double getCnyPerUsdtForBuyPageOrNaN() {
        if (getPayTypeInt() == 4) {
            return getUcoinRateMultiplier();
        }
        if (exchange_rate != null && !exchange_rate.trim().isEmpty()) {
            try {
                double v = Double.parseDouble(exchange_rate.trim());
                return v > 0 ? v : Double.NaN;
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    public double getWithdrawFeeUsdtOrDefault() {
        if (withdraw_fee != null && withdraw_fee >= 0 && !withdraw_fee.isNaN()) {
            return withdraw_fee;
        }
        return 2.0;
    }

    /** 提币最小数量；后台 {@link #min_amount} 无效时默认 4。 */
    public double getMinWithdrawUsdtOrDefault() {
        if (min_amount > 0) {
            return min_amount;
        }
        return 4.0;
    }
}
