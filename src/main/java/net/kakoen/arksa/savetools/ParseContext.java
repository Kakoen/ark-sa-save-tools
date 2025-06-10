package net.kakoen.arksa.savetools;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParseContext {
    private ArchiveType archiveType = ArchiveType.UNKNOWN;
    private int archiveVersion;

    public boolean useUE55Structure() {
        return
            ge(ArchiveType.CRYOPOD, 0x0407) ||
            ge(ArchiveType.ARK_ARCHIVE, 7) ||
            ge(ArchiveType.SAVE, 14);
    }

    public boolean ge(ArchiveType archiveType, int minVersion) {
        return this.archiveType == archiveType && archiveVersion >= minVersion;
    }
}
