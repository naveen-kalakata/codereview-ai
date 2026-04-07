package com.naveen.codereviewai.learning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) — tells JUnit:
// "Before each test, initialize all @Mock and @Spy fields"
// Without this, the @Mock fields would be null
@ExtendWith(MockitoExtension.class)
class MockitoBasicsTest {

    // ============================================================
    // PART 1: @Mock — creates a COMPLETELY FAKE object
    // ============================================================

    // Let's say we have a service that depends on an EmailService.
    // We don't want our test to actually send emails.

    // This is an interface — in real code, this would connect to Gmail/SMTP
    interface EmailService {
        boolean sendEmail(String to, String subject, String body);
        int getEmailCount();
    }

    // This is the class we're ACTUALLY testing
    static class UserService {
        private final EmailService emailService;

        UserService(EmailService emailService) {
            this.emailService = emailService;
        }

        public String registerUser(String email) {
            // Sends a welcome email
            boolean sent = emailService.sendEmail(email, "Welcome!", "Thanks for joining");
            if (sent) {
                return "User registered and email sent";
            }
            return "User registered but email failed";
        }
    }

    // @Mock creates a FAKE EmailService
    // - sendEmail() returns false by default (all methods return default values)
    // - No actual email is ever sent
    // - It's an empty shell that we control
    @Mock
    EmailService mockEmailService;

    @Test
    void mock_returnsDefaultValues() {
        // Without any setup, mock methods return defaults:
        // boolean → false, int → 0, Object → null
        assertFalse(mockEmailService.sendEmail("a", "b", "c"));
        assertEquals(0, mockEmailService.getEmailCount());
    }

    @Test
    void when_thenReturn_controlsWhatMockReturns() {
        // when().thenReturn() — "WHEN sendEmail is called, THEN RETURN true"
        // This is how you control the fake object's behavior
        when(mockEmailService.sendEmail("naveen@test.com", "Welcome!", "Thanks for joining"))
                .thenReturn(true);

        // Now create the real service with the fake emailService
        UserService userService = new UserService(mockEmailService);
        String result = userService.registerUser("naveen@test.com");

        // The real UserService called emailService.sendEmail() → got true (from our mock)
        assertEquals("User registered and email sent", result);
    }

    @Test
    void when_emailFails_handledGracefully() {
        // Simulate email failure — sendEmail returns false
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(false);
        //                  ^^^^^^^^^^
        // anyString() — matches ANY string argument
        // Other matchers: anyInt(), anyList(), any(MyClass.class)

        UserService userService = new UserService(mockEmailService);
        String result = userService.registerUser("anyone@test.com");

        assertEquals("User registered but email failed", result);
    }

    // ============================================================
    // PART 2: verify() — "was this method actually called?"
    // ============================================================

    @Test
    void verify_methodWasCalled() {
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);

        UserService userService = new UserService(mockEmailService);
        userService.registerUser("naveen@test.com");

        // verify() — "I want to confirm that sendEmail was called exactly once"
        verify(mockEmailService).sendEmail("naveen@test.com", "Welcome!", "Thanks for joining");

        // You can also verify call counts:
        // verify(mockEmailService, times(1)).sendEmail(...);     // called exactly once
        // verify(mockEmailService, never()).getEmailCount();     // was NEVER called
        // verify(mockEmailService, atLeast(2)).sendEmail(...);   // called 2+ times
    }

    @Test
    void verify_methodWasNeverCalled() {
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);

        UserService userService = new UserService(mockEmailService);
        userService.registerUser("test@test.com");

        // getEmailCount() should never be called during registration
        verify(mockEmailService, never()).getEmailCount();
    }

    // ============================================================
    // PART 3: ArgumentCaptor — "WHAT was passed to the mock?"
    // ============================================================

    @Test
    void argumentCaptor_capturesWhatWasPassed() {
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);

        UserService userService = new UserService(mockEmailService);
        userService.registerUser("naveen@test.com");

        // ArgumentCaptor — captures the actual argument value
        // "I don't just want to know sendEmail was called,
        //  I want to know EXACTLY what 'to' address was passed"
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockEmailService).sendEmail(
                toCaptor.capture(),       // capture 1st argument
                subjectCaptor.capture(),  // capture 2nd argument
                anyString()               // don't care about 3rd
        );

        assertEquals("naveen@test.com", toCaptor.getValue());
        assertEquals("Welcome!", subjectCaptor.getValue());
    }

    // ============================================================
    // PART 4: thenThrow — "simulate an error"
    // ============================================================

    @Test
    void when_thenThrow_simulatesErrors() {
        // Simulate: email server is down → throws RuntimeException
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("SMTP server down"));

        UserService userService = new UserService(mockEmailService);

        // Now when registerUser tries to send email, it blows up
        assertThrows(RuntimeException.class, () -> {
            userService.registerUser("naveen@test.com");
        });
    }

    // ============================================================
    // PART 5: @Spy vs @Mock — the difference
    // ============================================================

    // @Mock  = COMPLETELY FAKE object — all methods return defaults
    // @Spy   = REAL object — all methods work normally, but you CAN override specific ones

    @Spy
    ArrayList<String> spyList = new ArrayList<>();

    @Test
    void spy_usesRealMethods() {
        // Spy uses the REAL ArrayList methods
        spyList.add("hello");
        spyList.add("world");

        assertEquals(2, spyList.size());         // real size() works
        assertEquals("hello", spyList.get(0));   // real get() works
    }

    @Test
    void spy_canOverrideSpecificMethods() {
        spyList.add("hello");

        // Override ONLY size() — everything else stays real
        when(spyList.size()).thenReturn(999);

        assertEquals(999, spyList.size());         // returns our fake value
        assertEquals("hello", spyList.get(0));     // still works normally
    }

    // ============================================================
    // PART 6: Matching arguments — anyString, eq, argThat
    // ============================================================

    @Test
    void argumentMatchers() {
        // anyString() — matches any string
        when(mockEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);

        assertTrue(mockEmailService.sendEmail("a", "b", "c"));
        assertTrue(mockEmailService.sendEmail("x", "y", "z"));
        // Both return true because anyString() matches everything
    }

    @Test
    void mixingMatchersAndValues() {
        // RULE: if you use ANY matcher, ALL arguments must be matchers
        // You can't do: when(mock.method("literal", anyString()))  ← WRONG
        // You must do:  when(mock.method(eq("literal"), anyString()))  ← RIGHT

        when(mockEmailService.sendEmail(eq("naveen@test.com"), anyString(), anyString()))
                .thenReturn(true);
        //                               ^^
        // eq() — "match this exact value" (used when mixing matchers with literals)

        assertTrue(mockEmailService.sendEmail("naveen@test.com", "any subject", "any body"));
        assertFalse(mockEmailService.sendEmail("other@test.com", "any subject", "any body"));
        // Second call returns false — "other@test.com" doesn't match eq("naveen@test.com")
    }
}
