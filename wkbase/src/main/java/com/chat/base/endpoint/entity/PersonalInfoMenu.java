package com.chat.base.endpoint.entity;

/**
 * 2020-09-02 10:59
 * 个人中心菜单配置
 */
public class PersonalInfoMenu extends BaseEndpoint {
    public IPersonalInfoMenuClick iPersonalInfoMenuClick;
    public boolean isNewVersionIv = false;
    /**
     * 底部分割线类型：0=无，1=细线(1px，组内分隔)，2=大间距(15dp，区分区块)
     * 我的钱包用2，电脑端登录~通用用1，通用最后一项用0
     */
    public int bottomDividerType = 0;

    public PersonalInfoMenu(String sid, int imgResourceID, String text, IPersonalInfoMenuClick iPersonalInfoMenuClick) {
        this.imgResourceID = imgResourceID;
        this.text = text;
        this.sid = sid;
        this.iPersonalInfoMenuClick = iPersonalInfoMenuClick;
    }

    public PersonalInfoMenu(int imgResourceID, String text, IPersonalInfoMenuClick iPersonalInfoMenuClick) {
        this.imgResourceID = imgResourceID;
        this.text = text;
        this.iPersonalInfoMenuClick = iPersonalInfoMenuClick;
    }

    public PersonalInfoMenu(int imgResourceID, String text, int bottomDividerType, IPersonalInfoMenuClick iPersonalInfoMenuClick) {
        this.imgResourceID = imgResourceID;
        this.text = text;
        this.bottomDividerType = bottomDividerType;
        this.iPersonalInfoMenuClick = iPersonalInfoMenuClick;
    }

    public void setIsNewVersionIv(boolean is) {
        isNewVersionIv = is;
    }

    public interface IPersonalInfoMenuClick {
        void onClick();
    }
}
