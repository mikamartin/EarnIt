# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`.

No open findings — the previous audit's issues are all resolved (see git history for details).
The next `chore/qa-audit` pass populates this file per [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md)
step 8: a "What's Working" summary, numbered Issues Found, a Mutation Check Results table, Spec
Cross-Reference Notes, and (in default mode) a "Work, Grouped by Branch" section for any
follow-up branches.
