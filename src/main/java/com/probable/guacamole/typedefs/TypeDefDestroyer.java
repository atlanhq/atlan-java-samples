/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.typedefs;

import com.atlan.api.TypeDefsEndpoint;
import com.atlan.exception.AtlanException;
import com.probable.guacamole.AtlanRunner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TypeDefDestroyer extends AtlanRunner {

    public static void main(String[] args) {
        TypeDefDestroyer tdd = new TypeDefDestroyer();
        tdd.purgeTypeDefs();
    }

    void purgeTypeDefs() {
        purgeTypeDef(TypeDefCreator.RELATIONSHIP_DEF_NAME);
        purgeTypeDef(TypeDefCreator.ENTITY_DEF_PARENT_NAME);
        purgeTypeDef(TypeDefCreator.ENTITY_DEF_CHILD_NAME);
        purgeTypeDef(TypeDefCreator.ENUM_DEF_NAME);
        purgeTypeDef(TypeDefCreator.STRUCT_DEF_NAME);
    }

    private void purgeTypeDef(String name) {
        try {
            TypeDefsEndpoint.purgeTypeDef(name);
            log.info("Purged typedef: {}", name);
        } catch (AtlanException e) {
            log.error("Failed to purge typedef: {}", name, e);
        }
    }
}
