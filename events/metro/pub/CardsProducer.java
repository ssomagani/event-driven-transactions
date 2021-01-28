package metro.pub;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.voltdb.CLIConfig;

import metro.CardEvent;

public class CardsProducer {

    private final String brokers;
    private final String topicName;
    private Producer<Integer, CardEvent> producer;

    public CardsProducer(String brokers, String topicName) {
        this.brokers = brokers;
        this.topicName = topicName;
        this.producer = createProducer();
    }

    private Producer<Integer, CardEvent> createProducer() {

        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);

        props.put("key.serializer",
                "org.apache.kafka.common.serialization.IntegerSerializer");

        props.put("value.serializer",
                "metro.serde.CardEventSer");

        Producer<Integer, CardEvent> producer = new KafkaProducer
                <Integer, CardEvent>(props);
        return producer;
    }

    public void publish(List<CardEvent> rechargeEvents) {
        if (rechargeEvents != null) {
            for (CardEvent rechargeEvent : rechargeEvents) {
                producer.send(new ProducerRecord<Integer, CardEvent>(topicName, rechargeEvent));
            }
        }
    }

    public static Config CONFIG = new Config();

    public static class Config extends CLIConfig {

        @Option
        private String mode = "new";

        @Option(desc = "Number of Cards.")
        public int cardcount = 500000;

        @Option(desc = "File output location.")
        String output = "data/cards.csv";

        @Option
        String servers = "localhost:9999";

        @Option
        String topic = "";

        @Override
        public void validate() {
            if (cardcount < 0) exitWithMessageAndUsage("card count must be > 0");
        }
    }

    private static void genCards(Config config) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(config.output));
        for (int i = 0; i < config.cardcount; i++) {
            final StringBuilder sb = new StringBuilder();
            sb.append(i).append(",1,0,100000,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            i++;
            sb.append(i).append(",1,0,50000,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            i++;
            sb.append(i).append(",1,0,5000,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            i++;
            sb.append(i).append(",1,0,2000,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            i++;
            sb.append(i).append(",1,0,1000,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            i++;
            sb.append(i).append(",1,0,500,2030-01-01 12:00:00,Rider").append(i).append(",7815551212,Rider").append(i).append("@test.com,0\n");
            writer.write(sb.toString());
        }
        writer.close();
    }

    public List<CardEvent> getRechargeActivityRecords(int count) {
        final ArrayList<CardEvent> records = new ArrayList<>();
        int amt = (ThreadLocalRandom.current().nextInt(18)+2)*1000;
        ThreadLocalRandom.current().ints(count, 0, count).forEach((cardId)
                -> {
                    records.add(new CardEvent(amt, cardId));
                    }
        );
        return records;
    }

    public static void main(String[] args) throws IOException {
        CONFIG.parse("CardsProducer", args);
        if(CONFIG.mode.equals("new")) {
            genCards(CONFIG);
            return;
        }

        ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(10);
        CardsProducer producer = new CardsProducer(CONFIG.servers, CONFIG.topic);

        System.out.println("Recharging Cards");

        EXECUTOR.scheduleAtFixedRate (
                () -> {
                    producer.publish(producer.getRechargeActivityRecords(1));
                }, 1, 5, MILLISECONDS);
    }
}
