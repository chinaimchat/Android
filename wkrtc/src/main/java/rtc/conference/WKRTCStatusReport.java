package rtc.conference;

import org.webrtc.RTCStatsReport;

/**
 * 5/10/21 10:13 AM
 */
public class WKRTCStatusReport {
    public double lastTotalAudioEnergy;
    public RTCStatsReport rtcStatsReport;

    public WKRTCStatusReport(RTCStatsReport rtcStatsReport, double lastTotalAudioEnergy) {
        this.rtcStatsReport = rtcStatsReport;
        this.lastTotalAudioEnergy = lastTotalAudioEnergy;
    }
}
