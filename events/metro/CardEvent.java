package metro;

public class CardEvent {

    public final int cardId;
    public final int amount;
    public final int stationId;

    public CardEvent(int cardId, int amount, int stationId) {
        this.cardId = cardId;
        this.amount = amount;
        this.stationId = stationId;
    }

    @Override
    public String toString() {
        return cardId + " : " + amount + " : " + stationId;
    }
}

