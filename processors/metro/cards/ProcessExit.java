package metro.cards;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class ProcessExit extends VoltProcedure {
    private static byte ACTIVITY_INVALID = 0;
    private static byte ACTIVITY_EXIT = -1;
    private static byte ACTIVITY_ENTER = 1;

    public static byte ACTIVITY_ACCEPTED = 1;
    public static byte ACTIVITY_REJECTED = 0;
    public static byte ACTIVITY_FRAUD = -1;

    public final SQLStmt insertActivity = new SQLStmt(
            "INSERT INTO card_events (card_id, date_time, station_id, activity_code, amount, accept) VALUES (?,?,?,?,?,?);");

    public final SQLStmt checkCard = new SQLStmt(
            "SELECT enabled, card_type, balance, expires, name, phone, email, notify FROM cards WHERE card_id = ?;");

    public final SQLStmt checkStationFare = new SQLStmt(
            "SELECT fare, name FROM stations WHERE station_id = ?;");

    public final SQLStmt getTrip = new SQLStmt(
            "select concat(CAST(? as VARCHAR), '_', CAST(max(entry_id) as VARCHAR)) as trip_id, "
            + "max(date_time) as entry_time, "
            + "max(station_id) as entry_station "
            + "from "
            + "(select date_time, station_id, amount, ROW_NUMBER() over (partition by card_id) as entry_id "
            + "from card_events "
            + "where card_id = ? and date_time < ? and activity_code = 1 order by date_time) t"
            );

    public final SQLStmt publishTrip = new SQLStmt(
            "INSERT INTO trips (trip_id, card_id, entry_station, entry_time, exit_station, exit_time) values (?, ?, ?, ?, ?, ?);"
            );

    final VoltTable resultTemplate = new VoltTable(
            new VoltTable.ColumnInfo("card_accepted",VoltType.TINYINT),
            new VoltTable.ColumnInfo("message",VoltType.STRING));

    public VoltTable buildResult(int accepted, String msg) {
        VoltTable r = resultTemplate.clone(64);
        r.addRow(accepted, msg);
        return r;
    }

    public VoltTable run(int cardId, long tsl, int stationId, byte activity_code, int amt) throws VoltAbortException {
        voltQueueSQL(insertActivity, cardId, tsl, stationId, activity_code, 0, ACTIVITY_ACCEPTED);
        voltQueueSQL(getTrip, cardId, cardId, tsl);
        final VoltTable[] results = voltExecuteSQL();
        final VoltTable tripRow = results[1];
        if(tripRow.advanceRow()) {
            String tripId = tripRow.getString(0);
            if(tripId != null) {
                TimestampType entryTime = tripRow.getTimestampAsTimestamp(1);
                int entryStation = (int) tripRow.getLong(2);
                voltQueueSQL(publishTrip, tripId, cardId, entryStation, entryTime, stationId, new TimestampType(tsl));
                voltExecuteSQL(true);
            }
        }
        return buildResult(1, "");
    }
}
