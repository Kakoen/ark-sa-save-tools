package net.kakoen.arksa.savetools.struct;

import lombok.Data;

@Data
public class UnknownStruct {

    private String structType;
    private String value;
    public UnknownStruct(String structType, String value) {
        this.structType = structType;
        this.value = value;
    }
}
