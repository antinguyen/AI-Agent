# Release Announcement - v2.5.1 (2026-04-02)

## Scope
- Includes feature release v2.5.0 and hotfix v2.5.1.

## What is new (v2.5.0)
- Product description field in product master data.
- Product image upload API and UI flow.
- Currency normalization with automatic bank exchange-rate mapping.
- Supplier/brand/origin options endpoints and combo-box UX.

## Hotfix (v2.5.1)
- Fixed production upload permission issue in app container.
- Ensured runtime write access for `/app/uploads/products`.

## Deployment status
- Backend: healthy.
- Frontend: healthy.
- Database migration: Flyway V15 applied successfully.

## Verification summary
- Product options API: pass.
- Product create with USD auto exchange rate: pass.
- Product image upload and static URL access: pass (HTTP 200).
- Smoke data cleanup: completed (`SMK-*` products and `smoke*` users removed).

## Notes
- Release Notes UI has been updated to include v2.5.0 and v2.5.1.
- Changelog has been updated accordingly.
