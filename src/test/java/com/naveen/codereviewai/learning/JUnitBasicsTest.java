package com.naveen.codereviewai.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

// This is a plain Java class. No Spring, no magic.
// JUnit scans for @Test methods and runs them one by one.
class JUnitBasicsTest {

    // ============================================================
    // PART 1: Basic Assertions — "does this value match what I expect?"
    // ============================================================

    @Test
    void simpleAddition() {
        // assertEquals(expected, actual)
        // "I EXPECT 5, and the ACTUAL result should be 5"
        int result = 2 + 3;
        assertEquals(5, result);
        // If result is NOT 5, the test FAILS with: "Expected 5 but was ___"
    }

    @Test
    void stringEquality() {
        String name = "Naveen";
        assertEquals("Naveen", name);        // passes — same content
        // assertEquals("naveen", name);      // would FAIL — case sensitive
    }

    @Test
    void booleanChecks() {
        assertTrue(10 > 5);          // "I assert this is true"
        assertFalse(10 < 5);         // "I assert this is false"
    }

    @Test
    void nullChecks() {
        String exists = "hello";
        String doesNotExist = null;

        assertNotNull(exists);       // passes — it's not null
        assertNull(doesNotExist);    // passes — it IS null
    }

    // ============================================================
    // PART 2: Testing Exceptions — "does this code throw when it should?"
    // ============================================================

    @Test
    void divisionByZero_shouldThrow() {
        // assertThrows(ExceptionType, lambda)
        // "I expect this code to throw ArithmeticException"
        assertThrows(ArithmeticException.class, () -> {
            int result = 10 / 0;  // this will throw
        });
    }

    @Test
    void nullString_shouldThrow() {
        String s = null;
        assertThrows(NullPointerException.class, () -> {
            s.length();  // calling method on null → NPE
        });
    }

    // ============================================================
    // PART 3: Lifecycle Annotations — "run this BEFORE/AFTER tests"
    // ============================================================

    // Imagine you're testing a bank account:
    // Before each test, you want a fresh account with $100
    // After each test, you want to log what happened

    private int balance;

    @BeforeEach
        // runs BEFORE every single @Test method
    void setUp() {
        balance = 100;  // fresh start for each test
        System.out.println("--- Starting new test with balance: " + balance);
    }

    @AfterEach
        // runs AFTER every single @Test method
    void tearDown() {
        System.out.println("--- Test finished with balance: " + balance);
    }

    @Test
    void deposit() {
        balance += 50;
        assertEquals(150, balance);
        // setUp() gave us 100, we added 50 → 150
    }

    @Test
    void withdraw() {
        balance -= 30;
        assertEquals(70, balance);
        // setUp() gave us 100, we subtracted 30 → 70
        // NOTE: this test does NOT see the deposit from the test above
        // Each test starts fresh because @BeforeEach resets balance to 100
    }

    // ============================================================
    // PART 4: @DisplayName — makes test output readable
    // ============================================================

    @Test
    @DisplayName("Empty string should have length 0")
    void emptyStringLength() {
        assertEquals(0, "".length());
    }

    @Test
    @DisplayName("List.of() creates an immutable list")
    void immutableList() {
        var list = java.util.List.of("a", "b");
        assertEquals(2, list.size());
        // Can't add to an immutable list
        assertThrows(UnsupportedOperationException.class, () -> {
            list.add("c");
        });
    }
}
