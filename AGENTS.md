# Working with FxzMusic as an AI agent

FxzMusic is a modern Android music player and YouTube Music streaming client written in Kotlin and Jetpack Compose. It follows Material 3 design guidelines and utilizes AndroidX Media3 for audio playback.

## Project Architecture & Modules

- **`:app`**: Jetpack Compose UI, ViewModels, Room Databases (`AppDatabase`, `LyricsDatabase`), and `PlaybackService`.
- **`:innertube`**: YouTube Music API client, search engines, extractors, and music data models.
- **`:ytpipeline`**: Audio decoding, cipher deciphering, and streaming pipeline.

## Rules for working on the project

1. Always pull the latest changes from `main` before starting your work to minimize merge conflicts.
2. Commit names should be clear and follow the Conventional Commits format: `type(scope): short description`. For example: `feat(ui): add dark mode support` or `fix(player): handle audio focus loss`. Including the scope is optional.
3. All default string additions or edits should be made in `app/src/main/res/values/strings.xml`. Do not add hardcoded string literals directly in Composable screens or UI classes.
4. Follow best practices for Kotlin, Jetpack Compose, and modern Android development (Clean Architecture, State Hoisting, Unidirectional Data Flow).
5. **DO NOT EDIT THE APP'S DATABASE SCHEMA** (Room entities, `AppDatabase`, `LyricsDatabase`) without explicit authorization and proper migration scripts.

## AI-only guidelines

1. You are strictly prohibited from making ANY changes to the readme/markdown files (including this `AGENTS.md`), unless explicitly asked by the user.
2. Unless explicitly requested, you are not allowed to commit, push, or merge any changes to any branch.
   - You should absolutely NOT use any commands that would modify the git history, do force pushes, or delete branches without explicit instructions from a human.
3. Always follow the guidelines and instructions provided by human contributors.
4. Ensure the absolute highest code quality in all contributions, including proper formatting, clear variable naming, and comprehensive comments where necessary.
5. Comments should be added only for complex logic or non-obvious code. Avoid redundant comments that simply restate what the code does.
6. Prioritize playback stability, memory performance, battery efficiency, and UI smoothness (60/120 fps) in all code contributions.
7. If you have any doubts, ask a human contributor. Never make assumptions about the requirements or implementation details without clarification.
8. Always test your changes before claiming task completion.
9. You are absolutely **not allowed to bump the version** of the app in `build.gradle.kts` or manifest files in ANY way unless explicitly instructed. Version bumps are managed manually.

## Building and testing your changes

1. After making changes to the code, build the app to ensure there are no compilation or syntax errors. Use the following command from the root directory of the project:

```bash
./gradlew :app:assembleDebug
```

2. If the build fails, review the compilation error messages, fix the issues in your code, and attempt the build again.
3. Once the build succeeds, the output APK will be available at:
   `app/build/outputs/apk/debug/app-debug.apk`
   Verify your changes or test on an emulator/device whenever feasible.
