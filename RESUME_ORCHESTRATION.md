# Resume Orchestration Prompt

Copy and paste this entire prompt into a new Claude Code session to resume Epic 2 Phase B & C orchestration:

---

I need you to resume the BMAD orchestration for Epic 2 Phase B & C. Read the handoff document first:

```
Read /Users/reena/gauntletai/picstormai/docs/handoff/epic2_phase_bc_handoff.md
```

Then continue orchestration using /Users/reena/gauntletai/picstormai/orchestrator.md following these steps:

1. **First**: Complete QA review for Story 2.8 (currently "Ready for Review")
   - Invoke @qa-quality agent
   - Verify all acceptance criteria met
   - Mark story as "Done" or "In Progress"
   - Update orchestration-flow.md

2. **Then**: Continue with remaining stories (2.9-2.14)
   - For each story: @sm-scrum → @dev → @qa-quality cycle
   - **CRITICAL**: All stories must use SSE (Server-Sent Events), NOT WebSocket
   - Update story status after each agent invocation
   - Log to orchestration-flow.md

3. **Finally**: Generate Phase B & C completion report when all stories are "Done"

**Current Status**:
- ✅ Story 2.6: Done
- ✅ Story 2.7: Done
- 🔄 Story 2.8: Ready for QA Review (start here)
- ⏳ Story 2.9: Draft (TODO)
- ⏳ Stories 2.10-2.14: Draft (TODO)

**Key Files**:
- Orchestrator instructions: `/Users/reena/gauntletai/picstormai/orchestrator.md`
- Orchestration log: `/Users/reena/gauntletai/picstormai/docs/orchestration-flow.md`
- Handoff document: `/Users/reena/gauntletai/picstormai/docs/handoff/epic2_phase_bc_handoff.md`
- Story files: `/Users/reena/gauntletai/picstormai/docs/stories/2-*.md`

**IMPORTANT Reminders**:
1. Use SSE architecture (not WebSocket) for all stories
2. Update story status after EVERY agent invocation
3. Continue automatically until all stories "Done" (don't stop after one story)
4. Use TodoWrite to track progress

Begin orchestration now.
