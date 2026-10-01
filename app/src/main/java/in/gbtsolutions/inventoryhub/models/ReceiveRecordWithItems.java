package in.gbtsolutions.inventoryhub.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class ReceiveRecordWithItems {
    @Embedded
    public ReceiveRecord record;

    @Relation(
            parentColumn = "receive_record_id",
            entityColumn = "receive_record_id"
    )
    public List<ReceiveItem> items;
}
