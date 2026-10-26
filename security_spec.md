# Security Specification — Thumbnail AI Studio

## 1. Data Invariants
1. **User Isolation**: Every document in `/users/{userId}`, `/users/{userId}/projects/{projectId}`, `/users/{userId}/history/{historyId}`, and `/users/{userId}/brand_kits/{brandKitId}` is strictly readable and writable only by the authenticated user where `request.auth != null && request.auth.uid == userId`.
2. **Identity Integrity**: Every document payload must include `userId == request.auth.uid` on creation, and `userId` and `createdAt` are immutable on updates (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
3. **Path Variable Hardening**: All single-document operations (`get`, `create`, `update`, `delete`) validate path IDs with `isValidId(id)` (`^[a-zA-Z0-9_\-]+$`, max 128 chars).
4. **Strict Key & Type Validation**: Every `create` enforces `hasAll` and `hasOnly` on `incoming().keys()`, and every `update` starts with `isValid[Entity](incoming())` and enforces `incoming().diff(existing()).affectedKeys().hasOnly(...)`.
5. **Bounded Arrays**: `layersSummary` on `ThumbnailProject` is strictly bounded to `size() <= 25` and validates element type `(size() == 0 || layersSummary[0] is string)`.
6. **Temporal Integrity**: `createdAt` and `updatedAt` are validated with `is timestamp && <= request.time`.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get` or `list` on `/users/alice_123/projects`.
2. **Cross-User Project Read**: `bob_456` reading `/users/alice_123/projects/proj_1`.
3. **Cross-User Project Write**: `bob_456` creating a document under `/users/alice_123/projects/proj_2`.
4. **UID Spoofing in Body**: `alice_123` writing to `/users/alice_123/projects/proj_1` with `userId: "bob_456"`.
5. **Shadow Field Injection (Create)**: Adding `"isAdmin": true` or `"isVerified": true` to a `ThumbnailProject` creation payload.
6. **Shadow Field Injection (Update)**: Updating a `ThumbnailProject` with an undeclared field `"hacked": "yes"`.
7. **Immutable Field Mutation**: Updating `createdAt` or `userId` on an existing `ThumbnailProject` or `BrandKit`.
8. **Value Poisoning (Type Mismatch)**: Updating `width` on `ThumbnailProject` with `"wide"` (string instead of int) or `999999` (out of range).
9. **Oversized String DoS**: Sending a 5000-character string in `title` on `ThumbnailProject`.
10. **Unbounded Array DoS**: Sending 30 items in `layersSummary` (exceeding max 25).
11. **Malformed Document ID**: Creating a project with ID containing spaces or special characters (`proj$bad!`).
12. **History Mutation**: Attempting to `update` an append-only `GenerationHistoryItem` record.
