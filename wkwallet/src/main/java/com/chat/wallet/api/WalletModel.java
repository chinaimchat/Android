package com.chat.wallet.api;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.WKBaseModel;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.wallet.entity.*;
import com.chat.wallet.util.RechargeChannelsCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;

public class WalletModel extends WKBaseModel {
    private WalletModel() {}
    private static class H { static final WalletModel I = new WalletModel(); }
    public static WalletModel getInstance() { return H.I; }

    public void getBalance(IRequestResultListener<WalletBalanceResp> l) {
        request(createService(WalletService.class).getBalance(), l);
    }
    public void setPayPassword(String pwd, IRequestResultListener<CommonResponse> l) {
        JSONObject j = new JSONObject();
        j.put("password", pwd);
        request(createService(WalletService.class).setPayPassword(j), l);
    }
    public void changePayPassword(String oldPwd, String newPwd, IRequestResultListener<CommonResponse> l) {
        JSONObject j = new JSONObject();
        j.put("old_password", oldPwd);
        j.put("new_password", newPwd);
        request(createService(WalletService.class).changePayPassword(j), l);
    }
    public void getTransactions(int page, int size, IRequestResultListener<List<TransactionRecord>> l) {
        getTransactions(page, size, null, null, l);
    }

    /**
     * @param startDate 起始时间字符串（如 {@code yyyy-MM-dd HH:mm:ss}），可为 null
     * @param endDate   结束时间字符串，可为 null
     */
    public void getTransactions(int page, int size, String startDate, String endDate,
                                IRequestResultListener<List<TransactionRecord>> l) {
        Observable<List<TransactionRecord>> obs = createService(WalletService.class)
                .getTransactions(page, size, startDate, endDate)
                .map(WalletModel::parseTransactionRecordListFromBody);
        request(obs, l);
    }

    /**
     * 兼容：根为 JSON 数组，或对象内 {@code data/list/items/rows/result/transactions/records} 为数组（与常见后台封装一致）。
     */
    static List<TransactionRecord> parseTransactionRecordListFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new ArrayList<>();
        }
        return parseTransactionRecordListJson(body.string());
    }

    static List<TransactionRecord> parseTransactionRecordListJson(String s) {
        return parseListFromCommonKeys(s, TransactionRecord.class,
                "data", "list", "items", "rows", "result", "transactions", "records");
    }

    /**
     * 旧接口 POST /wallet/recharge，直接入账；App 侧应使用 {@link #rechargeApply(RechargeChannel, double, String, String, IRequestResultListener)}。
     */
    @Deprecated
    public void recharge(String uid, double amount, IRequestResultListener<CommonResponse> l) {
        JSONObject j = new JSONObject();
        j.put("uid", uid); j.put("amount", amount);
        request(createService(WalletService.class).recharge(j), l);
    }

    /**
     * 提交充值申请，请求体与后台约定一致（鉴权由 Token，body 不含 uid）。
     * <ul>
     *   <li>微信/支付宝等（元）：{@code amount} + {@code channel_id} + {@code remark}</li>
     *   <li>U 盾（pay_type=4）：{@code amount_u} + {@code channel_id} + 可选 {@code remark} + {@code proof_url}</li>
     * </ul>
     */
    public void rechargeApply(RechargeChannel channel, double amount, IRequestResultListener<RechargeApplyResp> l) {
        rechargeApply(channel, amount, null, null, l);
    }

    public void rechargeApply(RechargeChannel channel, double amount, String remark, String proofUrlU,
                              IRequestResultListener<RechargeApplyResp> l) {
        if (channel == null) {
            l.onFail(-1, "no channel");
            return;
        }
        JSONObject j = buildRechargeApplyBody(channel, amount, remark, proofUrlU);
        request(createService(WalletService.class).rechargeApply(j), new IRequestResultListener<RechargeApplyResp>() {
            @Override
            public void onSuccess(RechargeApplyResp r) {
                dispatchRechargeApply(r, l);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    static JSONObject buildRechargeApplyBody(RechargeChannel channel, double amount, String remark, String proofUrlU) {
        JSONObject j = new JSONObject();
        j.put("channel_id", channel.id);
        boolean isUShield = channel.getPayTypeInt() == 4;
        if (isUShield) {
            j.put("amount_u", amount);
            if (!TextUtils.isEmpty(remark)) {
                j.put("remark", remark);
            }
            j.put("proof_url", TextUtils.isEmpty(proofUrlU) ? "" : proofUrlU);
        } else {
            j.put("amount", amount);
            j.put("remark", remark != null ? remark : "");
        }
        return j;
    }

    /**
     * 分页拉取充值申请记录，原始 JSON 由调用方解析（结构与后台列表接口一致）。
     */
    public void getRechargeApplications(int page, int size, IRequestResultListener<String> l) {
        Observable<String> obs = createService(WalletService.class).getRechargeApplications(page, size)
                .map(WalletModel::rechargeApplicationsBodyToString);
        request(obs, l);
    }

    /**
     * 分页拉取充值申请并解析为列表（按创建时间新→旧排序）。
     */
    public void getRechargeApplicationsList(int page, int size,
                                            IRequestResultListener<List<RechargeApplicationRecord>> l) {
        getRechargeApplications(page, size, new IRequestResultListener<String>() {
            @Override
            public void onSuccess(String json) {
                List<RechargeApplicationRecord> list = parseRechargeApplicationListJson(json);
                Collections.sort(list, (a, b) -> Long.compare(
                        parseCreatedAtMillisForSort(b), parseCreatedAtMillisForSort(a)));
                l.onSuccess(list);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    private static long parseCreatedAtMillisForSort(RechargeApplicationRecord r) {
        if (r == null || TextUtils.isEmpty(r.createdAt)) {
            return 0L;
        }
        Long v = tryParseCreatedAtMillis(r.createdAt.trim());
        return v != null ? v : 0L;
    }

    @Nullable
    private static Long tryParseCreatedAtMillis(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String p : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(raw);
                if (d != null) {
                    return d.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** 列表/详情展示用时间（与 {@link #tryParseCreatedAtMillis(String)} 一致）。 */
    public static String formatRechargeApplicationTimeForDisplay(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "—";
        }
        String t = raw.trim();
        Long ms = tryParseCreatedAtMillis(t);
        if (ms != null) {
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.CHINA);
            return out.format(new java.util.Date(ms));
        }
        return t;
    }

    private static String rechargeApplicationsBodyToString(ResponseBody body) {
        if (body == null) {
            return "";
        }
        try {
            return body.string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 {@link WalletService#getRechargeApplications(int, int)} 返回的 JSON 为列表。
     */
    public static List<RechargeApplicationRecord> parseRechargeApplicationListJson(String s) {
        return parseListFromCommonKeys(s, RechargeApplicationRecord.class,
                "data", "list", "items", "rows", "records", "result", "applications");
    }


    /**
     * 在充值申请列表中按单号查找记录（多页，最多 {@code maxPage} 页）。
     */
    public void fetchRechargeApplicationByApplicationNo(String applicationNo, int maxPage,
                                                        IRequestResultListener<RechargeApplicationRecord> l) {
        if (TextUtils.isEmpty(applicationNo)) {
            l.onFail(-1, "no application no");
            return;
        }
        tryFetchRechargeApplicationOnPage(applicationNo, 1, maxPage, l);
    }

    private void tryFetchRechargeApplicationOnPage(String applicationNo, int page, int maxPage,
                                                   IRequestResultListener<RechargeApplicationRecord> l) {
        if (page > maxPage) {
            l.onFail(-1, "not found");
            return;
        }
        getRechargeApplications(page, 50, new IRequestResultListener<String>() {
            @Override
            public void onSuccess(String json) {
                List<RechargeApplicationRecord> list = parseRechargeApplicationListJson(json);
                for (RechargeApplicationRecord r : list) {
                    if (applicationNo.equals(r.getApplicationNo())) {
                        l.onSuccess(r);
                        return;
                    }
                }
                if (list.isEmpty()) {
                    l.onFail(-1, "not found");
                    return;
                }
                tryFetchRechargeApplicationOnPage(applicationNo, page + 1, maxPage, l);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    private static void dispatchRechargeApply(RechargeApplyResp r, IRequestResultListener<RechargeApplyResp> l) {
        if (r == null) {
            l.onFail(-1, "empty response");
            return;
        }
        if (isWalletBizHttpError(r.status)) {
            l.onFail(r.status, !TextUtils.isEmpty(r.msg) ? r.msg : "操作失败");
            return;
        }
        l.onSuccess(r);
    }
    /**
     * 链上提币：{@code amount} 为 USDT 数量；{@code address}、{@code channelId} 与后台
     * {@code POST /v1/wallet/withdrawal/apply} 约定对齐（未配置渠道时可传 0 与空地址由服务端校验）。
     * <p>成功响应中 {@link WithdrawApplyResp#actual_amount}（到账）替代旧字段 {@code total_freeze}，见 {@link WithdrawApplyResp#getResolvedActualAmountUsdt()}。</p>
     */
    public void withdraw(double amount, String password, @Nullable String address, long channelId,
                         IRequestResultListener<WithdrawApplyResp> l) {
        JSONObject j = new JSONObject();
        j.put("amount", amount);
        j.put("password", password);
        if (!TextUtils.isEmpty(address)) {
            j.put("address", address.trim());
        }
        if (channelId > 0) {
            j.put("channel_id", channelId);
        }
        request(createService(WalletService.class).withdrawalApply(j), new IRequestResultListener<WithdrawApplyResp>() {
            @Override
            public void onSuccess(WithdrawApplyResp r) {
                dispatchWithdrawApply(r, l);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    private static void dispatchWithdrawApply(WithdrawApplyResp r, IRequestResultListener<WithdrawApplyResp> l) {
        if (r == null) {
            l.onFail(-1, "empty response");
            return;
        }
        if (isWalletBizHttpError(r.status)) {
            l.onFail(r.status, TextUtils.isEmpty(r.msg) ? "操作失败" : r.msg);
            return;
        }
        l.onSuccess(r);
    }
    /**
     * 充值渠道：先读内存快照（若有）立即回调主线程刷新 UI，再静默请求网络；仅当与快照不一致时再次回调，避免无意义重绘。
     * <p>切换账号后请 {@link com.chat.wallet.util.RechargeChannelsCache#clear()}。</p>
     */
    public void getRechargeChannels(IRequestResultListener<List<RechargeChannel>> l) {
        final List<RechargeChannel> cached = RechargeChannelsCache.getCopy();
        if (cached != null) {
            new Handler(Looper.getMainLooper()).post(() -> l.onSuccess(cached));
        }
        Observable<List<RechargeChannel>> obs = createService(WalletService.class).getRechargeChannels()
                .map(WalletModel::parseRechargeChannelListFromBody);
        request(obs, new IRequestResultListener<List<RechargeChannel>>() {
            @Override
            public void onSuccess(List<RechargeChannel> fresh) {
                List<RechargeChannel> f = fresh != null ? fresh : new ArrayList<>();
                boolean hadSnapshot = RechargeChannelsCache.hasSnapshot();
                boolean changed = RechargeChannelsCache.differsFromSnapshot(f);
                RechargeChannelsCache.put(f);
                if (!hadSnapshot || changed) {
                    List<RechargeChannel> out = RechargeChannelsCache.getCopy();
                    l.onSuccess(out != null ? out : f);
                }
            }

            @Override
            public void onFail(int c, String m) {
                if (cached == null) {
                    l.onFail(c, m);
                }
            }
        });
    }

    /**
     * {@code GET /v1/wallet/withdrawal/fee-config}。{@code channelId}≤0 时不带 {@code channel_id} 查询参数。
     * 成功但体无法解析时回调 {@code null}，由提币页回退到渠道 {@link RechargeChannel} 上的配置。
     */
    public void getWithdrawalFeeConfig(long channelId, IRequestResultListener<WithdrawalFeeConfig> l) {
        final long cid = channelId;
        Long q = cid > 0 ? cid : null;
        Observable<WithdrawalFeeConfig> obs = createService(WalletService.class)
                .getWithdrawalFeeConfig(q)
                .map(body -> parseWithdrawalFeeConfigFromBody(body, cid));
        request(obs, l);
    }

    private static WithdrawalFeeConfig parseWithdrawalFeeConfigFromBody(ResponseBody body, long channelId)
            throws IOException {
        if (body == null) {
            return null;
        }
        return WithdrawalFeeConfig.parse(body.string(), channelId);
    }

    /**
     * {@code GET /v1/wallet/withdrawal/fee-preview}。{@code amount} 为已 trim 的十进制字符串；{@code channelId}≤0 时不带 query。
     */
    public void getWithdrawalFeePreview(String amount, long channelId, IRequestResultListener<WithdrawalFeePreview> l) {
        if (TextUtils.isEmpty(amount)) {
            l.onSuccess(WithdrawalFeePreview.failed(null));
            return;
        }
        Long q = channelId > 0 ? channelId : null;
        Observable<WithdrawalFeePreview> obs = createService(WalletService.class)
                .getWithdrawalFeePreview(amount.trim(), q)
                .map(WalletModel::parseWithdrawalFeePreviewFromBody);
        request(obs, l);
    }

    private static WithdrawalFeePreview parseWithdrawalFeePreviewFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return WithdrawalFeePreview.failed(null);
        }
        WithdrawalFeePreview p = WithdrawalFeePreview.parse(body.string());
        return p != null ? p : WithdrawalFeePreview.failed(null);
    }

    /**
     * 兼容：根为 JSON 数组，或对象内 {@code list/data/channels/items/rows/result} 为数组。
     * <p>与 {@code GET /v1/wallet/recharge/channels} 约定一致：{@code list} 与 {@code data} 可并存且内容相同，按顺序取第一个非空数组。</p>
     */
    static List<RechargeChannel> parseRechargeChannelListFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new ArrayList<>();
        }
        String s = body.string();
        return parseRechargeChannelListJson(s);
    }

    static List<RechargeChannel> parseRechargeChannelListJson(String s) {
        // list 与 data 内容相同时可并存；优先 list 与接口文档示例一致
        return parseListFromCommonKeys(s, RechargeChannel.class,
                "list", "data", "channels", "items", "rows", "result");
    }

    public void getCustomerServices(IRequestResultListener<List<CustomerService>> l) {
        Observable<List<CustomerService>> obs = createService(WalletService.class).getCustomerServices()
                .map(WalletModel::parseCustomerServiceListFromBody);
        request(obs, l);
    }

    /**
     * 兼容：根为 JSON 数组，或对象内 {@code data/list/items/rows/result/customer_services/services/records} 为数组。
     */
    static List<CustomerService> parseCustomerServiceListFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new ArrayList<>();
        }
        return parseCustomerServiceListJson(body.string());
    }

    static List<CustomerService> parseCustomerServiceListJson(String s) {
        return parseListFromCommonKeys(s, CustomerService.class,
                "data", "list", "items", "rows", "result", "customer_services", "services", "records");
    }

    public void getWithdrawalDetail(String withdrawalNo, IRequestResultListener<WithdrawalDetail> l) {
        Observable<WithdrawalDetail> obs = createService(WalletService.class)
                .getWithdrawalDetail(withdrawalNo)
                .map(body -> {
                    try {
                        if (body == null) {
                            return null;
                        }
                        return parseWithdrawalDetailJson(body.string());
                    } catch (IOException e) {
                        return null;
                    }
                });
        request(obs, new IRequestResultListener<WithdrawalDetail>() {
            @Override
            public void onSuccess(WithdrawalDetail r) {
                if (r == null || TextUtils.isEmpty(r.withdrawal_no)) {
                    l.onFail(-1, "无法解析提现详情");
                    return;
                }
                l.onSuccess(r);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    /**
     * 用户提现记录列表；响应为 {@code {list,total,page,size}} 或常见 {@code data/list} 包装，见 {@link #parseWithdrawalListJson(String)}。
     */
    public void getWithdrawalList(int page, int size, IRequestResultListener<List<WithdrawalListItem>> l) {
        Observable<List<WithdrawalListItem>> obs = createService(WalletService.class)
                .getWithdrawalList(page, size)
                .map(WalletModel::parseWithdrawalListFromBody);
        request(obs, l);
    }

    static WithdrawalDetail parseWithdrawalDetailJson(String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        JSONObject root = JSON.parseObject(s.trim());
        if (root == null) {
            return null;
        }
        if (root.containsKey("status")) {
            Object stObj = root.get("status");
            if (stObj instanceof Number) {
                int st = ((Number) stObj).intValue();
                if (isWalletBizHttpError(st)) {
                    return null;
                }
            }
        }
        JSONObject payload = root;
        Object data = root.get("data");
        if (data instanceof JSONObject) {
            payload = (JSONObject) data;
        }
        return payload.toJavaObject(WithdrawalDetail.class);
    }

    static List<WithdrawalListItem> parseWithdrawalListFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new ArrayList<>();
        }
        return parseWithdrawalListJson(body.string());
    }

    /**
     * 解析 {@link WalletService#getWithdrawalList(int, int)}：根数组或对象内 {@code data/list/items/rows/withdrawals/records}。
     */
    public static List<WithdrawalListItem> parseWithdrawalListJson(String s) {
        return parseListFromCommonKeys(s, WithdrawalListItem.class,
                "data", "list", "items", "rows", "result", "withdrawals", "records");
    }


    /**
     * 管理端提现列表（分页）。原始 JSON 见 {@link #parseManagerWithdrawalListJson(String)}。
     */
    public void getManagerWithdrawalList(int page, int size, IRequestResultListener<List<ManagerWithdrawalRecord>> l) {
        Observable<List<ManagerWithdrawalRecord>> obs = createService(WalletService.class)
                .getManagerWithdrawalList(page, size)
                .map(WalletModel::parseManagerWithdrawalListFromBody);
        request(obs, l);
    }

    /**
     * 管理端通过提现；{@code remark} 可为 null，将传空字符串。
     */
    public void approveManagerWithdrawal(long id, String remark, IRequestResultListener<CommonResponse> l) {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("remark", remark != null ? remark : "");
        request(createService(WalletService.class).approveManagerWithdrawal(j), l);
    }

    /**
     * 管理端拒绝提现；拒绝原因必填（与接口约定一致，避免无效请求）。
     */
    public void rejectManagerWithdrawal(long id, String remark, IRequestResultListener<CommonResponse> l) {
        if (TextUtils.isEmpty(remark)) {
            l.onFail(-1, "请填写拒绝原因");
            return;
        }
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("remark", remark);
        request(createService(WalletService.class).rejectManagerWithdrawal(j), l);
    }

    static List<ManagerWithdrawalRecord> parseManagerWithdrawalListFromBody(ResponseBody body) throws IOException {
        if (body == null) {
            return new ArrayList<>();
        }
        return parseManagerWithdrawalListJson(body.string());
    }

    /**
     * 解析 {@link WalletService#getManagerWithdrawalList(int, int)} 响应：根数组或常见 {@code data/list/items/rows/withdrawals/records} 包装。
     */
    public static List<ManagerWithdrawalRecord> parseManagerWithdrawalListJson(String s) {
        return parseListFromCommonKeys(s, ManagerWithdrawalRecord.class,
                "data", "list", "items", "rows", "result", "withdrawals", "records");
    }

    private static <T> List<T> parseListFromCommonKeys(String s, Class<T> clazz, String... keys) {
        if (TextUtils.isEmpty(s)) {
            return new ArrayList<>();
        }
        String t = s.trim();
        if (t.startsWith("[")) {
            List<T> list = JSON.parseArray(t, clazz);
            return list != null ? list : new ArrayList<>();
        }
        JSONObject obj = JSON.parseObject(t);
        if (obj == null) {
            return new ArrayList<>();
        }
        return extractListFromCommonKeys(obj, clazz, keys);
    }

    private static <T> List<T> extractListFromCommonKeys(JSONObject obj, Class<T> clazz, String... keys) {
        if (obj == null || keys == null || keys.length == 0) {
            return new ArrayList<>();
        }
        for (String k : keys) {
            if (!obj.containsKey(k)) {
                continue;
            }
            Object v = obj.get(k);
            List<T> list = parseListValue(v, clazz, keys);
            if (!list.isEmpty()) {
                return list;
            }
        }
        return new ArrayList<>();
    }

    private static <T> List<T> parseListValue(Object value, Class<T> clazz, String... keys) {
        if (value instanceof JSONArray) {
            List<T> list = ((JSONArray) value).toJavaList(clazz);
            return list != null ? list : new ArrayList<>();
        }
        if (value instanceof JSONObject) {
            return extractListFromCommonKeys((JSONObject) value, clazz, keys);
        }
        if (value instanceof String) {
            String inner = ((String) value).trim();
            if (inner.startsWith("[")) {
                List<T> list = JSON.parseArray(inner, clazz);
                return list != null ? list : new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public void sendRedPacket(int type, String channelId, int channelType, double totalAmount, int totalCount, String toUid, String remark, String password, IRequestResultListener<RedPacketSendResp> l) {
        JSONObject j = new JSONObject();
        j.put("type", type); j.put("channel_id", channelId); j.put("channel_type", channelType);
        j.put("total_amount", totalAmount); j.put("total_count", totalCount);
        j.put("remark", remark); j.put("password", password);
        if (toUid != null) j.put("to_uid", toUid);
        request(createService(WalletService.class).sendRedPacket(j), new IRequestResultListener<RedPacketSendResp>() {
            @Override
            public void onSuccess(RedPacketSendResp r) {
                dispatchRedPacketSend(r, l);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }
    public void openRedPacket(String packetNo, IRequestResultListener<RedPacketOpenResp> l) {
        JSONObject j = new JSONObject();
        j.put("packet_no", packetNo);
        request(createService(WalletService.class).openRedPacket(j), new IRequestResultListener<RedPacketOpenResp>() {
            @Override
            public void onSuccess(RedPacketOpenResp r) {
                dispatchOpenRedPacket(r, l);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    private static void dispatchOpenRedPacket(RedPacketOpenResp r, IRequestResultListener<RedPacketOpenResp> l) {
        if (r == null) {
            l.onFail(-1, "empty response");
            return;
        }
        if (isWalletBizHttpError(r.status)) {
            l.onFail(r.status, TextUtils.isEmpty(r.msg) ? "操作失败" : r.msg);
            return;
        }
        if (r.code != null && isWalletBizHttpError(r.code)) {
            l.onFail(r.code, TextUtils.isEmpty(r.msg) ? "操作失败" : r.msg);
            return;
        }
        l.onSuccess(r);
    }

    public void getRedPacketDetail(String packetNo, IRequestResultListener<RedPacketDetailResp> l) {
        request(createService(WalletService.class).getRedPacketDetail(packetNo), l);
    }

    /**
     * {@code POST /v1/transfer/send}，请求体与后台 tfSend 一致：
     * {@code to_uid}、{@code amount}、{@code remark}、{@code password}、{@code channel_id}、{@code channel_type}；
     * {@code pay_scene} 仅在对应业务场景传入（例如收款码场景为 {@code receive_qr}）。
     */
    public void sendTransfer(String toUid, double amount, String remark, String password,
                             String channelId, int channelType,
                             @Nullable String payScene, IRequestResultListener<TransferSendResp> l) {
        if (TextUtils.isEmpty(channelId)) {
            l.onFail(-1, "channel_id 不能为空");
            return;
        }
        JSONObject j = new JSONObject();
        j.put("to_uid", toUid);
        j.put("amount", amount);
        j.put("remark", remark);
        j.put("password", password);
        j.put("channel_id", channelId);
        j.put("channel_type", channelType);
        if (!TextUtils.isEmpty(payScene)) {
            j.put("pay_scene", payScene);
        }
        request(createService(WalletService.class).sendTransfer(j), new IRequestResultListener<TransferSendResp>() {
            @Override
            public void onSuccess(TransferSendResp r) {
                dispatchTransferSend(r, l, null);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }

    public void acceptTransfer(String transferNo, IRequestResultListener<TransferSendResp> l) {
        request(createService(WalletService.class).acceptTransfer(transferNo), new IRequestResultListener<TransferSendResp>() {
            @Override
            public void onSuccess(TransferSendResp r) {
                dispatchTransferSend(r, l, transferNo);
            }

            @Override
            public void onFail(int c, String m) {
                l.onFail(c, m);
            }
        });
    }
    public void getTransferDetail(String transferNo, IRequestResultListener<TransferDetailResp> l) {
        request(createService(WalletService.class).getTransferDetail(transferNo), l);
    }

    private static String redPacketNoFromResp(RedPacketSendResp r) {
        if (r == null) {
            return null;
        }
        if (!TextUtils.isEmpty(r.packet_no)) {
            return r.packet_no;
        }
        if (r.data != null) {
            String p = r.data.getString("packet_no");
            if (!TextUtils.isEmpty(p)) {
                return p;
            }
        }
        return null;
    }

    private static String transferNoFromResp(TransferSendResp r) {
        if (r == null) {
            return null;
        }
        if (!TextUtils.isEmpty(r.transfer_no)) {
            return r.transfer_no;
        }
        if (r.data != null) {
            String t = r.data.getString("transfer_no");
            if (!TextUtils.isEmpty(t)) {
                return t;
            }
        }
        return null;
    }

    /** 业务层错误：HTTP 已是 200，但 JSON 里 status 表示失败 */
    private static boolean isWalletBizHttpError(int status) {
        return status >= 400 && status < 600;
    }

    private static void dispatchRedPacketSend(RedPacketSendResp r, IRequestResultListener<RedPacketSendResp> l) {
        if (r == null) {
            l.onFail(-1, "empty response");
            return;
        }
        String no = redPacketNoFromResp(r);
        if (!TextUtils.isEmpty(no)) {
            r.packet_no = no;
        }
        if (isWalletBizHttpError(r.status)) {
            l.onFail(r.status, !TextUtils.isEmpty(r.msg) ? r.msg : "操作失败");
            return;
        }
        if (TextUtils.isEmpty(r.packet_no)) {
            l.onFail(r.status != 0 ? r.status : -1,
                    !TextUtils.isEmpty(r.msg) ? r.msg : "未返回红包单号，请检查接口或余额是否扣除");
            return;
        }
        l.onSuccess(r);
    }

    private static void dispatchTransferSend(
            TransferSendResp r,
            IRequestResultListener<TransferSendResp> l,
            @Nullable String transferNoIfAbsent
    ) {
        if (r == null) {
            l.onFail(-1, "empty response");
            return;
        }
        String no = transferNoFromResp(r);
        if (TextUtils.isEmpty(no) && !TextUtils.isEmpty(transferNoIfAbsent)) {
            no = transferNoIfAbsent;
        }
        if (!TextUtils.isEmpty(no)) {
            r.transfer_no = no;
        }
        if (isWalletBizHttpError(r.status)) {
            l.onFail(r.status, !TextUtils.isEmpty(r.msg) ? r.msg : "操作失败");
            return;
        }
        if (TextUtils.isEmpty(r.transfer_no)) {
            l.onFail(r.status != 0 ? r.status : -1,
                    !TextUtils.isEmpty(r.msg) ? r.msg : "未返回转账单号，请检查接口或余额是否扣除");
            return;
        }
        l.onSuccess(r);
    }
}
