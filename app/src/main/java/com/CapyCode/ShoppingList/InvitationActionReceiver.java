package com.CapyCode.ShoppingList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

public class InvitationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String listId = intent.getStringExtra("LIST_ID");
        String listName = intent.getStringExtra("LIST_NAME");
        String action = intent.getAction();

        if (listId == null || action == null) return;

        // Cancel notification immediately
        NotificationManagerCompat.from(context).cancel(listId.hashCode());

        final PendingResult pendingResult = goAsync();
        ShoppingListRepository repository = new ShoppingListRepository(context);

        if ("ACCEPT_INVITATION".equals(action)) {
            repository.acceptInvitation(listId, new UserRepository.OnProfileActionListener() {
                @Override
                public void onSuccess() {
                    pendingResult.finish();
                }

                @Override
                public void onError(String message) {
                    pendingResult.finish();
                }
            });
        } else if ("DECLINE_INVITATION".equals(action)) {
            repository.declineInvitation(listId, new UserRepository.OnProfileActionListener() {
                @Override
                public void onSuccess() {
                    pendingResult.finish();
                }

                @Override
                public void onError(String message) {
                    pendingResult.finish();
                }
            });
        } else {
            pendingResult.finish();
        }
    }
}
