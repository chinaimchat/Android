# Android Update Notes (2026-04-21)

This update focuses on registration reliability after fresh install, cross-device layout adaptivity, and recharge flow parity after switching from sheet to full-screen.

## Registration/Login reliability and adaptivity
- Fixed first-launch register page invite-code visibility timing; added config-loading state to avoid incorrect flash before remote config returns.
- Refined login/register input and agreement row behavior for small screens and large-font scenarios.
- Improved field single-line constraints and text ellipsize behavior to prevent row squeeze/overflow.

## Cross-page Android adaptivity fixes
- Updated multiple hard-coded layouts to be more adaptive across screen sizes (RTC call pages, scan page, web-login auth, PC-login view, file preview, user/group QR pages, moments publish/header, floating call, and related wallet pages).
- Replaced risky fixed spacing patterns and improved text truncation behavior where needed.

## Wallet / Recharge experience
- Switched Wallet quick recharge from half-sheet to full-screen activity.
- Restored parity features in full-screen recharge page:
  - QR display
  - Copy address
  - Save QR
  - Persistent channel row (aligned with sheet behavior)
  - Top-right Orders entry
  - Floating Contact Customer Service entry
- Ensured QR behavior matches sheet behavior (prefer server channel QR when available, fallback only when necessary).
- Fixed overlap between Confirm Recharge button and floating customer-service button by moving confirm action area upward.

## Visual consistency updates
- Payment password dots now show empty/filled states aligned with iOS style.
- Red packet opening dialog visual/interaction tuned closer to iOS (size/radius/close behavior/window params).

