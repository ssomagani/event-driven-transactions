package metro.serde;

import metro.CardEvent;

public class CardEventSer implements org.apache.kafka.common.serialization.Serializer<CardEvent> {

    @Override
    public byte[] serialize(String arg0, CardEvent rechargeEvent) {
        byte[] retVal = null;
        try {
            retVal = (rechargeEvent.cardId + "," + rechargeEvent.amount).getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
