/* This file is part of VoltDB.
 * Copyright (C) 2008-2020 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package metro.pub;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static metro.RiderEvent.ACTIVITY.ENTER;
import static metro.RiderEvent.ACTIVITY.EXIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.voltdb.CLIConfig;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.NullCallback;
import org.voltdb.client.ProcedureCallback;

import com.google_voltpatches.common.collect.ConcurrentHashMultiset;
import com.google_voltpatches.common.collect.Multiset;

public class RidersProducer {

    public static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
                    "----------" + "----------" + "----------" + "----------" + "\n";

    private static MetroCardConfig config;
    private long benchmarkStartTS;

    private static Client client;
    private ClientStatsContext periodicStatsContext;
    private ClientStatsContext fullStatsContext;

    private Timer periodicStatsTimer;
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(100);
    public static void printHeading(String heading) {
        System.out.print("\n"+HORIZONTAL_RULE);
        System.out.println(" " + heading);
        System.out.println(HORIZONTAL_RULE);
    }

    /**
     * Declaratively state command line options with defaults and validation
     */
    public static class MetroCardConfig extends CLIConfig {

        @Option(desc = "Comma separated list of the form server[:port] to connect to.")
        String servers = "localhost";

        @Option(desc = "User name")
        public String user = "";

        @Option(desc = "Password")
        public String password = "";

        @Option(desc = "TPS rate for card entry and exit swipes")
        int rate = 5000;

        @Option(desc = "Interval for performance feedback, in seconds.")
        long displayinterval = 5;

        @Option(desc = "Filename to write raw summary statistics to.")
        String statsfile = "";

        @Option(desc = "Proc for card entry swipes")
        String cardEntry = "ValidateEntry";

        @Option(desc = "Proc for card entry swipes")
        String cardExit = "ProcessExit";

        @Option(desc = "Number of Cards. If you loaded cards via csv make sure you use that number.")
        int cardcount = 500000;

        @Option(desc = "Benchmark duration, in seconds.")
        int duration = 2000;

        @Option(desc = "Print out latency report result")
        public boolean latencyreport = true;

    }

    public RidersProducer(MetroCardConfig config) {
        RidersProducer.config = config;
        printHeading("Command Line Configuration");
        System.out.println(config.getConfigDumpString());
    }

    /**
     * Provides a callback to be notified on node failure.
     * This example only logs the event.
     */
    class StatusListener extends ClientStatusListenerExt {
        @Override
        public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
            // if the benchmark is still active
            if ((System.currentTimeMillis() - benchmarkStartTS) < (config.duration * 1000)) {
                System.err.printf("Connection to %s:%d was lost.\n", hostname, port);
            }
        }
    }

    /**
     * Connect to a single server with retry. Limited exponential backoff.
     * No timeout. This will run until the process is killed if it's not
     * able to connect.
     *
     * @param server hostname:port or just hostname (hostname can be ip).
     */
    void connectToOneServerWithRetry(String server, Client client) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            }
            catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try { Thread.sleep(sleep); } catch (Exception interruted) {}
                if (sleep < 8000) sleep += sleep;
            }
        }
        System.out.printf("Connected to VoltDB node at: %s.\n", server);
    }

    /**
     * Connect to a set of servers in parallel. Each will retry until
     * connection. This call will block until all have connected.
     *
     * @param servers A comma separated list of servers using the hostname:port
     * syntax (where :port is optional).
     * @throws InterruptedException if anything bad happens with the threads.
     */
    void connect(String servers, Client client) throws InterruptedException {
        System.out.println("Connecting to VoltDB...");

        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(server, client);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }

    /**
     * Create a Timer task to display performance data on the Vote procedure
     * It calls printStatistics() every displayInterval seconds
     */
    public void schedulePeriodicStats(ClientStatsContext periodicStatsContext) {
        periodicStatsTimer = new Timer();
        TimerTask statsPrinting = new TimerTask() {
            @Override
            public void run() { printStatistics(periodicStatsContext); }
        };
        periodicStatsTimer.scheduleAtFixedRate(statsPrinting,
                config.displayinterval * 1000,
                config.displayinterval * 1000);
    }

    /**
     * Prints a one line update on performance that can be printed
     * periodically during a benchmark.
     */
    public synchronized void printStatistics(ClientStatsContext periodicStatsContext) {
        ClientStats stats = periodicStatsContext.fetchAndResetBaseline().getStats();
        long time = Math.round((stats.getEndTimestamp() - benchmarkStartTS) / 1000.0);

        System.out.printf("%02d:%02d:%02d ", time / 3600, (time / 60) % 60, time % 60);
        System.out.printf("Throughput %d/s, ", stats.getTxnThroughput());
        System.out.printf("Aborts/Failures %d/%d, ",
                stats.getInvocationAborts(), stats.getInvocationErrors());

        // cast to stats.getAverageLatency from long to double
        System.out.printf("Avg/95%% Latency %.2f/%dms\n",
                stats.getAverageLatency(),
                stats.kPercentileLatency(0.95));
    }

    /**
     * Prints the results of the voting simulation and statistics
     * about performance.
     *
     * @throws Exception if anything unexpected happens.
     */
    public synchronized void printResults(Client client, ClientStatsContext fullStatsContext) throws Exception {
        printHeading("Transaction Results");
        BenchmarkCallback.printAllResults();

        ClientStats stats = fullStatsContext.fetch().getStats();

        // 3. Performance statistics
        printHeading("Client Workload Statistics");

        System.out.printf("Average throughput:            %,9d txns/sec\n", stats.getTxnThroughput());
        if(RidersProducer.config.latencyreport) {
            System.out.printf("Average latency:               %,9.2f ms\n", stats.getAverageLatency());
            System.out.printf("10th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.1));
            System.out.printf("25th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.25));
            System.out.printf("50th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.5));
            System.out.printf("75th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.75));
            System.out.printf("90th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.9));
            System.out.printf("95th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.95));
            System.out.printf("99th percentile latency:       %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.99));
            System.out.printf("99.5th percentile latency:     %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.995));
            System.out.printf("99.9th percentile latency:     %,9.2f ms\n", stats.kPercentileLatencyAsDouble(.999));

            System.out.print("\n" + HORIZONTAL_RULE);
            System.out.println(" System Server Statistics");
            System.out.println(HORIZONTAL_RULE);
            System.out.printf("Reported Internal Avg Latency: %,9.2f ms\n", stats.getAverageInternalLatency());

            System.out.print("\n" + HORIZONTAL_RULE);
            System.out.println(" Latency Histogram");
            System.out.println(HORIZONTAL_RULE);
            System.out.println(stats.latencyHistoReport());
        }
        printHeading("System Server Statistics");

        System.out.printf("Reported Internal Avg Latency: %,9.2f ms\n", stats.getAverageInternalLatency());

        // 4. Write stats to file if requested
        client.writeSummaryCSV(stats, config.statsfile);
    }

    public static class BenchmarkCallback implements ProcedureCallback {

        private static Multiset<String> stats = ConcurrentHashMultiset.create();
        private static ConcurrentHashMap<String,Integer> procedures = new ConcurrentHashMap<String,Integer>();
        String procedureName;
        long maxErrors;
        Object[] args;

        public static int count( String procedureName, String event ){
            return stats.add(procedureName + event, 1);
        }

        public static int getCount( String procedureName, String event ){
            return stats.count(procedureName + event);
        }

        public static void printProcedureResults(String procedureName) {
            System.out.println("  " + procedureName);
            System.out.println("        calls: " + getCount(procedureName,"call"));
            System.out.println("      commits: " + getCount(procedureName,"commit"));
            System.out.println("    rollbacks: " + getCount(procedureName,"rollback"));
        }

        public static void printAllResults() {
            List<String> l = new ArrayList<String>(procedures.keySet());
            Collections.sort(l);
            for (String e : l) {
                printProcedureResults(e);
            }
        }

        public BenchmarkCallback(String procedure, Object[] args, long maxErrors) {
            super();
            this.procedureName = procedure;
            this.args = args;
            this.maxErrors = maxErrors;
            procedures.putIfAbsent(procedure,1);
        }

        public BenchmarkCallback(String procedure, Object[] args) {
            this(procedure, args, 25l);
        }

        @Override
        public void clientCallback(ClientResponse cr) {

            count(procedureName,"call");
            if (cr.getStatus() == ClientResponse.SUCCESS) {
                count(procedureName,"commit");
                if(cr.getResults()[0].advanceRow()) {
                    if (cr.getResults()[0].getLong(0) == 1) {
                        final int delayMS = 1000+ThreadLocalRandom.current().nextInt(100);
                        EXECUTOR.schedule(() -> {
                            callNoCallback(config.cardExit, getExitFromEntryRecords(args));
                        }, delayMS, MILLISECONDS);
                    }
                }
            } else {
                long totalErrors = count(procedureName,"rollback");

                if (totalErrors > maxErrors) {
                    System.err.println("exceeded " + maxErrors + " maximum database errors - exiting client");
                    System.exit(-1);
                }

                System.err.println("DATABASE ERROR: " + cr.getStatusString());
            }
        }
    }

    protected void call(String proc, List<Object[]> args) {
        if (args != null) {
            for (Object[] arg : args) {
                call(proc, arg);
            }
        }
    }

    protected static void call(String proc, Object[] args) {
        try {
            client.callProcedure(new BenchmarkCallback(proc, args), proc, args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void callNoCallback(String proc, Object[] args) {
        try {
            client.callProcedure(new NullCallback(), proc, args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Object[]> getEntryActivityRecords(int count) {
        final ArrayList<Object[]> records = new ArrayList<>();
        long curTime = System.currentTimeMillis();
        ThreadLocalRandom.current().ints(1, 0, count).forEach((cardId)
                -> {
                    records.add(new Object[] {cardId, curTime, Stations.getRandomStation().stationId, ENTER.value, 0});
                    }
        );
        return records;
    }

    public static Object[] getExitFromEntryRecords(Object[] record) {
        int nextStationId = Stations.getNextRandomStationId((Integer) record[2]);
        long delay = Stations.getTimeToStation((Integer)record[2], nextStationId);
            record[1] = (long) record[1] + delay;
            record[2] = nextStationId;
            record[3] = EXIT.value;
        return record;
    }

    /**
     * Core benchmark code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void runBenchmark() throws Exception {

        ClientConfig clientConfig = new ClientConfig(config.user, config.password, new StatusListener());
        clientConfig.setMaxTransactionsPerSecond(config.rate);
        client = ClientFactory.createClient(clientConfig);

        connect(config.servers, client);

        periodicStatsContext = client.createStatsContext();
        fullStatsContext = client.createStatsContext();

        benchmarkStartTS = System.currentTimeMillis();
        schedulePeriodicStats(periodicStatsContext);

        int microsPerTrans = 1000000/RidersProducer.config.rate;

        EXECUTOR.scheduleAtFixedRate (
                () -> {
                    List<Object[]> entryRecords = getEntryActivityRecords(config.cardcount);
                    call(config.cardEntry, entryRecords);
                }, 10000, microsPerTrans, MICROSECONDS);

        EXECUTOR.schedule(() -> {shutdown();}, RidersProducer.config.duration, SECONDS);
    }

    private void shutdown() {
        EXECUTOR.shutdownNow();
        periodicStatsTimer.cancel();
        try {
            // print the summary results
            printResults(client, fullStatsContext);
            // close down the client connections
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        config = new MetroCardConfig();
        config.parse(RidersProducer.class.getName(), args);

        RidersProducer producer = new RidersProducer(config);
        producer.runBenchmark();
    }
}
