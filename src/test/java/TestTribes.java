import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestTribes {

    Path testPath = Path.of("c:\\tmp\\save\\");

    @Test
    @Disabled
    public void test() throws IOException {
        // Try to read every .arktribe file by using ArkTribe constructor
        // and print the result
        Files.list(testPath)
                .filter(path -> path.toString().endsWith(".arktribe"))
                .forEach(path -> {
                    try {
                        ArkTribe tribe = new ArkTribe(path);
                        JsonUtils.writeJsonToFile(tribe, Path.of(path.toString().replace(".arktribe", ".json")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

}
