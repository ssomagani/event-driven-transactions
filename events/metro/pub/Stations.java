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

import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class Stations {

    static HashMap<Integer, Station> idToStationMap = new HashMap<>();

    static {
        idToStationMap.put(1, new Station(1, 1200000, 450000));
        idToStationMap.put(2, new Station(2, 1050000, 250000));
        idToStationMap.put(3, new Station(3, 850000, 300000));
        idToStationMap.put(4, new Station(4, 900000, 350000));
        idToStationMap.put(5, new Station(5, 500000, 260000));
        idToStationMap.put(6, new Station(6, 950000, 190000));
        idToStationMap.put(7, new Station(7, 450000, 130000));
        idToStationMap.put(8, new Station(8, 200000, 280000));
        idToStationMap.put(9, new Station(9, 200000, 110000));
        idToStationMap.put(10, new Station(10, 450000, 300000));
        idToStationMap.put(11, new Station(11, 550000, 200000));
        idToStationMap.put(12, new Station(12, 550000, 200000));
        idToStationMap.put(13, new Station(13, 800000, 150000));
        idToStationMap.put(14, new Station(14, 950000, 100000));
        idToStationMap.put(15, new Station(15, 1000000, 130000));
        idToStationMap.put(16, new Station(16, 1200000, 220000));
        idToStationMap.put(17, new Station(17, 1500000, 500000));
    }

    private static final RandomCollection<Integer> WEIGHTED_STATIONS_COLLECTION = new RandomCollection<>();
    static {
        WEIGHTED_STATIONS_COLLECTION.add(242200,1);
        WEIGHTED_STATIONS_COLLECTION.add(325479,2);
        WEIGHTED_STATIONS_COLLECTION.add(221055,3);
        WEIGHTED_STATIONS_COLLECTION.add(581530,4);
        WEIGHTED_STATIONS_COLLECTION.add(406389,5);
        WEIGHTED_STATIONS_COLLECTION.add(375640,6);
        WEIGHTED_STATIONS_COLLECTION.add(259210,7);
        WEIGHTED_STATIONS_COLLECTION.add(412809,8);
        WEIGHTED_STATIONS_COLLECTION.add(496942,9);
        WEIGHTED_STATIONS_COLLECTION.add(559110,10);
        WEIGHTED_STATIONS_COLLECTION.add(131022,11);
        WEIGHTED_STATIONS_COLLECTION.add(145955,12);
        WEIGHTED_STATIONS_COLLECTION.add(207333,13);
        WEIGHTED_STATIONS_COLLECTION.add(56457,14);
        WEIGHTED_STATIONS_COLLECTION.add(122236,15);
        WEIGHTED_STATIONS_COLLECTION.add(51981,16);
        WEIGHTED_STATIONS_COLLECTION.add(203866,17);
    }

    public static long getTimeToStation(int origin, int dest) {
        long time = 0;
        int start = origin;
        int end = dest;
        if(origin > dest) {
            start = dest;
            end = origin;
        }

        for(int i=start; i<end; i++) {
            Station station = idToStationMap.get(i);
            time += station.stnWaitDuration;
            time += station.nextStnDuration;
        }
        return time;
    }

    public static Station getStation(int stationId) {
        return idToStationMap.get(stationId);
    }

    public static int size() {
        return idToStationMap.size();
    }

    public static Station getRandomStation() {
        return getStation(WEIGHTED_STATIONS_COLLECTION.next());
    }

    public static int getNextRandomStationId(int stationId) {
        int stationCount = idToStationMap.size();

        if(stationId == 1) {
            return ThreadLocalRandom.current().nextInt(2, stationCount);
        } else if(stationId == stationCount) {
            return ThreadLocalRandom.current().nextInt(1, stationCount - 1);
        } else {
            if(ThreadLocalRandom.current().nextBoolean())
                return ThreadLocalRandom.current().nextInt(stationId+1, stationCount+1);
            return ThreadLocalRandom.current().nextInt(1, stationId);
        }
    }

    public static class Station {
        public final int stationId;
        public final int nextStnDuration;
        public final int stnWaitDuration;
        public Station(int stationId, int nextStnDuration, int stnWaitDuration) {
            this.stationId = stationId;
            this.nextStnDuration = nextStnDuration;
            this.stnWaitDuration = stnWaitDuration;
        }
    }

    public static class RandomCollection<E> {
        private final NavigableMap<Double, E> map = new TreeMap<>();
        private double total = 0;

        public void add(double weight, E result) {
            if (weight <= 0) return;
            total += weight;
            map.put(total, result);
        }

        public E next() {
            double value = ThreadLocalRandom.current().nextDouble() * total;
            return map.ceilingEntry(value).getValue();
        }
    }
}
