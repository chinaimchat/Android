package com.chat.base.entity;

public class WKAPPConfig {
    public int version;
    public String web_url;
    public int phone_search_off;
    public int shortno_edit_off;
    public int revoke_second;
    public int register_invite_on;
    public int send_welcome_message_on;
    public int invite_system_account_join_group_on;
    public int register_user_must_complete_info_on;
    public int can_modify_api_url;
    /** common/appconfig：null 或非 0 展示对方分端在线文案；0 仅展示是否在线 */
    public Integer show_device_online_on;
}
