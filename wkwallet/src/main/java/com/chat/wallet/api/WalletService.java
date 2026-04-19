package com.chat.wallet.api;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.net.entity.CommonResponse;
import com.chat.wallet.entity.*;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.*;

public interface WalletService {
    @GET("wallet/balance")
    Observable<WalletBalanceResp> getBalance();

    @POST("wallet/password")
    Observable<CommonResponse> setPayPassword(@Body JSONObject body);

    @PUT("wallet/password")
    Observable<CommonResponse> changePayPassword(@Body JSONObject body);

    /**
     * 用户端钱包交易记录。完整路径：{@code GET /v1/wallet/transactions}；需登录（与其它钱包接口相同 Token 鉴权）。
     * 可选查询：{@code start_date}、{@code end_date}（常见格式 {@code yyyy-MM-dd HH:mm:ss}）；不传则不限时间。
     * 响应体由 {@link WalletModel} 解析，兼容根数组或 {@code data/list/items} 等常见封装。
     */
    @GET("wallet/transactions")
    Observable<ResponseBody> getTransactions(
            @Query("page") int page,
            @Query("size") int size,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate);

    /**
     * 旧接口：直接加余额，客户端勿用；须走 {@link #rechargeApply} 审核流程。
     */
    @POST("wallet/recharge")
    Observable<CommonResponse> recharge(@Body JSONObject body);

    /**
     * 提交充值申请；到账金额以响应为准（与后台审核流程对齐）。
     * 完整路径：{@code POST /v1/wallet/recharge/apply}
     * <p>
     * Body 约定：U 盾为 {@code channel_id} + {@code amount_u} + 可选 {@code remark} + {@code proof_url}；
     * 非 U 盾（元）为 {@code amount} + {@code channel_id} + 可选 {@code remark}。鉴权与其它钱包接口相同（Token）。
     */
    @POST("wallet/recharge/apply")
    Observable<RechargeApplyResp> rechargeApply(@Body JSONObject body);

    /**
     * 充值申请记录列表。完整路径：{@code GET /v1/wallet/recharge/applications}
     */
    @GET("wallet/recharge/applications")
    Observable<ResponseBody> getRechargeApplications(@Query("page") int page, @Query("size") int size);

    @POST("redpacket/send")
    Observable<RedPacketSendResp> sendRedPacket(@Body JSONObject body);

    @POST("redpacket/open")
    Observable<RedPacketOpenResp> openRedPacket(@Body JSONObject body);

    @GET("redpacket/{packet_no}")
    Observable<RedPacketDetailResp> getRedPacketDetail(@Path("packet_no") String packetNo);

    /**
     * 用户申请提现。完整路径：{@code POST /v1/wallet/withdrawal/apply}。
     * Body：{@code amount}、{@code password}、{@code address}、{@code channel_id} 等。
     * <p><b>响应</b>：{@code actual_amount} 为到账金额（≈ amount - fee）；旧字段名 {@code total_freeze} 已废弃。</p>
     * <p><b>资金语义</b>：成功受理后服务端冻结本单；拒绝或超时解冻退回；通过后扣减并完成出账。见 {@link com.chat.wallet.entity.WithdrawApplyResp}。</p>
     */
    @POST("wallet/withdrawal/apply")
    Observable<WithdrawApplyResp> withdrawalApply(@Body JSONObject body);

    /**
     * 用户提现记录列表（分页）。完整路径：{@code GET /v1/wallet/withdrawal/list}。
     * 响应常见形态：{@code { "list": [], "total", "page", "size" }}，见 {@link com.chat.wallet.api.WalletModel#parseWithdrawalListJson(String)}。
     */
    @GET("wallet/withdrawal/list")
    Observable<ResponseBody> getWithdrawalList(@Query("page") int page, @Query("size") int size);

    /**
     * 提币手续费与最小提币额等。完整路径：{@code GET /v1/wallet/withdrawal/fee-config}。
     * 可选 {@code channel_id}：与当前选中的链上渠道 id 一致时服务端可返回该链配置。
     */
    @GET("wallet/withdrawal/fee-config")
    Observable<ResponseBody> getWithdrawalFeeConfig(@Query("channel_id") Long channelId);

    /**
     * 按提币数量预览手续费与到账。完整路径：{@code GET /v1/wallet/withdrawal/fee-preview?amount=…}。
     * 金额以字符串传递，避免浮点编码精度问题；可选 {@code channel_id} 与当前链一致。
     */
    @GET("wallet/withdrawal/fee-preview")
    Observable<ResponseBody> getWithdrawalFeePreview(
            @Query("amount") String amount,
            @Query("channel_id") Long channelId);

    /**
     * <b>用户端</b>：获取已启用的充值渠道列表（含链上地址、收款码图 URL、渠道图标等）。
     * <ul>
     *   <li>方法/路径：{@code GET /v1/wallet/recharge/channels}（Retrofit {@code baseUrl} 已含末尾 {@code /v1/}）</li>
     *   <li>鉴权：需登录态，与其它用户接口一致（Header {@code token}）</li>
     *   <li>响应：{@code {"status":200,"list":[...]}}；{@code list} 与 {@code data} 可并存且内容相同，客户端兼容解析其一即可（见 {@link com.chat.wallet.api.WalletModel#parseRechargeChannelListJson}）</li>
     * </ul>
     *
     * <p><b>前端接口约定（重点）</b></p>
     * <ul>
     *   <li>{@code qr_image_url}：展示收款码图时<b>必须使用接口返回的完整 URL</b>；不要自行拼接文件域名，不要改用库里原始对象存储（如 momo-file）直链。</li>
     *   <li>{@code icon}：渠道图标同样使用返回的 {@code icon}（与二维码字段一并规范化后的值）。</li>
     *   <li>加载图片：目标形态为「与 App {@code baseUrl} 同域或可访问的 API 主机」+ {@code /v1/file/preview/...}，一般为 <b>HTTP 200 + 图片流</b>。
     *       {@code /v1/file/preview/...} 路由可不挂鉴权（无 token 也可访问）。全走 preview 时通常无预签名链。</li>
     *   <li>请求最终图片 URL 时：<b>不要强行加业务头</b>（{@code token}、{@code appid} 等）；若用 WebView / 自定义 Client，避免改 {@code Host}、乱加 query，以免破坏预签名（已全走 preview 时一般无此问题）。</li>
     *   <li>本工程 OkHttp：与 TangSengDaoDaoAndroid 官网一致，{@link com.chat.base.net.CommonRequestParamInterceptor} 对主 Client <b>所有</b>请求附加业务头。{@link com.chat.base.net.OkHttpUtils#fetchGatewayThenBareRedirect} 仅用于部分预览类 GET 字节拉取，禁止 {@code file/upload} 入口。</li>
     *   <li>若使用 Glide/Coil 等带磁盘缓存：后端升级 URL 规则后建议<b>清缓存</b>或依赖 URL 变更触发重新拉取，避免仍命中旧响应里的对象存储直链。</li>
     * </ul>
     *
     * <p><b>勿在用户端调用管理端充值配置接口</b>（需管理员 token），例如：
     * {@code GET /v1/manager/pay/recharge-channels}、{@code GET /v1/manager/pay/recharge-channels/:id} 及其它 {@code /v1/manager/...} 中与后台表单对应的接口。
     * </p>
     */
    @GET("wallet/recharge/channels")
    Observable<ResponseBody> getRechargeChannels();

    /**
     * 钱包客服列表。完整路径：{@code GET /v1/manager/wallet/customer_service/list}；鉴权与其它接口相同（Token）。
     * 响应体由 {@link WalletModel} 解析，兼容根数组或 {@code data/list/items} 等封装。
     */
    @GET("manager/wallet/customer_service/list")
    Observable<ResponseBody> getCustomerServices();

    /**
     * 管理端提现列表。完整路径：{@code GET /v1/manager/wallet/withdrawal/list}；需管理端登录 Token。
     * 分页参数若后台未实现可忽略，由服务端决定默认行为。
     */
    @GET("manager/wallet/withdrawal/list")
    Observable<ResponseBody> getManagerWithdrawalList(@Query("page") int page, @Query("size") int size);

    /**
     * 管理端通过提现。完整路径：{@code POST /v1/manager/wallet/withdrawal/approve}。
     * Body：{@code id} 为列表接口返回的内部记录 id；{@code remark} 可选，可为空字符串。
     * 业务：仅 {@code status == 0}（待审核）可审，否则后台返回「该提现已处理，无法重复审核」。
     */
    @POST("manager/wallet/withdrawal/approve")
    Observable<CommonResponse> approveManagerWithdrawal(@Body JSONObject body);

    /**
     * 管理端拒绝提现。完整路径：{@code POST /v1/manager/wallet/withdrawal/reject}。
     * Body：{@code id} 必填；{@code remark} 必填（拒绝原因）。拒绝后后台应退回提现金额+手续费并记流水 {@code withdrawal_refund}。
     */
    @POST("manager/wallet/withdrawal/reject")
    Observable<CommonResponse> rejectManagerWithdrawal(@Body JSONObject body);

    /**
     * 用户提现详情。完整路径：{@code GET /v1/wallet/withdrawal/detail/:withdrawal_no}。
     * 使用 {@link ResponseBody} 以便兼容根对象或 {@code data} 包装。
     */
    @GET("wallet/withdrawal/detail/{withdrawal_no}")
    Observable<ResponseBody> getWithdrawalDetail(@Path("withdrawal_no") String withdrawalNo);

    /**
     * 发起转账。完整路径：{@code POST /v1/transfer/send}。
     * 请求体与后台 tfSend 一致：{@code to_uid}、{@code amount}、{@code remark}、{@code password}、{@code channel_id}、{@code channel_type}；
     * {@code pay_scene} 仅在对应场景携带（如收款码为 {@code receive_qr}）。
     */
    @POST("transfer/send")
    Observable<TransferSendResp> sendTransfer(@Body JSONObject body);

    @POST("transfer/{transfer_no}/accept")
    Observable<TransferSendResp> acceptTransfer(@Path("transfer_no") String transferNo);

    @GET("transfer/{transfer_no}")
    Observable<TransferDetailResp> getTransferDetail(@Path("transfer_no") String transferNo);
}
