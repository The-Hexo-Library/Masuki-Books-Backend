-- Rename products table to books_metadata for strict schema naming.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'products'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'books_metadata'
    ) THEN
        ALTER TABLE public.products RENAME TO books_metadata;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public' AND indexname = 'idx_products_category'
    ) THEN
        ALTER INDEX public.idx_products_category RENAME TO idx_books_metadata_category;
    END IF;
END $$;

-- Users: ensure role exists for Admin/User RBAC.
ALTER TABLE IF EXISTS public.users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Category: ensure collections field exists.
ALTER TABLE IF EXISTS public.categories
    ADD COLUMN IF NOT EXISTS collections TEXT;

-- Subscription plans and user subscriptions.
CREATE TABLE IF NOT EXISTS public.subscription (
    subscription_id UUID PRIMARY KEY,
    plan_name VARCHAR(120) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    duration_days INTEGER NOT NULL,
    is_plan BOOLEAN NOT NULL DEFAULT TRUE,
    user_id UUID,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    expires_at TIMESTAMP,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES public.users(user_id)
);

-- Public library metadata.
CREATE TABLE IF NOT EXISTS public.public_library (
    public_library_id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    notes TEXT,
    editable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_public_library_product UNIQUE (product_id),
    CONSTRAINT fk_public_library_product FOREIGN KEY (product_id)
        REFERENCES public.books_metadata(product_id)
);
