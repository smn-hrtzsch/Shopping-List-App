package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UserRepositoryTest {

    @Mock
    Context mockContext;
    @Mock
    FirebaseFirestore mockDb;
    @Mock
    FirebaseAuth mockAuth;
    @Mock
    FirebaseStorage mockStorage;
    @Mock
    FirebaseUser mockUser;

    private UserRepository userRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userRepository = new UserRepository(mockContext, mockDb, mockAuth, mockStorage);
    }

    @Test
    public void testIsAuthenticated_True() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        assertTrue(userRepository.isAuthenticated());
    }

    @Test
    public void testIsAuthenticated_False() {
        when(mockAuth.getCurrentUser()).thenReturn(null);
        assertFalse(userRepository.isAuthenticated());
    }

    @Test
    public void testGetCurrentUserId() {
        String uid = "test-uid-123";
        when(mockAuth.getUid()).thenReturn(uid);
        assertEquals(uid, userRepository.getCurrentUserId());
    }
}
