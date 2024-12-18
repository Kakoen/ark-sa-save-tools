package net.kakoen.arksa.savetools.store;

import lombok.Data;


@Data
public class TribeIdentifier {

    private long tribeId;

    public TribeIdentifier(long tribeId) {
        this.tribeId = tribeId;
    }
}
