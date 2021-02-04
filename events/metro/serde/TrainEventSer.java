package metro.serde;

import java.time.format.DateTimeFormatter;

import metro.TrainEvent;

public class TrainEventSer implements org.apache.kafka.common.serialization.Serializer<TrainEvent> {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSSSSS");

    @Override
    public byte[] serialize(String arg0, TrainEvent trainEvent) {
        byte[] retVal = null;
        try {
            retVal = (
                    trainEvent.trainId + ","
                            + trainEvent.location.station.stationId + ","
                            + trainEvent.location.state.value + ","
                            + trainEvent.location.timestamp.format(formatter)).getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
}