package com.CapyCode.ShoppingList;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuthValidatorTest {

    @Test
    public void testValidEmail() {
        assertTrue(AuthValidator.isValidEmail("test@example.com"));
        assertTrue(AuthValidator.isValidEmail("user.name+tag@domain.co.uk"));
    }

    @Test
    public void testInvalidEmail() {
        assertFalse(AuthValidator.isValidEmail("test@"));
        assertFalse(AuthValidator.isValidEmail("@domain.com"));
        assertFalse(AuthValidator.isValidEmail("test@domain"));
        assertFalse(AuthValidator.isValidEmail(null));
        assertFalse(AuthValidator.isValidEmail(""));
    }

    @Test
    public void testValidPassword() {
        assertTrue(AuthValidator.isValidPassword("123456"));
        assertTrue(AuthValidator.isValidPassword("password123"));
    }

    @Test
    public void testInvalidPassword() {
        assertFalse(AuthValidator.isValidPassword("12345"));
        assertFalse(AuthValidator.isValidPassword("abc"));
        assertFalse(AuthValidator.isValidPassword(null));
        assertFalse(AuthValidator.isValidPassword(""));
    }
}
