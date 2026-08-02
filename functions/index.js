const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({
  maxInstances: 10,
});

exports.sendMessageNotification = onDocumentCreated(
    "Messages/{messageId}",
    async (event) => {
      if (!event.data) return;

      const message = event.data.data();

      if (!message) return;
      const receiverId = message.receiverId;

      const userDoc = await admin.firestore()
          .collection("Users")
          .doc(receiverId)
          .get();

      if (!userDoc.exists) return;

      const token = userDoc.data().fcmToken;

      if (!token) return;

      const senderDoc = await admin.firestore()
          .collection("Users")
          .doc(message.senderId)
          .get();

      const senderName =
            senderDoc.exists ?
                senderDoc.data().username :
                "Someone";

      let body = message.message || "";

      switch (message.type) {
        case "image":
          body = "📷 Image";
          break;

        case "voice":
          body = "🎤 Voice Message";
          break;

        default:
          body = message.message || "New Message";
      }

      await admin.messaging().send({

        token,

        notification: {
          title: senderName,
          body: body,

        },

        data: {
          senderId: message.senderId,
          receiverId: message.receiverId,
        },

        android: {
          priority: "high",
        },

      });

      console.log("Notification sent");
    },
);
