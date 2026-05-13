package com.iae;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppTest {
    @Test
    void applicationClassCanBeLoaded() {
        assertDoesNotThrow(() -> Class.forName("com.iae.MainApp"));
    }
}
