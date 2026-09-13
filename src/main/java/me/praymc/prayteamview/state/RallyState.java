package me.praymc.prayteamview.state;

public final class RallyState {

    private long epoch;
    private long lastSequence = -1;
    private RallyPoint point;

    public long beginWorld() {
        epoch++;
        lastSequence = -1;
        point = null;
        return epoch;
    }

    public long epoch() {
        return epoch;
    }

    public void apply(long incomingEpoch, long sequence, RallyPoint point) {
        if (incomingEpoch != epoch || sequence <= lastSequence) return;
        lastSequence = sequence;
        this.point = point;
    }

    public RallyPoint current() {
        // 고정 지점은 삭제나 월드 전환까지 유지하므로 주기적인 재전송이 필요 없다.
        return point;
    }
}
