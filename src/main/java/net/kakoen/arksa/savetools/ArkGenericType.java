package net.kakoen.arksa.savetools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kakoen.arksa.savetools.property.ArkValueType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArkGenericType {

    private String value;
    private List<ArkGenericType> subTypes;

    public ArkGenericType(String value) {
        this.value = value;
    }

    public static ArkGenericType fromValueType(ArkValueType valueType) {
        return new ArkGenericType(valueType.getName());
    }

    public ArkValueType asValueType() {
        return ArkValueType.fromGenericType(this);
    }

    public boolean hasSubTypes() {
        return subTypes != null && !subTypes.isEmpty();
    }
}
