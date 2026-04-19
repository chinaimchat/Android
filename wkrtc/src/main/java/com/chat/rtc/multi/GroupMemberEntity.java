package com.chat.rtc.multi;

import android.os.Parcel;
import android.os.Parcelable;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.xinbida.wukongim.entity.WKChannelMember;

/**
 * 2020-05-31 16:55
 * 群成员
 */
public class GroupMemberEntity implements Parcelable, MultiItemEntity {
    public int checked;
    public int isCanCheck;
    public String pying;
    public WKChannelMember member;
    public int itemType;
    public boolean isSetDelete;
    protected GroupMemberEntity(Parcel in) {
        checked = in.readInt();
        isCanCheck = in.readInt();
        pying = in.readString();
        member = in.readParcelable(WKChannelMember.class.getClassLoader());
    }

    public GroupMemberEntity() {
        isCanCheck = 1;
    }

    public static final Creator<GroupMemberEntity> CREATOR = new Creator<>() {
        @Override
        public GroupMemberEntity createFromParcel(Parcel in) {
            return new GroupMemberEntity(in);
        }

        @Override
        public GroupMemberEntity[] newArray(int size) {
            return new GroupMemberEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(checked);
        parcel.writeInt(isCanCheck);
        parcel.writeString(pying);
        parcel.writeParcelable(member, i);
    }


    @Override
    public int getItemType() {
        return itemType;
    }
}
