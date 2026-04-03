# Implementation Framework

Muc tieu: tranh trien khai manh mun, co khung day du de nang cap UI/UX va nghiep vu theo tung phase co kiem soat.

## 1. Product Vision
- Giao dien nhat quan cho cac module: Dashboard, Product, Customer, Order, Payment, Report.
- Luong CRUD day du: create, read, update, delete, validate, loading, empty state, error state.
- Feedback ro rang: toast + inline validation + ma loi nghiep vu.
- Bao mat va phan quyen ro: STAFF/ADMIN theo endpoint.

## 2. Technical Baseline (bat buoc truoc khi them tinh nang)
- API contracts: dung chung TypeScript interfaces theo backend DTO.
- Error contract: thong nhat format ApiError { code, message, details, path, timestamp }.
- Form standards:
  - Numeric fields dung valueAsNumber.
  - Required fields map 1-1 voi backend validations.
  - Moi form co loading state + disabled submit.
- Auth standards:
  - Luu token + user trong localStorage.
  - Redirect khi 401.
  - Kiem tra role theo `ADMIN`/`STAFF`.

## 3. UI Framework (MVP)
- Layout:
  - Hero + Filter bar + Data table/card + Pagination + Modal form.
- States:
  - Skeleton loading
  - Empty result
  - Validation error
  - API error
- Reusable components:
  - ModalShell
  - PaginationBar
  - ToastProvider
  - ConfirmDialogProvider

## 4. Domain Rollout Order
1. Auth + User profile (login/register/logout + role-aware navigation)
2. Product module (CRUD + low stock + search + active toggle)
3. Customer module (CRUD + validation)
4. Order module (create/confirm/cancel + stock impact)
5. Payment + Invoice
6. Reports + export

## 5. Definition of Done (moi module)
- CRUD chay du tren frontend va backend.
- Toi thieu 1 flow test tay tu UI den API thanh cong.
- Toast + error message hien dung nguyen nhan.
- Role guard dung theo ADMIN/STAFF.
- Tai lieu README cap nhat endpoint + luong su dung.

## 6. Current Gaps Found (April 2026)
- Product create failed do frontend khong gui `active` (backend bat buoc) -> da fix.
- JSON malformed requests de bi nham voi INTERNAL_ERROR -> da bo sung MALFORMED_JSON.
- Thieu quy trinh rollout theo phase -> bo sung file framework nay.

## 7. Next Steps
1. Them e2e smoke test cho login + create product.
2. Bo sung role-based UI gating (an/hien nut admin-only).
3. Chuan hoa metric va analytics (so luong loi form, API fail theo module).
4. Chot checklist release (security, env vars, backup, rollback).
