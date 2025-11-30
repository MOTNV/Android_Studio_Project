const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

/**
 * Scheduled function to delete messages older than 30 days.
 * Runs every day at midnight (00:00).
 * Timezone: Asia/Seoul (or default UTC, can be configured)
 */
exports.deleteOldMessages = functions.pubsub.schedule("0 0 * * *")
    .timeZone("Asia/Seoul")
    .onRun(async (context) => {
        const now = admin.firestore.Timestamp.now();
        const thirtyDaysAgo = new Date(now.toDate().getTime() - (30 * 24 * 60 * 60 * 1000));
        const cutoffTimestamp = admin.firestore.Timestamp.fromDate(thirtyDaysAgo);

        console.log(`Deleting messages created before: ${thirtyDaysAgo.toISOString()}`);

        const messagesRef = db.collection("messages");
        const snapshot = await messagesRef.where("timestamp", "<", cutoffTimestamp).get();

        if (snapshot.empty) {
            console.log("No matching documents.");
            return null;
        }

        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
        });

        await batch.commit();
        console.log(`Deleted ${snapshot.size} messages.`);
        return null;
    });
