# Phase 8 · Agent B: Unggah video (7.3)

Worktree `D:\PROJECT\BudayaGo\m\p8-video` (branch `feat/unggah-video`). Read `docs/agents/phase8-common.md` first; it is binding.

## Screen
| No. | Screen | Artifact file | Module |
|---|---|---|---|
| 7.3 | Unggah video | `UnggahVideoIlustratif.dc.html` | `:feature:postcard` |

Reached from the "Video" button on Tulis postcard (7.1, already in `:feature:postcard`, see `TulisPostcardIlustratif.dc.html`, the button links to `UnggahVideoIlustratif`). Wire that button.

## What it does
- Pick a video with the Android Photo Picker (`PickVisualMedia` VideoOnly; no storage permission). Show the place it is for ("Direkam di <place>"), a caption field, the three "Sebelum mengunggah" checkboxes (all three must be ticked before "Unggah video" is enabled), and "Unggah video".
- The max duration shows `[ISI]` in the artifact: put it in ONE named constant marked PROVISIONAL and validate the picked video's duration against it (read with `MediaMetadataRetriever`); never render the literal `[ISI]`.
- Login required (use the existing `requireLogin` hook from `:core:ui`). Only accounts with a stamp for that place may post (RLS enforces it on the server; the client shows a clear "ambil cap dulu" state when the repository says no stamp, check how 7.1 handles it and do the same).
- Stories go live immediately (no moderation step in the UI).
- The video host (Cloudflare Stream or Mux) is not chosen yet. Add a `VideoUploadRepository` interface in a NEW file in `:core:model` and a Fake implementation in a NEW file in `:core:data` (with a NEW Hilt module file), shaped so a real resumable upload can replace it: input = content Uri + placeId + caption, output = progress Flow then success / failure (not stamped, too long, network). Say plainly in the report that upload is fake.
- States: no video picked, picked, too long, uploading with progress, success (back to the story or Cerita), failure with retry, offline.

## Files you may touch
`:feature:postcard`; NEW files in `:core:model`, `:core:data` (incl. a new Hilt module), `:core:navigation` (a new route file, e.g. `VideoRoutes.kt`); one-line registration in `android/app/.../KelanaNavHost.kt`.

## Done means
`assembleDebug lintDebug testDebugUnitTest` pass. ViewModel test covers: upload disabled until all three checks and a video, too-long video, progress then success, failure then retry, not-stamped.
