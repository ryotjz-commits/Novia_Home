# Phase 8.6 — Workflow revision verification

This workflow explicitly prints a Phase 8.6 marker and commit SHA, normalizes both root and `app/build.gradle`, verifies the Android module tasks, then builds using the module-qualified task `:app:assembleDebug`.

Mobile update steps:
1. In GitHub repository, open `.github/workflows/android-apk.yml`.
2. Replace the ENTIRE file content with the workflow in this package.
3. Commit the change to the default branch (`main` or `master`).
4. In Actions, open the new run. First step must be `Identify workflow revision` and print `PHASE=8.6`.
5. Then check step `Normalize root and app module Gradle files (Phase 8.6)` and confirm the printed `app/build.gradle` begins with `plugins { id 'com.android.application' }`, WITHOUT version or `apply false`.

If those step names / PHASE marker do not appear, GitHub is running a different workflow file/branch. Do not upload more fixes until this is verified.
