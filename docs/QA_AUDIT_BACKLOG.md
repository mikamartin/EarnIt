# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`. Once every item from a pass is resolved this way, the
file is emptied back to this shell, ready for the next audit to repopulate.

**No open findings.** All seven follow-up branches from the last audit have landed:
`fix/moshi-crossref-keep-rule`, `fix/import-schema-validation`,
`test/fk-cascade-and-cleanup-assertions`, `refactor/reward-progress-derived-state`,
`test/settings-persistence-and-assertions`, `chore/ci-release-build-gate`, and
`chore/qa-audit-doc-fixes`. Run a new pass per `QA_AUDIT_RULES.md` to repopulate this file.
