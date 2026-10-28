# 0017 — Database backups (`align-backup.sh`)

- **Status:** Accepted
- **Date:** 2026-09-07
- **Origin:** migrated verbatim from `CLAUDE.md` on 2026-10-27. Body kept as written; later corrections appear as dated notes, not edits.


Problem framing agreed: before this, the only backup was a `pg_dump` inlined in `deploy.yml`, run once per deploy into `/opt/align/backups/` — on the same disk as the database it protects. That covers exactly one failure mode (a deploy or data migration that corrupts data) and leaves three uncovered: **corruption discovered days after the last deploy** (backup cadence was coupled to deploy cadence, which has no relation to when data is at risk), **loss of the host** (disk, provider incident, `rm -rf` — backups die with it), and **loss of `/etc/align/align.env`** (correctly not in git, but also in no backup: `ALIGN_CRYPTO_SECRET` gone means every `llm_credentials` row is undecryptable, and a lost VAPID private key kills every existing push subscription).

A real bug found in that inline line, same "the build won't catch it" class as the `@Transactional`-override, FK-cascade, Flyway-uppercase-`V` and text-block-newline gotchas documented elsewhere in this file: **`pg_dump ... | gzip > file` returns `gzip`'s exit status, not `pg_dump`'s.** A `pg_dump` that failed (auth, disk full, DB unreachable) still left `gzip` writing a truncated file and exiting `0`, so the script's `set -e` saw nothing, the deploy went green, and the backup was silently corrupt. It never detonated — the deploy always ran as the `align` user, so Postgres peer auth worked — but a backup you plan around and that turns out empty is strictly worse than no backup, because it removes the urgency to have a real one.

Decisions made:

- **One implementation, two triggers**, not an inline fix in `deploy.yml` plus a separate script for the timer. `/usr/local/bin/align-backup.sh <label> [keep]` is the only place the backup logic exists; `deploy.yml` calls it with `pre 5` and `align-backup.timer` with `daily 7`. Same "no duplicated logic" criterion the code reviews apply. Collateral benefit: inside a script with a `#!/usr/bin/env bash` shebang, `set -o pipefail` (the actual fix for the bug above) is guaranteed to work — in `appleboy/ssh-action`'s inline script the remote shell isn't guaranteed to be bash, and under `dash` that line aborts outright.
- **This is *not* a `@Scheduled` job in `scheduler`**, despite the project having a scheduler package and a documented habit of flagging new job candidates. Three reasons, all disqualifying on their own: an app that can't start can't back itself up; backing up the database from inside the process that may itself be what's failing couples the safety net to the thing it protects; and it needs the `pg_dump` binary plus a shell. There is no domain rule here — it's pure ops, and the correct layer is the OS.
- **systemd timer, not cron.** The box is already systemd-centric, `systemctl list-timers` shows when the next run is, and the script's stdout/exit code land in `journalctl -u align-backup` instead of depending on local mail. `Persistent=true` gives catch-up after downtime — the exact guarantee the app's own `@Scheduled` jobs lack and that Phase 5 documents as an accepted gap. Tolerable there (housekeeping with a 24h TTL), not tolerable for a backup, and free at this layer.
- **Three validation layers plus an atomic rename.** Not empty / `gzip -t` (stream complete) / `pg_dump`'s closing marker present (the dump ran to the end). The file is written to `<name>.partial` and only `mv`'d to its final name if all three pass, so a `*.sql.gz` in `backups/` is never a half-written file. A `trap ... EXIT` clears the partial on any failure path.
- **Retention split by label** (`pre` 5, `daily` 7), rotated independently with the same `ls -1t | tail -n +N | xargs -r rm -f` idiom `deploy.yml` already uses for `releases/`. A day with four deploys can't evict the daily history.
- **A failed backup now aborts the deploy**, before `ln -sfn` repoints `current.jar` — the script returns an honest exit code, so the existing `set -e` finally has something to act on. Deliberate: if the DB can't be backed up, the migration shouldn't run.

Two mistakes made and fixed while building this, both worth remembering as shell gotchas rather than bugs in the backup itself: (1) the completion-marker check used `tail -1`, but `pg_dump` closes with a **three-line** comment block, so the marker is never the last line — the check reported "dump incompleto" on a perfectly good dump. (2) The replacement used `grep -q` inside a pipeline; `grep -q` exits on first match and can `SIGPIPE` its upstream, which under `pipefail` would be *another* false negative. Both resolved by capturing `tail -5` into a variable and matching with `case`, keeping the check out of any pipeline.

Known gaps, deliberate for this cut:

- **No off-site copy — this is the largest remaining gap in the whole deployment.** Backups still share a disk with the database, so nothing here helps if the host is lost. Next step (planned, not built): `rclone` to Backblaze B2 / Cloudflare R2, with the encryption passphrase in a password manager rather than on the VPS, which would just recreate the problem.
- **`/etc/align/align.env` still isn't backed up anywhere.** Cheapest real mitigation is copying its values into a password manager — zero infrastructure, and it's the only genuinely irreconstructible thing on the box.
- **The restore path has never been exercised.** A backup that was never restored is a hypothesis. One manual `createdb align_restore_test` → `gunzip -c | psql` → row-count check would settle it; the dump includes `flyway_schema_history`, so a restored DB starts coherent without Flyway re-migrating.
- **Daily cadence means up to 24h of exposure.** Accepted: at single-user volume the loss is a day of tasks/transactions the user themselves created. Frequency and retention depth are separate dimensions — `keep` is a *count*, so raising cadence without raising `keep` silently shortens how far back you can go, which is the dimension that matters for corruption noticed late. Off-site coverage is worth more than either, so it goes first.
- NPM's config (Proxy Hosts, certs) is still unversioned and unbacked-up — reconstructible by hand in ~15 min since Let's Encrypt re-issues for free, so deliberately ranked below the two points above.



## Later notes

- **2026-10-27 — correction.** The reference copies (`deploy/align-backup.sh`, `.service`, `.timer`) were never committed. Until they are copied back from the VPS into `deploy/`, losing the VPS also loses the backup script, which is the opposite of what this ADR assumed.
