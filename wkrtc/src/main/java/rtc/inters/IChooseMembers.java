package rtc.inters;

import android.content.Context;


import java.util.List;

/**
 * 5/7/21 3:58 PM
 * 选择联系人
 */
public interface IChooseMembers {
    void chooseMembers(Context context, String roomID, String channelID, byte channelType, List<String> selectedUIDs, IChooseMembersBack iChooseMembersBack);
}
