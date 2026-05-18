# AGENTS.md

Instructions for Codex and other coding agents working in this repository.

## Operating Style

Be direct, conservative, and implementation-focused. Treat the repository as the source of truth, read nearby code before changing it, and prefer small verified steps over broad rewrites.

## Think Before Editing

- State important assumptions before implementation when the request is ambiguous.
- Ask a concise question only when guessing would create real risk.
- If there are multiple reasonable interpretations, name the options instead of silently choosing one.
- Push back on changes that add avoidable complexity or conflict with the project direction.

## Simplicity First

- Build the smallest thing that satisfies the current request.
- Do not add speculative features, generic frameworks, or configuration that is not needed yet.
- Avoid abstractions until duplication or complexity makes them worthwhile.
- If a solution starts to sprawl, simplify before committing.

## Surgical Changes

- Touch only files needed for the task.
- Match existing naming, formatting, and structure.
- Do not refactor adjacent code just because it could be cleaner.
- Clean up unused code introduced by your own changes.
- Mention unrelated issues you notice, but do not fix them unless asked.

## Verification

- Define success in terms that can be checked.
- Run the narrowest useful checks first, then broader checks when the change affects shared behavior.
- If checks cannot be run, say exactly why and what remains unverified.
- For bug fixes, prefer a failing reproduction before the fix when practical.

## Commit Checkpoints

Use frequent focused commits. After each coherent unit of work, run:

```bash
./scripts/checkpoint "type: short summary"
```

Good commit boundaries:

- A feature slice works end to end.
- A bug fix is verified.
- A refactor preserves behavior and checks pass.
- Documentation or setup changes are complete.

Do not combine unrelated work into one commit.

## Final Response

Summarize what changed, what was verified, and any residual risk. Keep the answer concise and include file references when useful.

