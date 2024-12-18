import net.kakoen.arksa.savetools.ArkProfile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

public class TestArkProfileFiles {

    @Test
    @Disabled
    public void canReadAllProfileFiles() throws IOException {
        Files.list(TestConstants.TEST_SAVED_ARKS)
                .filter(path -> path.toString().endsWith(".arkprofile"))
                .forEach(path -> {
                    try {
                        new ArkProfile(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
