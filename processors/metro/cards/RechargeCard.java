package metro.cards;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class RechargeCard extends VoltProcedure {

    public final SQLStmt updateBalance = new SQLStmt("UPDATE cards SET balance = balance + ? WHERE card_id = ? AND card_type = 0");
    public final SQLStmt getCard = new SQLStmt("SELECT * from cards WHERE card_id = ?");
    public final SQLStmt exportNotif = new SQLStmt("INSERT INTO CARD_ALERT_EXPORT values (?, NOW, ?, ?, ?, ?, ?, ?)");
    public final SQLStmt getStationName = new SQLStmt("SELECT name FROM stations WHERE station_id = ?");
            
    public long run(int cardId, int amt, int stationId) {
        voltQueueSQL(getStationName, stationId);
        voltQueueSQL(getCard, cardId);
        String station = "UNKNOWN";
        
        final VoltTable[] results = voltExecuteSQL();
        if(results.length == 0) 
            exportError(cardId, station);
        
        VoltTable stationResult = results[0];
        if(stationResult.advanceRow()) 
            station = stationResult.getString(0);
        
        VoltTable card = results[1];
        if(card.advanceRow()) {
            voltQueueSQL(updateBalance, amt, cardId);
            
            String name = card.getString(5);
            String phone = card.getString(6);
            String email = card.getString(7);
            int notify = (int) card.getLong(8);
            
            voltQueueSQL(updateBalance, amt, cardId);
            voltQueueSQL(exportNotif, cardId, station, name, phone, email, notify, "Card recharged successfully");
            
            voltExecuteSQL(true);
        } else {
            exportError(cardId, station);
        }
        return 0;
    }
    
    private void exportError(int cardId, String station) {
        exportError(cardId, station, "", "", "", 0, "Could not locate details of card for recharge");
    }
    
    private void exportError(int cardId, String station, String name, String phone, String email, int notify, String msg) {
        voltQueueSQL(exportNotif, cardId, station, name, phone, email, notify, msg);
        voltExecuteSQL(true);
    }
}
