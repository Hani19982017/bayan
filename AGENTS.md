# Project Instructions & Persistent Rules

- **APK Generation & Root Placement (MANDATORY)**:
  After every modification, compilation, or build, ALWAYS copy the latest compiled APK from `./app/build/outputs/apk/debug/app-debug.apk` to the project root directory as:
  - `tawthiq.apk`
  - `app-debug.apk`
  so the user can immediately access and download it from the root directory.

- **Design Consistency**:
  - Keep the Top Bar layout matched with:
    - Left: Tawthiq logo (Cloud + Notebook).
    - Right: QR scanner, search, filter, and notification bell with red badge.
  - Typography: Cairo font family with large, clear arabic text sizes.
  - Account Card: Avatar/photo on right with no overlapping blocker, clear name, amount subtitle with direction arrow, and relative time on left.
