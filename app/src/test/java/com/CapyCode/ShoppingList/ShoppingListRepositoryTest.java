package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShoppingListRepositoryTest {

    @Mock
    Context mockContext;
    @Mock
    FirebaseFirestore mockDb;
    @Mock
    FirebaseAuth mockAuth;

    private ShoppingListRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // We need to avoid real DB helper instantiation in pure unit test if possible,
        // but here it's instantiated in the constructor. 
        // For pure logic tests we might need to inject the helper too.
        repository = new ShoppingListRepository(mockContext, mockDb, mockAuth);
    }

    @Test
    public void testGetCurrentUserId() {
        String uid = "user123";
        when(mockAuth.getUid()).thenReturn(uid);
        assertEquals(uid, repository.getCurrentUserId());
    }
}
