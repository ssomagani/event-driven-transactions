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
package metro;

import static metro.TrainEvent.LastKnownLocation.STATE.STOPPED;

import java.time.LocalDateTime;

import metro.TrainEvent.LastKnownLocation.DIRECTION;
import metro.pub.Stations.Station;

    public class TrainEvent {
        public final int trainId;
        public final LastKnownLocation location;

        public TrainEvent(int trainId, Station station, DIRECTION direction, LocalDateTime lastTimestamp) {
            this(trainId, new LastKnownLocation(station, direction, STOPPED, lastTimestamp));
        }

        public TrainEvent(int trainId, LastKnownLocation location) {
            this.trainId = trainId;
            this.location = location;
        }

        @Override
        public boolean equals(Object other) {
            return ((TrainEvent)other).trainId == this.trainId && ((TrainEvent)other).location.equals(this.location);
        }

        @Override
        public String toString() {
            return "" + trainId + ":" + location.toString();
        }

        public static class LastKnownLocation {

            public final Station station;
            public final DIRECTION direction;
            public final LocalDateTime timestamp;
            public final STATE state;

            public LastKnownLocation(Station station, DIRECTION direction, STATE state, LocalDateTime timestamp) {
                this.station = station;
                this.direction = direction;
                this.state = state;
                this.timestamp = timestamp;
            }

            public Boolean equals(LastKnownLocation other) {
                return (this.station.equals(other.station) &&
                        this.direction.equals(other.direction) &&
                        this.state.equals(other.state));
            }

            @Override
            public String toString() {
                return " " + station.stationId + ":" + direction + ":" + state;
            }

            public enum STATE {
                STOPPED(0), DEPARTED(1);
                public final int value;
                private STATE(int value) {
                    this.value = value;
                }
            }

            public enum DIRECTION {
                FORWARD(Boolean.TRUE), BACKWARD(Boolean.FALSE);
                public final Boolean value;
                private DIRECTION(Boolean value) {
                    this.value = value;
                }
            }
        }

    }
