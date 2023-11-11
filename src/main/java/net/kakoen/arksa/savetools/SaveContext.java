package net.kakoen.arksa.savetools;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SaveContext {

    private Map<Integer, String> names;
    private List<String> parts;
}
