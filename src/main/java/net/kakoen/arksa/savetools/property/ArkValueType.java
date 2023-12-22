package net.kakoen.arksa.savetools.property;

import lombok.Getter;

import java.math.BigInteger;
import java.util.List;

@Getter
public enum ArkValueType {
    Boolean("BoolProperty", Boolean.class),
    Byte("ByteProperty", Byte.class),
    Float("FloatProperty", Float.class),
    Int("IntProperty", Integer.class),
    Name("NameProperty", String.class),
    Object("ObjectProperty", String.class),
    String("StrProperty", String.class),
    Struct("StructProperty", Object.class), //FIXME
    Array("ArrayProperty", List.class),
    Double("DoubleProperty", Double.class),
    Int16("Int16Property", Short.class),
    Int64("Int64Property", Long.class),
    Int8("Int8Property", Byte.class),
    UInt16("UInt16Property", Integer.class),
    UInt32("UInt32Property", Long.class),
    UInt64("UInt64Property", BigInteger.class),
    SoftObject("SoftObjectProperty", String.class),
    Set("SetProperty", ArkSet.class),
    Map("MapProperty", ArkProperty.class); //FIXME

    private final String name;
    private final Class<?> clazz;

    ArkValueType(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public static ArkValueType fromName(String name) {
        for(ArkValueType type : values()) {
            if(type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
