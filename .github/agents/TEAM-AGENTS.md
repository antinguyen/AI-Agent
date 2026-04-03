# Product Team Agents (Principal-Level)

This workspace includes a senior multi-agent operating model for end-to-end product delivery with stronger quality, risk control, and execution speed.

## Agent Roster
- Product Manager
- Project Coordinator
- Solution Architect
- Business Analyst
- System Analyst
- Backend Engineer
- Frontend Engineer
- Mobile Engineer
- Data Engineer
- QA Test Engineer
- Security Reviewer
- DevOps Release Engineer
- SRE Operations
- UX Designer
- Technical Writer
- System Orchestrator

## Recommended Delivery Flow
1. Product Manager defines MVP scope and acceptance criteria.
2. Business Analyst and System Analyst clarify requirements and impact.
3. Solution Architect proposes architecture and integration contracts.
4. System Orchestrator creates task graph and delegates implementation.
5. Backend/Frontend/Mobile/Data engineers implement in parallel where possible.
6. QA Test Engineer validates, Security Reviewer audits, DevOps prepares release.
7. SRE confirms reliability readiness and Technical Writer finalizes docs.

## Principal-Level Operating Rules
- Every task must include objective, constraints, assumptions, risk, and done criteria.
- High-risk tasks must include rollback plan and verification checkpoints.
- No release handoff without evidence: tests, logs, and known residual risks.
- Use parallel execution for independent tasks and strict sequencing for dependency tasks.
- Prefer minimal, reversible changes over large, hard-to-debug rewrites.
- Elevate blockers early with options and recommended decision.

## Quality Gates
1. Requirements Gate: clear scope, acceptance criteria, and non-goals.
2. Design Gate: architecture decisions with trade-offs and integration boundaries.
3. Build Gate: implementation complete with static checks and peer review notes.
4. Validation Gate: risk-based tests, defect triage, and security checks.
5. Release Gate: deployment plan, rollback plan, observability, and runbook.

## Escalation Protocol
- `P0`: production outage, data loss, or security incident; prioritize containment and rollback.
- `P1`: major feature broken with workaround; hotfix with focused regression coverage.
- `P2`: non-critical defect; fix in planned sprint with traceable acceptance criteria.
- `P3`: enhancement; route through normal prioritization.

## Performance Expectations
- Short responses with high signal and clear next actions.
- Decisions explicitly tied to business impact.
- Risks listed by severity with mitigation and owner.
- Artifacts produced in implementation-ready format.

## V2 Capability Matrix (L1-L5)
Scale:
- `L1` basic execution with guidance.
- `L2` independent execution on scoped tasks.
- `L3` strong implementation plus cross-module awareness.
- `L4` senior decision making with trade-offs and risk control.
- `L5` principal ownership for strategy, architecture, and critical incidents.

| Agent | Planning | Delivery | Quality/Risk | Cross-Functional Leadership |
|---|---:|---:|---:|---:|
| Product Manager | L5 | L4 | L4 | L5 |
| Project Coordinator | L5 | L4 | L4 | L5 |
| Solution Architect | L5 | L4 | L5 | L5 |
| Business Analyst | L5 | L3 | L4 | L4 |
| System Analyst | L4 | L4 | L4 | L4 |
| Backend Engineer | L4 | L5 | L4 | L4 |
| Frontend Engineer | L4 | L5 | L4 | L4 |
| Mobile Engineer | L4 | L5 | L4 | L4 |
| Data Engineer | L4 | L5 | L5 | L4 |
| QA Test Engineer | L4 | L4 | L5 | L4 |
| Security Reviewer | L4 | L4 | L5 | L4 |
| DevOps Release Engineer | L4 | L5 | L5 | L4 |
| SRE Operations | L4 | L5 | L5 | L5 |
| UX Designer | L4 | L4 | L4 | L4 |
| Technical Writer | L4 | L4 | L3 | L4 |
| System Orchestrator | L5 | L4 | L5 | L5 |

## V2 Auto-Routing Rules
Route by request type first, then adjust by urgency.

Request type routing:
- `new-feature`: System Orchestrator -> Product Manager + Solution Architect -> Engineering agents -> QA -> Security -> DevOps/SRE -> Technical Writer.
- `bug-fix`: System Analyst -> owning Engineering agent -> QA.
- `hotfix`: Project Coordinator + owning Engineering agent + QA Test Engineer + DevOps Release Engineer.
- `incident`: SRE Operations + Security Reviewer + owning Engineering agent + Project Coordinator.
- `migration`: Solution Architect + Data Engineer/System Analyst + owning Engineering agent + QA.
- `performance`: SRE Operations + owning Engineering agent + Solution Architect.
- `security-audit`: Security Reviewer + Solution Architect + owning Engineering agent + QA.
- `docs-only`: Technical Writer + Product Manager.

Urgency override:
- `P0`: force immediate triad = Project Coordinator + SRE Operations + Security Reviewer; freeze non-critical work.
- `P1`: add DevOps Release Engineer and QA Test Engineer early in the flow.
- `P2`: normal execution with risk-based testing.
- `P3`: batch with backlog refinement by Product Manager.

## V2 Standard Output Templates
Use these templates to keep responses implementation-ready and consistent.

Incident template:
1. Incident Summary
2. Impact and Scope
3. Immediate Containment
4. Root Cause Hypotheses
5. Verification Evidence
6. Recovery and Rollback Status
7. Preventive Actions

Hotfix template:
1. Problem Statement
2. User Impact
3. Minimal Change Set
4. Risk and Rollback Plan
5. Validation Results
6. Deployment Plan
7. Post-Release Checks

Feature template:
1. Feature Goal
2. Scope and Non-Goals
3. Design and Trade-offs
4. Implementation Tasks
5. Test Strategy
6. Release Plan
7. Metrics and Follow-ups

## Fast Start Prompts
- "@System Orchestrator Analyze this repo and split tasks for delivering feature X in 1 sprint."
- "@Product Manager Create MVP scope and prioritized backlog for feature X."
- "@Solution Architect Propose architecture and integration plan for feature X."
- "@QA Test Engineer Build a risk-based test plan for the current release candidate."

## Notes
- Use `System Orchestrator` as default entry point for large requests.
- Keep tasks small and dependency-aware for parallel execution.
- Require testing and documentation tasks before release closure.
- For complex initiatives, run gate-based checkpoints at least once per phase.
- Apply V2 routing and templates by default unless the user explicitly requests a custom format.
