# ConnectHub - review & fix report

See the chat reply for the summary. Highlights are repeated here so they stay with the project.

## Build / project
- Removed duplicated / conflicting dependencies (two `material`, two `firebase-messaging`, old `activity`, unused `firebase-storage`); FCM version now comes from the BOM.
- Manifest: added `POST_NOTIFICATIONS` (no push was ever shown on Android 13+), notification channel + icon meta-data, `windowSoftInputMode` per screen.
- Deleted unused `zego/` package: it contained a hard-coded ZEGO `APP_SIGN` secret. **Rotate that key** - it is still in your git history.
- Deleted dead helper classes (`ChatRepository`, `TypingManager`, `PresenceManager`, `ChatUploadManager`, `VoiceUploadManager`, `WaveformGenerator`).
- The launcher icon was the default green Android robot; replaced with the ConnectHub logo (adaptive + legacy).

## Things you must do
1. Deploy the Cloud Functions (`firebase deploy --only functions`) - group + like/comment/follow pushes are new.
2. Review `docs/firestore.rules.recommended` and apply it (the project had no rules file).
3. Build once in Android Studio (`Build > Rebuild Project`) - this review environment had no Android SDK, so the code was statically checked, not compiled.
