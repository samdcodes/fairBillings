package fairBilling;

public class Bill {
    private int sessions;
    private long totalTimeSeconds;

    public Bill(int sessions, long totalTimeSeconds) {
        this.sessions = sessions;
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public int getSessions() {
        return sessions;
    }

    public long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }


    public void addSession() {
        this.sessions += 1;
    }

    public void addActiveSessionTime(long sessionTime) {
        this.totalTimeSeconds += sessionTime;
    }
}
