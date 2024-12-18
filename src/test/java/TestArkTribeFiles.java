import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestArkTribeFiles {

    @Test
    @Disabled
    public void canReadAllTribeFiles() throws IOException {
        Files.list(TestConstants.TEST_SAVED_ARKS)
                .filter(path -> path.toString().endsWith(".arktribe"))
                .forEach(path -> {
                    try {
                        new ArkTribe(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
