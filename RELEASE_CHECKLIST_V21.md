# Release Checklist V21 (Product Category + VAT)

## Scope
- Migration: V21 adds products.category and products.vat_rate.
- Behavior: order totals include VAT from product vatRate.
- UI/API: product create/update/list supports category and vatRate.

## Pre-deploy
- Verify latest backup/snapshot is completed for production database.
- Confirm application artifact includes migration file V21.
- Confirm app config points to production database and correct profile.
- Confirm a rollback owner is assigned and reachable.

## Deploy Steps
- Put system in maintenance window (if required by policy).
- Deploy backend artifact and allow Flyway to run migration V21.
- Deploy frontend artifact that includes category and VAT fields/filters.
- Check startup logs for: "Successfully validated 21 migrations" and migration V21 applied.

## Verify Points (Post-deploy)
- Product management:
  - Create product with category and vatRate.
  - Edit product category and vatRate.
  - Filter products by category.
- Order flow:
  - Create one order with at least 2 line items.
  - Confirm order succeeds and totalAmount is positive.
  - Confirm shipment/payment flow still works.
- Smoke script:
  - Preferred one-command gate: run scripts/release_smoke_v21_suite.ps1 and confirm V21_SUITE_RESULT=PASS.
  - Optional separate checks:
    - Run scripts/release_smoke_multiline_order.ps1 and confirm MULTI_ORDER_SMOKE_RESULT=PASS.
    - Run scripts/release_smoke_multiline_vat_assert.ps1 and confirm VAT_ASSERT_SMOKE_RESULT=PASS.
  - Ensure expected vs actual total amount delta is <= 0.01.

## Rollback Notes
- Application rollback:
  - Roll back backend/frontend to previous stable release if functional issues appear.
- Database rollback for V21 (manual SQL):
  - Use only if business approves data loss for V21 fields.
  - SQL:

```sql
DROP INDEX IF EXISTS idx_products_category;
ALTER TABLE products DROP COLUMN IF EXISTS category;
ALTER TABLE products DROP COLUMN IF EXISTS vat_rate;
DELETE FROM flyway_schema_history WHERE version = '21';
```

## Operational Cautions
- If any rows rely on category/vat_rate after go-live, database rollback may lose data.
- Prefer forward-fix if production already has new writes using V21 fields.
