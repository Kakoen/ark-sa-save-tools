import net.kakoen.arksa.savetools.ArkProfile;
import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestArkProfiles {

    Path testPath = Path.of("c:\\tmp\\save\\");

    @Test
    public void test() throws IOException {
        // Try to read every .arktribe file by using ArkTribe constructor
        // and print the result
        Files.list(testPath)
                .filter(path -> path.toString().endsWith(".arkprofile"))
                .forEach(path -> {
                    try {
                        ArkProfile profile = new ArkProfile(path);
                        JsonUtils.writeJsonToFile(profile, Path.of(path.toString().replace(".arkprofile", ".json")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

}
