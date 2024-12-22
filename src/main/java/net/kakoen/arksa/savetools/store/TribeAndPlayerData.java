package net.kakoen.arksa.savetools.store;

import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkProfile;
import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.SaveContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles reading tribe and player data when the -usestore feature is enabled.
 * Using this flag stores the data in the save file rather than creating .arkprofile and .arktribe files.
 */
public class TribeAndPlayerData {

    private static final int HEADER_OFFSET_ADJUSTMENT = 4;
    private static final int TRIBE_HEADER_BASE_OFFSET = 12;

    private final ArkBinaryData data;
    private final Map<TribeIdentifier, OffsetAndSize> tribeDataPointers = new HashMap<>();
    private final Map<PlayerIdentifier, OffsetAndSize> playerDataPointers = new HashMap<>();

    public TribeAndPlayerData(ArkBinaryData storeData) {
        this.data = storeData;
        initializeData();
    }

    private void initializeData() {
        boolean something = data.readBoolean();
        readTribeHeaders();
        readPlayerHeaders();
    }

    private void readPlayerHeaders() {
        int playerHeaderStart = data.readInt() + data.getPosition() + HEADER_OFFSET_ADJUSTMENT;
        int playerCount = data.readInt();
        if (playerCount == 0) {
            return;
        }
        int playerDataStart = data.getPosition();

        data.setPosition(playerHeaderStart);
        for (int i = 0; i < playerCount; i++) {
            long eosId = data.readLong();
            int offset = data.readInt() + playerDataStart;
            int size = data.readInt();
            playerDataPointers.put(new PlayerIdentifier(eosId), new OffsetAndSize(offset, size));
        }
    }

    private void readTribeHeaders() {
        int tribeHeaderStart = data.readInt() + TRIBE_HEADER_BASE_OFFSET;
        int tribeCount = data.readInt();
        if (tribeCount == 0) {
            return;
        }
        int tribeDataStart = data.getPosition();

        data.setPosition(tribeHeaderStart);
        for (int i = 0; i < tribeCount; i++) {
            long tribeId = data.readUInt32();
            int something = data.readInt();
            int offset = data.readInt() + tribeDataStart;
            int size = data.readInt();
            tribeDataPointers.put(new TribeIdentifier(tribeId), new OffsetAndSize(offset, size));
        }
    }

    public Set<TribeIdentifier> getTribeIdentifiers() {
        return tribeDataPointers.keySet();
    }

    public Set<PlayerIdentifier> getPlayerIdentifiers() {
        return playerDataPointers.keySet();
    }

    public ArkTribe getArkTribe(TribeIdentifier tribeIdentifier) {
        OffsetAndSize offsetAndSize = tribeDataPointers.get(tribeIdentifier);
        return offsetAndSize == null ? null : readArkTribe(offsetAndSize);
    }

    private ArkTribe readArkTribe(OffsetAndSize offsetAndSize) {
        data.setPosition(offsetAndSize.offset());
        return new ArkTribe(data, new SaveContext());
    }

    public ArkProfile getArkProfile(PlayerIdentifier playerIdentifier) {
        OffsetAndSize offsetAndSize = playerDataPointers.get(playerIdentifier);
        return offsetAndSize == null ? null : readArkProfile(offsetAndSize);
    }

    private ArkProfile readArkProfile(OffsetAndSize offsetAndSize) {
        data.setPosition(offsetAndSize.offset());
        return new ArkProfile(data, new SaveContext());
    }

    /**
     * Raw data as it would be in the .arktribe file
     */
    public byte[] getArkTribeRawData(TribeIdentifier tribeId) {
        //Compute hash over tribe data
        OffsetAndSize offsetAndSize = tribeDataPointers.get(tribeId);
        if (offsetAndSize == null) {
            return null;
        }
        data.setPosition(offsetAndSize.offset());
        return data.readBytes(offsetAndSize.size());
    }

    /**
     * Raw data as it would be in the .arkprofile file
     */
    public byte[] getArkProfileRawData(PlayerIdentifier playerId) {
        //Compute hash over player data
        OffsetAndSize offsetAndSize = playerDataPointers.get(playerId);
        if (offsetAndSize == null) {
            return null;
        }
        data.setPosition(offsetAndSize.offset());
        return data.readBytes(offsetAndSize.size());
    }
}
