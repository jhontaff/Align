# 0023 — Conversation memory and chat history

- **Status:** Accepted
- **Date:** 2026-08
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


`ConversationMemory` persists and reloads conversation turns per user, so `AgentService` is not stateless across requests. It depends only on `Message` (provider-neutral), never on provider DTOs — same rule as the rest of the AI layer.

Design decisions currently in place:

- One conversation per user. There is no session/thread concept yet; introduce one only when a real requirement for multiple concurrent conversations shows up, not before.
- The system prompt is never persisted. `AgentService` rebuilds it from `SystemPromptBuilder` on every request and prepends it to the loaded history, so prompt changes never leave stale system messages in old users' history.
- Only a completed turn is persisted — the final assistant response with no pending tool calls. A turn that exhausts `MAX_STEPS` without resolving is discarded rather than saved half-finished.
- History is stored as a single serialized `List<Message>` per user (`ConversationHistory.historyJson`), not as one row per message. This is the minimal shape that solves today's requirement (remembering the conversation); move to a row-per-message model only if querying or trimming history becomes a real need, not preemptively.
- A failure to persist a turn (`append`) propagates and fails the request; the assistant's reply is not returned to the user in that case. This is intentional: silently swallowing a persistence failure would mask real infrastructure problems and let the agent forget a turn without anyone noticing.

## Agent chat & history (`ai.agent`)

Current coverage: `POST /api/agent/chat` (existing) and `GET /api/agent/history` (new — added because the frontend needs it to restore the conversation view on load). Both endpoints are thin on `AgentController`, delegating to `AgentService`.

- Done: `AgentResponse` moved from a flat file in `ai.agent` into `ai.agent.dto`, alongside two new records: `ChatTurn(String role, String content)` and `ChatHistoryResponse(List<ChatTurn> turns)`. `AgentService.getHistory(User user)` returns `ChatHistoryResponse`, never the neutral `Message` type the AI layer uses internally — `Message` is shaped for the LLM round-trip (roles `system`/`user`/`assistant`/`tool`, `toolCalls`, `toolCallId`), not for a chat UI, so it doesn't cross the REST boundary directly. Same reasoning as why domain entities never leave a controller as-is.
- Done: `AgentServiceImpl.getHistory` delegates to `conversationMemory.loadHistory(user)` and maps each `Message` to a `ChatTurn` via a private exhaustive `switch` (`toChatTurn`) — the same idiom `GeminiLlmClient.toGeminiContent` already uses to map `Message` toward the provider. No MapStruct: the AI layer has no precedent for it anywhere (MapStruct is a domain-module convention for entity↔DTO mapping, not used inside `ai`).
- Deliberate design decision: `toChatTurn` throws `IllegalStateException` if it encounters a `SystemMessage` or `ToolMessage`. In practice `ConversationMemory` should never hand back those types — `AgentServiceImpl.chat` only ever persists `List.of(userTurn, assistant)` on the no-tool-calls branch, so persisted history is always alternating `UserMessage`/`AssistantMessage`. Throwing turns a broken invariant into a loud, visible failure instead of silently dropping the unexpected turn — same "fail loudly" preference already applied to `ConversationMemory.append` above.
- Done, project-wide fix driven by this feature: `GlobalExceptionHandler`'s catch-all `handleException` (which is what handles the `IllegalStateException` above, since it has no dedicated handler) now logs the exception before returning its generic 500. It previously logged nothing, so any unmapped exception anywhere in the app — not just this one — would reach the client as a bare 500 with zero trace in the logs, defeating the point of failing loudly. Same "log detail internally, respond generically to the client" pattern `AgentServiceImpl.runTool` already used for unexpected tool failures.
- Done: test coverage in `AgentServiceImplTest` — the happy path (`UserMessage`/`AssistantMessage` → `ChatTurn` list) and the invariant-violation path (`ToolMessage` in history → `IllegalStateException`). Plain JUnit 5 unit tests using the existing `SpyConversationMemory` double, no Spring context, matching the rest of the AI layer's tests. No `AgentControllerTest` exists for either endpoint (`chat` was never tested at the controller layer either) — history is only tested at the service layer.
- Known gap: `getHistory` returns the entire persisted history every time, no pagination or trimming — it inherits the "single serialized blob, not row-per-message" shape already noted in Conversation memory above. Revisit only if history grows large enough for that to matter.

