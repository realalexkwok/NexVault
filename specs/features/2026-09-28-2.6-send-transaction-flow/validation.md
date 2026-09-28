# 2.6 — Send transaction flow (validation record)

> Status: **in progress on `feature/2.6-send-transaction-flow`** (owner decisions: `requirements.md`
> Q1–Q5, 2026-09-28). Evidence fills in per task group; nothing below is claimed done until its
> command and observed result are recorded.

## Task 0 — Milestone static-analysis baseline (DONE 2026-09-28)

Setup, all on this branch: `org.sonarqube` **7.4.0.8496** in the catalog (`sonar` alias), applied at
the root `build.gradle.kts` (root-level by design — the whole build is one Sonar project), tech-stack
§6 row added, `sonar` task verified present. Host URL and token pass on the command line; the token
lives in `local.properties` (git-ignored), never printed.

Token journey, recorded because the discipline applies to tooling too: the first attempt hit the
server's changed admin credentials (401); a resume-era token authenticated but its account had zero
project visibility and no create rights ("not authorized to analyze ... not authorized to create");
the owner created the `NexVault` project and, after one revoked over-powered global token, minted a
**Project Analysis Token** scoped to `NexVault` — the least-privilege choice and the standing recipe
(project-scoped, not global; AGENTS/dev-environment updated accordingly below).

Baseline scan (the pre-2.6 tree — only the task-0 build changes and this spec were present):

```
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<local.properties> -Dsonar.projectKey=NexVault
-> BUILD SUCCESSFUL, exit 0 (2026-09-28)
```

Baseline measures, read back from `/api/measures/component?component=NexVault`:

| Metric | Baseline |
| --- | --- |
| ncloc | **12,121** |
| bugs | 3 |
| vulnerabilities | 5 |
| code smells | 70 |
| security hotspots | 0 |
| duplicated lines density | 0.3 |
| coverage | 0.0 (JaCoCo not wired — coverage joins the gate at 4.8) |

The gate at **2.10** is new-code-only: no new blocker/critical, no new security hotspots, duplication
not increasing, measured against these numbers.

## Task 1 — data layer (pending)

## Task 2 — domain use cases (pending)

## Task 3 — feature-send UI (pending)

## Task 4 — navigation re-enable (pending)

## Task 5 — verification (pending)
