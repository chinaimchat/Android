package rtc.inters;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public interface IAvatarLoader {
    public void onAvatarLoader(@Nullable Context context, String uid, ImageView imageView, int width, boolean isP2PCall);
}
