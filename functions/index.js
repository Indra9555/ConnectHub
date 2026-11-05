const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({
  maxInstances: 10,
});

const db = admin.firestore();

/**
 * Text shown in the notification for a chat message.
 * @param {Object} message the message document
 * @return {string} notification body
 */
function previewOf(message) {
  switch (message.type) {
    case "image":
      return "📷 Photo";
    case "voice":
      return "🎤 Voice message";
    default:
      return message.message || "New message";
  }
}

/**
 * Sends a push to the given users and removes tokens that are no longer valid
 * (uninstalled app / logged-out device), which used to make every later send fail.
 * @param {string[]} userIds users to notify
 * @param {Object} payload notification + data
 * @return {Promise<void>}
 */
async function pushToUsers(userIds, payload) {
  if (!userIds.length) return;

  const docs = await Promise.all(
      userIds.map((id) => db.collection("Users").doc(id).get()),
  );

  const targets = [];

  docs.forEach((doc) => {
    if (doc.exists && doc.data().fcmToken) {
      targets.push({ref: doc.ref, token: doc.data().fcmToken});
    }
  });

  if (!targets.length) return;

  const response = await admin.messaging().sendEach(
      targets.map((target) => ({
        token: target.token,
        notification: payload.notification,
        data: payload.data,
        android: {
          priority: "high",
          notification: {channelId: "messages"},
        },
      })),
  );

  const cleanup = [];

  response.responses.forEach((result, index) => {
    if (!result.success && result.error) {
      const code = result.error.code;

      if (code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token") {
        cleanup.push(
            targets[index].ref.update({
              fcmToken: admin.firestore.FieldValue.delete(),
            }),
        );
      }
    }
  });

  await Promise.all(cleanup);
}

/**
 * Private chat messages.
 */
exports.sendMessageNotification = onDocumentCreated(
    "Messages/{messageId}",
    async (event) => {
      if (!event.data) return;

      const message = event.data.data();

      // Guard: a missing receiverId used to crash the function
      if (!message || !message.receiverId || !message.senderId) return;

      const senderDoc = await db.collection("Users")
          .doc(message.senderId).get();

      const senderName = senderDoc.exists ?
        (senderDoc.data().name || senderDoc.data().username || "Someone") :
        "Someone";

      await pushToUsers([message.receiverId], {
        notification: {title: senderName, body: previewOf(message)},
        data: {
          type: "chat",
          senderId: message.senderId,
          receiverId: message.receiverId,
        },
      });
    },
);

/**
 * Group chat messages (these never produced a notification before).
 */
exports.sendGroupMessageNotification = onDocumentCreated(
    "GroupMessages/{groupId}/Messages/{messageId}",
    async (event) => {
      if (!event.data) return;

      const message = event.data.data();

      if (!message || !message.senderId) return;

      const groupId = event.params.groupId;

      const groupDoc = await db.collection("Groups").doc(groupId).get();

      if (!groupDoc.exists) return;

      const group = groupDoc.data();

      const recipients = (group.members || [])
          .filter((uid) => uid !== message.senderId);

      const senderName = message.senderName || "Someone";

      await pushToUsers(recipients, {
        notification: {
          title: group.groupName || "Group",
          body: `${senderName}: ${previewOf(message)}`,
        },
        data: {
          type: "group",
          groupId: groupId,
          senderId: message.senderId,
        },
      });
    },
);

/**
 * Likes, comments and new followers (in-app notification documents) also as push.
 */
exports.sendSocialNotification = onDocumentCreated(
    "Notifications/{notificationId}",
    async (event) => {
      if (!event.data) return;

      const notification = event.data.data();

      if (!notification || !notification.receiverId ||
          !notification.senderId) return;

      const senderDoc = await db.collection("Users")
          .doc(notification.senderId).get();

      const senderName = senderDoc.exists ?
        (senderDoc.data().name || senderDoc.data().username || "Someone") :
        "Someone";

      let body;

      switch (notification.type) {
        case "like":
          body = "liked your post";
          break;
        case "follow":
          body = "started following you";
          break;
        case "comment":
          body = notification.message || "commented on your post";
          break;
        default:
          body = notification.message || "New activity";
      }

      await pushToUsers([notification.receiverId], {
        notification: {title: senderName, body: body},
        data: {
          type: notification.type || "social",
          senderId: notification.senderId,
          postId: notification.postId || "",
        },
      });
    },
);
