# Masuki Books Backend API Test Plan

Base URL:
- http://localhost:8081

Auth header:
- Authorization: Bearer <JWT_TOKEN>

## Step 1: Public Auth Endpoints
- POST /auth/register -> api-tests/payloads/auth-register.json
- POST /auth/login -> api-tests/payloads/auth-login.json
- POST /auth/admin/login -> api-tests/payloads/auth-admin-login.json

## Step 2: User Endpoints (use user JWT)
- GET /user/categories
- GET /user/cart
- POST /user/cart/items -> api-tests/payloads/user-cart-add-item.json
- PUT /user/cart/items/{cartItemId} -> api-tests/payloads/user-cart-update-item.json
- DELETE /user/cart/items/{cartItemId}
- GET /user/orders
- POST /user/checkout -> api-tests/payloads/user-checkout.json
- GET /user/subscriptions/plans
- POST /user/subscriptions/activate -> api-tests/payloads/user-subscription-activate.json
- GET /user/subscriptions/me
- GET /user/public-library
- GET /user/library
- POST /user/library/{bookId} -> api-tests/payloads/user-library-add.json
- DELETE /user/library/{bookId}

## Step 3: Admin Endpoints (use admin JWT)
- GET /admin/users
- GET /admin/orders
- PATCH /admin/orders/{orderId}/status -> api-tests/payloads/admin-order-status-update.json
- GET /admin/books
- POST /admin/books -> api-tests/payloads/admin-book-create.json
- PUT /admin/books/{bookId} -> api-tests/payloads/admin-book-update.json
- DELETE /admin/books/{bookId}
- POST /admin/categories -> api-tests/payloads/admin-category-create.json
- PUT /admin/categories/{categoryId} -> api-tests/payloads/admin-category-update.json
- DELETE /admin/categories/{categoryId}
- GET /admin/public-library
- POST /admin/public-library -> api-tests/payloads/admin-public-library-upsert.json
- DELETE /admin/public-library/{publicLibraryId}
- POST /admin/subscriptions/plans -> api-tests/payloads/admin-subscription-plan-create.json

## Notes
- Replace placeholder UUIDs before calling endpoints.
- Create category first, then use categoryId in book creation.
- Create subscription plan first, then use plan ID in user activation payload.
- For cart update/delete, first add item and extract cartItemId from response.
- For library add/delete, use a valid bookId that exists in books_metadata.
