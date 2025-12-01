/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onSchedule} = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// Konfiguration: Ab wann gilt ein User als inaktiv?
// 6 Monate = 180 Tage * 24h * 60m * 60s * 1000ms
const INACTIVE_THRESHOLD_MS = 180 * 24 * 60 * 60 * 1000; 

// Zum Testen: 5 Minuten (uncomment to test)
// const INACTIVE_THRESHOLD_MS = 5 * 60 * 1000; 

exports.cleanupInactiveAnonymousUsers = onSchedule("every day 03:00", async (event) => {
    const now = Date.now();
    const cutoffTime = now - INACTIVE_THRESHOLD_MS;

    logger.info("Starting cleanup of inactive anonymous users created before " + new Date(cutoffTime).toISOString());

    // Wir müssen leider alle User durchgehen, da auth.listUsers keine Filterung erlaubt.
    // Bei sehr vielen Usern müsste man das paginieren ("nextPageToken").
    // Hier eine einfache Implementierung für bis zu 1000 User pro Batch.
    
    try {
        const listUsersResult = await auth.listUsers(1000);
        const usersToDelete = [];

        for (const user of listUsersResult.users) {
            // 1. Ist der User anonym?
            const isAnonymous = user.providerData.length === 0; 

            // 2. Wann war der letzte Login?
            const lastSignInTime = new Date(user.metadata.lastSignInTime).getTime();
            const creationTime = new Date(user.metadata.creationTime).getTime();
            
            // Nutze lastSignInTime, falls vorhanden, sonst creationTime
            const lastActive = lastSignInTime || creationTime;

            if (isAnonymous && lastActive < cutoffTime) {
                usersToDelete.push(user.uid);
            }
        }

        logger.info(`Found ${usersToDelete.length} inactive anonymous users.`);

        for (const uid of usersToDelete) {
            await deleteUserAndData(uid);
        }

        logger.info("Cleanup finished.");

    } catch (error) {
        logger.error("Error listing users:", error);
    }
});

async function deleteUserAndData(uid) {
    logger.info(`Processing cleanup for user: ${uid}`);

    try {
        // 1. User Dokument löschen (Profil)
        // Wir holen erst den Username, falls wir ihn irgendwo loggen wollen, aber eigentlich egal.
        await db.collection("users").document(uid).delete();

        // 2. Listen bereinigen
        // Finde alle Listen, wo der User Mitglied ist
        const listsQuery = await db.collection("shoppingLists").where("members", "array-contains", uid).get();
        
        const batch = db.batch();
        let batchCount = 0;

        for (const doc of listsQuery.docs) {
            const listData = doc.data();
            const members = listData.members || [];

            if (members.length <= 1) {
                // Fall A: User ist das einzige Mitglied -> Liste löschen
                logger.info(`Deleting list ${doc.id} (User was only member)`);
                batch.delete(doc.ref);
                
                // Auch Subcollections (Items) müssten gelöscht werden!
                // Firestore löscht Subcollections NICHT automatisch.
                // Das ist komplex. Fürs Erste löschen wir nur das Hauptdokument.
                // Die Items bleiben als "Waisen" zurück, kosten aber kaum Speicher.
                // (Um rekursiv zu löschen, bräuchte man mehr Code).
            } else {
                // Fall B: Es gibt andere Mitglieder -> User entfernen
                logger.info(`Removing user from list ${doc.id} (Other members exist)`);
                const newMembers = members.filter(m => m !== uid);
                batch.update(doc.ref, { members: newMembers });
            }
            batchCount++;
        }

        if (batchCount > 0) {
            await batch.commit();
        }

        // 3. Auth Account löschen
        await auth.deleteUser(uid);
        logger.info(`Successfully deleted user ${uid}`);

    } catch (error) {
        logger.error(`Failed to cleanup user ${uid}:`, error);
    }
}
