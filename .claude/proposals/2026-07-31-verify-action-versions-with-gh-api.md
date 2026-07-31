# Proposal — look up GitHub Action versions instead of recalling them

## What

When writing or editing a `.github/workflows/*.yml`, never write a `uses:` major tag from memory.
Model training data lags the marketplace by months, so recalled versions are systematically too
old (this session's first draft had `actions/checkout@v5` and `gradle/actions/setup-gradle@v5`
while both were two majors behind). Resolve them instead:

```bash
for r in actions/checkout actions/setup-java gradle/actions android-actions/setup-android; do
  echo -n "$r: "; gh api repos/$r/releases/latest --jq .tag_name
done
```

Then pin to the major (`@v7`), not the full patch tag the API returns. A second, cheaper source
when `gh` isn't available: an existing workflow in a sibling project the user actively maintains.

## Why shared

Nothing about this is WallosMobile-specific — it applies to any repo with GitHub Actions, and the
failure mode is silent (an old-but-valid major keeps working, so nothing surfaces the drift until
a deprecation warning turns into a hard failure). It came up here in step 0.7, and the same
guess-from-memory reflex would fire in any project that gains CI.

## Target

New guidance, small. Either a short shared skill (`github-actions-workflow`) or a bullet inside
an existing CI/setup skill if one already covers workflow authoring. Edit, not a rewrite.

## Suggested text

> **Don't write `uses:` versions from memory.** Your training data lags the action marketplace;
> recalled majors are usually stale. Run
> `gh api repos/<owner>/<repo>/releases/latest --jq .tag_name` for each action, then pin the
> major (`actions/checkout@v7`). If `gh` isn't available, copy the versions from a workflow in a
> sibling project the user actively maintains rather than guessing.

## Source

WallosMobile, 2026-07-31, checklist step 0.7 (first CI workflow).
