package metro.pub;

import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static metro.TrainEvent.LastKnownLocation.DIRECTION.BACKWARD;
import static metro.TrainEvent.LastKnownLocation.DIRECTION.FORWARD;
import static metro.TrainEvent.LastKnownLocation.STATE.DEPARTED;
import static metro.TrainEvent.LastKnownLocation.STATE.STOPPED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import metro.TrainEvent;
import metro.TrainEvent.LastKnownLocation;

public class TrainProducer {

    private HashMap<Integer, TrainEvent> idToTrainMap = new HashMap<>();

    public void initialize(int count) {
        IntStream.range(0, count).forEach((trainId) -> {
            idToTrainMap.put(trainId, new TrainEvent(trainId, Stations.getRandomStation(), FORWARD, LocalDateTime.now()));
        });
    }

    private String brokers;
    private String topicName;
    private Producer<String, TrainEvent> producer;
    public final int count;

    public TrainProducer(String brokers, String topicName, int count) {
        this.brokers = brokers;
        this.topicName = topicName;
        this.count = count;
        this.producer = createProducer();
        initialize(count);
    }

    private Producer<String, TrainEvent> createProducer() {

        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);

        props.put("key.serializer",
           "org.apache.kafka.common.serialization.StringSerializer");

        props.put("value.serializer",
           "metro.serde.TrainEventSer");

        Producer<String, TrainEvent> producer = new KafkaProducer
           <String, TrainEvent>(props);

        return producer;
    }

    public void publish(List<TrainEvent> trainEvents) {
        if (trainEvents != null) {
            for (TrainEvent trainEvent : trainEvents) {
                producer.send(new ProducerRecord<String, TrainEvent>(topicName, trainEvent));
            }
        }
    }

    public List<TrainEvent> getNewEvents() {
        ArrayList<TrainEvent> records = new ArrayList<>();
        for(TrainEvent trainEvent : idToTrainMap.values()) {
            LastKnownLocation prevLoc = trainEvent.location;
            LastKnownLocation curLoc = next(prevLoc, LocalDateTime.now());
            if(!prevLoc.equals(curLoc)) {
                trainEvent = new TrainEvent(trainEvent.trainId, curLoc);
                idToTrainMap.put(trainEvent.trainId, trainEvent);
                records.add(trainEvent);
            }
        }
        return records;
    }

    public static LastKnownLocation next(LastKnownLocation location, LocalDateTime now) {
        long timeElapsed = location.timestamp.until(now, MICROS);
        if(location.state.equals(STOPPED) && location.station.stnWaitDuration < timeElapsed) {
            if(location.station.stationId == Stations.size()) {
                if(location.direction == FORWARD) {
                    return new LastKnownLocation(location.station, BACKWARD, DEPARTED, now);
                }
            }
            if(location.station.stationId == 1) {
                if(location.direction == BACKWARD) {
                    return new LastKnownLocation(location.station, FORWARD, DEPARTED, now);
                }
            }
            return new LastKnownLocation(location.station, location.direction, DEPARTED, now);
        } else if(location.state.equals(DEPARTED) && location.station.nextStnDuration < timeElapsed) {
            if(location.direction == FORWARD) {
                if(location.station.stationId == Stations.size()) {
                    return new LastKnownLocation(Stations.getStation(location.station.stationId), BACKWARD, STOPPED, now);
                }
                return new LastKnownLocation(Stations.getStation(location.station.stationId+1), location.direction, STOPPED, now);
            } else {
                if(location.station.stationId == 1) {
                    return new LastKnownLocation(Stations.getStation(0), FORWARD, STOPPED, now);
                }
                return new LastKnownLocation(Stations.getStation(location.station.stationId-1), location.direction, STOPPED, now);
            }
        }
        return location;
    }


    public static void main(String[] args) {
        ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(10);
        TrainProducer producer = new TrainProducer(args[0], args[1], Integer.parseInt(args[2]));

        System.out.println("Scheduling trains");

        EXECUTOR.scheduleAtFixedRate (
                () -> {
                    producer.publish(producer.getNewEvents());
                }, 1, 50, MILLISECONDS);
    }
}
