/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2022 Atlan Pte. Ltd. */
package com.probable.guacamole.model.structs;

import com.atlan.model.structs.AtlanStruct;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.processing.Generated;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Complex embedded attributes for Guacamole objects.
 */
@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
@Getter
@Jacksonized
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class GuacamoleStruct extends AtlanStruct {

    public static final String TYPE_NAME = "GuacamoleStruct";

    /** Fixed typeName for GuacamoleStruct. */
    @JsonIgnore
    @Getter(onMethod_ = {@Override})
    @Builder.Default
    String typeName = TYPE_NAME;

    /** TBC */
    String guacamoleComplexOne;

    /** TBC */
    Long guacamoleComplexTwo;

    /**
     * Quickly create a new GuacamoleStruct.
     * @param guacamoleComplexOne TBC
     * @param guacamoleComplexTwo TBC
     * @return a GuacamoleStruct with the provided information
     */
    public static GuacamoleStruct of(String guacamoleComplexOne, Long guacamoleComplexTwo) {
        return GuacamoleStruct.builder()
                .guacamoleComplexOne(guacamoleComplexOne)
                .guacamoleComplexTwo(guacamoleComplexTwo)
                .build();
    }
}
