package com.eventledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test that verifies the Spring application context loads successfully.
 *
 * <p>A failing context load (e.g. misconfigured beans, missing properties)
 * will cause this test to fail, providing early feedback on wiring issues.
 *
 * @author Sarathkumar Ravi
 */
@SpringBootTest
class EventLedgerApplicationTests {

    /**
     * Asserts that the full Spring application context starts without errors.
     */
    @Test
    void contextLoads() {
    }
}
