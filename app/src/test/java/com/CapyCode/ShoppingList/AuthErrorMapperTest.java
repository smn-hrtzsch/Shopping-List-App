package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthErrorMapperTest {

    @Mock
    Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock specific string resources
        when(mockContext.getString(R.string.error_login_user_not_found)).thenReturn("User not found");
        when(mockContext.getString(R.string.error_login_wrong_password)).thenReturn("Wrong password");
        when(mockContext.getString(R.string.error_login_register_hint)).thenReturn("Please register");
        when(mockContext.getString(R.string.error_email_collision)).thenReturn("Email exists");
    }

    @Test
    public void testUserNotFoundException() {
        FirebaseAuthInvalidUserException e = mock(FirebaseAuthInvalidUserException.class);
        String result = AuthErrorMapper.getErrorMessage(mockContext, e);
        assertEquals("User not found\nPlease register", result);
    }

    @Test
    public void testWrongPasswordException() {
        FirebaseAuthInvalidCredentialsException e = mock(FirebaseAuthInvalidCredentialsException.class);
        String result = AuthErrorMapper.getErrorMessage(mockContext, e);
        assertEquals("Wrong password\nPlease register", result);
    }

    @Test
    public void testEmailCollisionException() {
        FirebaseAuthUserCollisionException e = mock(FirebaseAuthUserCollisionException.class);
        String result = AuthErrorMapper.getErrorMessage(mockContext, e);
        assertEquals("Email exists", result);
    }
}
