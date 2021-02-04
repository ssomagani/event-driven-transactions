package metro;

public class RiderEvent {

    public enum ACTIVITY {
        ENTER(1), EXIT(-1);

        public int value;
        private ACTIVITY(int value) {
            this.value = value;
        }
    }

    public final int cardId;
    public final int stationId;
    public final ACTIVITY activity;

    public RiderEvent(int cardId, int stationId, ACTIVITY activity) {
        this.cardId = cardId;
        this.stationId = stationId;
        this.activity = activity;
    }
}
