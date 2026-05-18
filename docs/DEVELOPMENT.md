# Development Workflow

This repository uses `main` as the development branch.

## Commit checkpoints

Make small commits whenever one of these is true:

- A feature slice works end to end.
- A bug is fixed and verified.
- A refactor is complete without behavior changes.
- Documentation or setup changes are complete.

Use the checkpoint helper:

```bash
./scripts/checkpoint "type: short summary"
```

Examples:

```bash
./scripts/checkpoint "docs: add development workflow"
./scripts/checkpoint "feat: add first flow model"
./scripts/checkpoint "fix: handle empty flow title"
```

The script shows the current diff, stages tracked and untracked files, creates a commit, and leaves pushing as an explicit separate step.

## Before pushing

Run the project checks once they exist, then push:

```bash
git status --short
git push
```

Keep each commit focused enough that it can be reviewed or reverted independently.

