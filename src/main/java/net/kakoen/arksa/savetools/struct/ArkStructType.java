package net.kakoen.arksa.savetools.struct;

import lombok.Getter;
import net.kakoen.arksa.savetools.ArkBinaryData;

import java.util.function.Function;

public enum ArkStructType {

    LinearColor("LinearColor", ArkLinearColor::new),
    Quat("Quat" ,ArkQuat::new),
    Vector("Vector", ArkVector::new),
    Rotator("Rotator", ArkRotator::new),
    UniqueNetIdRepl("UniqueNetIdRepl", ArkUniqueNetIdRepl::new),
    Color("Color", ArkColor::new),
    IntPoint("IntPoint", IntPoint::new)

    ;

    @Getter
    private String typeName;

    @Getter
    private final Function<ArkBinaryData, Object> constructor;

    private ArkStructType(String typeName, Function<ArkBinaryData, Object> constructor) {
        this.typeName = typeName;
        this.constructor = constructor;
    }

    public static ArkStructType fromTypeName(String typeName) {
        for (ArkStructType structType : ArkStructType.values()) {
            if (structType.getTypeName().equals(typeName)) {
                return structType;
            }
        }
        return null;
    }

}
