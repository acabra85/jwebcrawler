package com.acabra.jwebcrawler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    public void should_fail_main() {
        NullPointerException npe = Assertions.assertThrows(NullPointerException.class, () -> Main.main(""));
        Assertions.assertTrue(npe.getMessage().contains("Sub-domain not found:"));
    }

    @Test
    public void should_succeed_main() {
        Main.main("http://localhost:8000");
    }
}