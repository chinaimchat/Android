package rtc.inters;

/**
 * 5/18/21 12:11 PM
 * 计时器事件
 */
public interface ITimerListener {
    public void onTimeChanged(long time, String timeText);
}
