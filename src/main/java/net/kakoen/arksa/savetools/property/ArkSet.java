package net.kakoen.arksa.savetools.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkSet {
    ArkValueType valueType;
    Set<?> values;
}
