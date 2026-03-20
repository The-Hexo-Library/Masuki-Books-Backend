# Masuki Books Backend API Test Plan

Base URL:
- `http://localhost:8081`

Auth header format:
- `Authorization: Bearer <JWT_TOKEN>`

Guest header format:
- `X-Guest-Token: guest-12345`

## 1) Auth APIs
- POST `/auth/register` -> payload: `api-tests/payloads/auth-register.json`
- POST `/auth/login` -> payload: `api-tests/payloads/auth-login.json`
- POST `/auth/admin/login` -> payload: `api-tests/payloads/auth-login.json`

## 2) Product & Category APIs
- GET `/products`
- GET `/products/{productId}`
- POST `/products` (admin) -> payload: `api-tests/payloads/products-create.json`
- GET `/categories`
- GET `/categories/{categoryId}`
- POST `/categories` (admin) -> payload: `api-tests/payloads/categories-create.json`

## 3) Cart APIs
- GET `/cart` (guest/user)
- POST `/cart/items` -> payload: `api-tests/payloads/cart-add-item.json`
- PUT `/cart/items/{cartItemId}` -> payload: `api-tests/payloads/cart-update-item.json`
- DELETE `/cart/items/{cartItemId}`
- DELETE `/cart`

## 4) Order & Payment APIs
- POST `/orders/checkout` -> payload: `api-tests/payloads/orders-checkout.json`
- GET `/orders` (auth)
- GET `/orders/{orderId}` (auth)
- POST `/orders/{orderId}/cancel` (auth)
- POST `/payments/initiate` (auth) -> payload: `api-tests/payloads/payments-initiate.json`
- POST `/payments/callback` -> payload: `api-tests/payloads/payments-callback.json`
- GET `/payments/order/{orderId}` (auth)

## 5) User APIs
- GET `/users/me` (auth)
- PATCH `/users/me` (auth) -> payload: `api-tests/payloads/users-update-profile.json`
- POST `/users/me/change-password` (auth) -> payload: `api-tests/payloads/users-change-password.json`

## 6) Wallet APIs
- GET `/wallet` (auth)
- POST `/wallet/topup` (auth) -> payload: `api-tests/payloads/wallet-topup.json`
- GET `/wallet/transactions` (auth)

## 7) Review APIs
- GET `/reviews/product/{productId}`
- POST `/reviews` (auth user) -> payload: `api-tests/payloads/reviews-submit.json`

## 8) Reader APIs
- GET `/reader/{bookId}/metadata` (auth)
- GET `/reader/{bookId}/page/{pageNumber}` (auth)
- GET `/reader/{bookId}/pages?start=1&end=5` (auth)
- POST `/reader/{bookId}/progress` (auth) -> payload: `api-tests/payloads/reader-save-progress.json`
- GET `/reader/{bookId}/progress` (auth)
- GET `/reader/{bookId}/bookmarks` (auth)
- POST `/reader/{bookId}/bookmarks` (auth) -> payload: `api-tests/payloads/reader-create-bookmark.json`
- PUT `/reader/{bookId}/bookmarks/{bookmarkId}` (auth) -> payload: `api-tests/payloads/reader-create-bookmark.json`
- DELETE `/reader/{bookId}/bookmarks/{bookmarkId}` (auth)

## 9) Support APIs
- POST `/support-tickets` (auth) -> payload: `api-tests/payloads/support-ticket-create.json`
- GET `/support-tickets` (auth)
- GET `/support-tickets/{ticketId}` (auth)

## 10) Resale APIs
- GET `/resale/marketplace`
- GET `/resale/my-listings` (auth)
- POST `/resale/list` (auth) -> payload: `api-tests/payloads/resale-list.json`
- POST `/resale/{resaleId}/buy` (auth)
- POST `/resale/{resaleId}/cancel` (auth)

## 11) Admin APIs (sample)
- GET `/admin/dashboard/stats` (admin)
- GET `/admin/users` (admin)
- PATCH `/admin/users/{userId}/status` (admin) -> body: `{ "isActive": true }`
- GET `/admin/orders` (admin)
- PATCH `/admin/orders/{orderId}/status` (admin) -> body: `{ "status": "PAID" }`
- POST `/admin/discounts` (admin) -> payload: `api-tests/payloads/admin-discount-create.json`

## Notes
- Replace all placeholder UUID values in payload JSON files before testing.
- Use a JWT from `/auth/login` for protected APIs.
- If an endpoint returns 401/403, verify role/authorization first.
