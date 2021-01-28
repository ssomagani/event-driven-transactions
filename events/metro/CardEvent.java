package metro;

public class CardEvent {

    public final int cardId;
    public final int amount;

    public CardEvent(int cardId, int amount) {
        this.cardId = cardId;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return cardId + " : " + amount;
    }
}

