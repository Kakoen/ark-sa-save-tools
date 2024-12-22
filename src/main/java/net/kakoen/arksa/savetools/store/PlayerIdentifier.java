package net.kakoen.arksa.savetools.store;

import lombok.Data;

@Data
public class PlayerIdentifier {
    private long eosId;

    public PlayerIdentifier(long eosId) {
        this.eosId = eosId;
    }
}
