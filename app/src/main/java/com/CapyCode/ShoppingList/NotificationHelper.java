package com.CapyCode.ShoppingList;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class NotificationHelper {

    private static final String CHANNEL_ID = "invitation_notifications";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_SHOWN_NOTIFICATIONS = "shown_invitations";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showInvitationNotification(Context context, String listId, String listName, String ownerName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> shownNotifications = prefs.getStringSet(KEY_SHOWN_NOTIFICATIONS, new HashSet<>());

        if (shownNotifications.contains(listId)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent acceptIntent = new Intent(context, InvitationActionReceiver.class);
        acceptIntent.setAction("ACCEPT_INVITATION");
        acceptIntent.putExtra("LIST_ID", listId);
        acceptIntent.putExtra("LIST_NAME", listName);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, listId.hashCode(), acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(context, InvitationActionReceiver.class);
        declineIntent.setAction("DECLINE_INVITATION");
        declineIntent.putExtra("LIST_ID", listId);
        declineIntent.putExtra("LIST_NAME", listName);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(context, listId.hashCode() + 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, listId.hashCode() + 2, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_join_list) // Using existing drawable
                .setContentTitle(context.getString(R.string.notification_invitation_title))
                .setContentText(context.getString(R.string.notification_invitation_message, listName, ownerName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_join_list, context.getString(R.string.notification_action_accept), acceptPendingIntent)
                .addAction(R.drawable.ic_decline_list, context.getString(R.string.notification_action_decline), declinePendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(listId.hashCode(), builder.build());
            
            // Mark as shown
            Set<String> newShown = new HashSet<>(shownNotifications);
            newShown.add(listId);
            prefs.edit().putStringSet(KEY_SHOWN_NOTIFICATIONS, newShown).apply();
        } catch (SecurityException e) {
            // Should not happen as we check permission above
        }
    }

    public static void clearShownNotification(Context context, String listId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> shownNotifications = prefs.getStringSet(KEY_SHOWN_NOTIFICATIONS, new HashSet<>());
        if (shownNotifications.contains(listId)) {
            Set<String> newShown = new HashSet<>(shownNotifications);
            newShown.remove(listId);
            prefs.edit().putStringSet(KEY_SHOWN_NOTIFICATIONS, newShown).apply();
        }
    }
}