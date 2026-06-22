-- V3: Add Stripe payment fields and webhook idempotency table

ALTER TABLE IF EXISTS public.payments
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_charge_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_event_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_receipt_url TEXT,
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(100);

ALTER TABLE IF EXISTS public.orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS checkout_fingerprint VARCHAR(128),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS public.processed_stripe_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    processed_at TIMESTAMP NOT NULL DEFAULT now(),
    payload JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_processed_stripe_events_event_id ON public.processed_stripe_events (event_id);
