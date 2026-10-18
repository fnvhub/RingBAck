# RingBack V3

RingBack is a private Android callback reminder assistant. It does **not** replace the normal Phone/Dialer app.

## V3 changes

- Keeps the normal phone app as the phone app.
- Uses `CallScreeningService` to capture the number and direction of incoming/outgoing cellular calls.
- Uses `PHONE_STATE` only to detect the call lifecycle and its end.
- Correctly distinguishes answered incoming calls from missed calls using RINGING/OFFHOOK/IDLE state.
- Measures answered-call duration from the OFFHOOK moment instead of counting ringing time.
- Post-call choices: 5, 10, 15, 30 minutes, 1, 2 or 4 hours, tomorrow at the same time, or a custom date/time.
- Manual call scheduling with contact selection or a typed number.
- Main screen focused on **Llamadas pendientes**, with Call now / Postpone / Delete actions.
- Work schedule limits only automatic post-call pop-ups; manual reminders are still honored at the chosen time.
- Exact alarms are used when Android grants the special Alarms & reminders access; otherwise RingBack falls back to an inexact alarm rather than crashing.
- Pending reminders are restored after reboot or app update.
- `READ_CALL_LOG` is not used.

## Build

The GitHub Actions workflow builds the debug APK online with Gradle 8.9 and Android SDK 35.

Artifact: `RingBack-debug-apk`.
