-- V1: Baseline schema (all existing tables)
-- This migration is skipped on existing DBs via baseline-on-migrate=true

CREATE TABLE IF NOT EXISTS assets (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    height integer,
    is_active boolean NOT NULL,
    mime_type character varying(100) NOT NULL,
    original_name character varying(255) NOT NULL,
    size bigint NOT NULL,
    storage_path character varying(500) NOT NULL,
    tenant_id uuid NOT NULL,
    thumbnail_path character varying(500),
    thumbnail_url character varying(500),
    url character varying(500) NOT NULL,
    width integer
);

CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL,
    duration_ms bigint NOT NULL,
    email character varying(150),
    ip character varying(45),
    method character varying(10) NOT NULL,
    path character varying(255) NOT NULL,
    status integer NOT NULL,
    "timestamp" timestamp(6) with time zone NOT NULL,
    user_id uuid
);

CREATE TABLE IF NOT EXISTS backup_export_logs (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    error_message character varying(500),
    file_path character varying(500),
    file_size bigint,
    sections_count integer,
    started_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    task_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    tenant_name character varying(100),
    CONSTRAINT backup_export_logs_status_check CHECK (((status)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS backup_records (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    error_message character varying(500),
    file_name character varying(255),
    file_size bigint,
    started_at timestamp(6) with time zone,
    status character varying(255),
    type character varying(255),
    updated_at timestamp(6) with time zone,
    CONSTRAINT backup_records_status_check CHECK (((status)::text = ANY ((ARRAY['IN_PROGRESS'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT backup_records_type_check CHECK (((type)::text = ANY ((ARRAY['DATABASE'::character varying, 'ASSETS'::character varying, 'CONFIG'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS backup_tasks (
    id uuid NOT NULL,
    archive_path character varying(500),
    archive_size bigint,
    completed_at timestamp(6) with time zone,
    completed_tenants integer,
    created_at timestamp(6) with time zone,
    error_message character varying(1000),
    failed_tenants integer,
    file_path character varying(500),
    file_size bigint,
    requested_by uuid,
    retention_until timestamp(6) with time zone,
    scope character varying(20) NOT NULL,
    started_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    tenant_id uuid,
    total_tenants integer,
    type character varying(30) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT backup_tasks_scope_check CHECK (((scope)::text = ANY ((ARRAY['ALL_TENANTS'::character varying, 'SINGLE_TENANT'::character varying])::text[]))),
    CONSTRAINT backup_tasks_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'PARTIAL'::character varying])::text[]))),
    CONSTRAINT backup_tasks_type_check CHECK (((type)::text = ANY ((ARRAY['SCHEDULED'::character varying, 'MANUAL_FULL'::character varying, 'MANUAL_TENANT'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS budget_lines (
    id uuid NOT NULL,
    budget_id uuid NOT NULL,
    phase_id uuid,
    quantity integer,
    service_id uuid,
    service_name character varying(100),
    sort_order integer,
    total numeric(10,2) NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    monthly_price numeric(10,2),
    phase_number integer
);

CREATE TABLE IF NOT EXISTS budgets (
    id uuid NOT NULL,
    acceptance_token character varying(64),
    accepted_at timestamp(6) with time zone,
    budget_number character varying(20),
    client_notes text,
    created_at timestamp(6) with time zone,
    discount_total numeric(10,2) NOT NULL,
    notes text,
    profile_id uuid,
    rejected_at timestamp(6) with time zone,
    rejected_reason character varying(255),
    sent_at timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    tenant_id uuid NOT NULL,
    total numeric(10,2) NOT NULL,
    updated_at timestamp(6) with time zone,
    valid_until date,
    version integer,
    recommendation text,
    recommended_phase_ids text,
    business_size character varying(50),
    sector character varying(50),
    CONSTRAINT budgets_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SENT'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS catalog_services (
    id uuid NOT NULL,
    cost numeric(10,2) NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    is_addon boolean NOT NULL,
    monthly_price numeric(10,2) NOT NULL,
    name character varying(100) NOT NULL,
    phase_id uuid,
    profile_id uuid,
    sale_price numeric(10,2) NOT NULL,
    slug character varying(60) NOT NULL,
    sort_order integer,
    type character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT catalog_services_type_check CHECK (((type)::text = ANY ((ARRAY['CREDENTIALS'::character varying, 'LANDING'::character varying, 'AUTOMATION'::character varying, 'BILLING'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS communication_requests (
    id uuid NOT NULL,
    channel character varying(20) NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone,
    field_id uuid,
    recipient character varying(200) NOT NULL,
    request_type character varying(30) NOT NULL,
    responded_at timestamp(6) with time zone,
    response_data text,
    sent_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    subject character varying(200),
    tenant_id uuid NOT NULL,
    tenant_service_id uuid NOT NULL,
    body text NOT NULL,
    CONSTRAINT communication_requests_channel_check CHECK (((channel)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'TELEGRAM'::character varying, 'EMAIL'::character varying])::text[]))),
    CONSTRAINT communication_requests_request_type_check CHECK (((request_type)::text = ANY ((ARRAY['REQUEST_CREDENTIAL'::character varying, 'REQUEST_PERMISSION'::character varying, 'REQUEST_INFO'::character varying, 'REQUEST_CONFIRMATION'::character varying])::text[]))),
    CONSTRAINT communication_requests_status_check CHECK (((status)::text = ANY ((ARRAY['SENT'::character varying, 'DELIVERED'::character varying, 'RESPONDED'::character varying, 'EXPIRED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS contact_identifiers (
    id uuid NOT NULL,
    channel character varying(255) NOT NULL,
    contact_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    identifier character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    CONSTRAINT contact_identifiers_channel_check CHECK (((channel)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'WHATSAPP_META'::character varying, 'TELEGRAM'::character varying, 'EMAIL'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS contact_leads (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    email character varying(200),
    landing_id uuid NOT NULL,
    message text,
    metadata text,
    name character varying(150),
    phone character varying(30)
);

CREATE TABLE IF NOT EXISTS contacts (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255),
    tenant_id uuid NOT NULL,
    conversation_summary text,
    total_message_count integer DEFAULT 0 NOT NULL,
    summary_updated_at timestamp with time zone,
    email character varying(255),
    phone character varying(30)
);

CREATE TABLE IF NOT EXISTS conversations (
    id bigint NOT NULL,
    approved_at timestamp(6) with time zone,
    channel character varying(255) NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    customer_identifier character varying(100) NOT NULL,
    pending_approval boolean NOT NULL,
    role character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    CONSTRAINT conversations_channel_check CHECK (((channel)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'TELEGRAM'::character varying, 'EMAIL'::character varying])::text[]))),
    CONSTRAINT conversations_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ASSISTANT'::character varying])::text[])))
);

ALTER TABLE conversations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME conversations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE IF NOT EXISTS credential_audit_logs (
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    credential_id uuid NOT NULL,
    masked_value character varying(100),
    user_id uuid NOT NULL,
    CONSTRAINT credential_audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['VIEW'::character varying, 'CREATE'::character varying, 'UPDATE'::character varying, 'DELETE'::character varying, 'VERIFY'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS credential_fields (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    is_required boolean NOT NULL,
    field_key character varying(100) NOT NULL,
    label character varying(150) NOT NULL,
    placeholder character varying(255),
    service_id uuid NOT NULL,
    sort_order integer,
    type character varying(255),
    updated_at timestamp(6) with time zone,
    validation_regex character varying(255),
    CONSTRAINT credential_fields_type_check CHECK (((type)::text = ANY ((ARRAY['PASSWORD'::character varying, 'TEXT'::character varying, 'HOST'::character varying, 'PORT'::character varying, 'EMAIL'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS demo_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    token uuid DEFAULT gen_random_uuid() NOT NULL,
    prospect_email character varying(255) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    company_name character varying(150),
    agent_context text,
    blocked_at timestamp with time zone,
    block_reason character varying(255)
);

CREATE TABLE IF NOT EXISTS discounts (
    id uuid NOT NULL,
    applied_count integer,
    applies_to character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    created_by uuid NOT NULL,
    is_active boolean NOT NULL,
    label character varying(100),
    max_applications integer,
    reference_id uuid,
    tenant_id uuid,
    type character varying(255) NOT NULL,
    valid_from date,
    valid_until date,
    discount_value numeric(10,2) NOT NULL,
    applies_to_monthly boolean NOT NULL,
    applies_to_setup boolean NOT NULL,
    commitment_months integer NOT NULL,
    is_lifetime boolean NOT NULL,
    program character varying(255) NOT NULL,
    CONSTRAINT discounts_applies_to_check CHECK (((applies_to)::text = ANY ((ARRAY['BUDGET'::character varying, 'PHASE'::character varying, 'SERVICE'::character varying])::text[]))),
    CONSTRAINT discounts_program_check CHECK (((program)::text = ANY ((ARRAY['MANUAL'::character varying, 'EARLY_ADOPTER'::character varying, 'ANNUAL_CONTRACT'::character varying, 'REFERRAL'::character varying])::text[]))),
    CONSTRAINT discounts_type_check CHECK (((type)::text = ANY ((ARRAY['PERCENTAGE'::character varying, 'FIXED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS domain_dns_records (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    domain_id uuid NOT NULL,
    name character varying(253) NOT NULL,
    priority integer,
    ttl integer NOT NULL,
    type character varying(10) NOT NULL,
    value character varying(512) NOT NULL
);

CREATE TABLE IF NOT EXISTS early_adopter_programs (
    id uuid NOT NULL,
    active boolean NOT NULL,
    commitment_months integer NOT NULL,
    max_slots integer NOT NULL,
    monthly_discount_pct numeric(5,2) NOT NULL,
    setup_discount_pct numeric(5,2) NOT NULL,
    updated_at timestamp(6) with time zone,
    used_slots integer NOT NULL
);

CREATE TABLE IF NOT EXISTS expenses (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    category character varying(50),
    created_at timestamp(6) with time zone,
    description character varying(255) NOT NULL,
    holded_expense_id character varying(50),
    tenant_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS gocardless_configs (
    id uuid NOT NULL,
    api_key_ref character varying(100),
    created_at timestamp(6) with time zone,
    creditor_id character varying(50),
    environment character varying(255),
    is_active boolean,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    webhook_secret character varying(100),
    CONSTRAINT gocardless_configs_environment_check CHECK (((environment)::text = ANY ((ARRAY['SANDBOX'::character varying, 'LIVE'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS gocardless_mandates (
    id uuid NOT NULL,
    account_holder_name character varying(100),
    bank_name character varying(100),
    created_at timestamp(6) with time zone,
    gc_mandate_id character varying(50),
    gc_redirect_flow_id character varying(50),
    last_four_digits character varying(4),
    status character varying(255),
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT gocardless_mandates_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_SUBMISSION'::character varying, 'SUBMITTED'::character varying, 'ACTIVE'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'EXPIRED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS gocardless_payments (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    charge_date date,
    created_at timestamp(6) with time zone,
    failure_reason character varying(255),
    gc_payment_id character varying(50),
    monthly_invoice_id uuid NOT NULL,
    paid_out_at timestamp(6) with time zone,
    status character varying(255),
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT gocardless_payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_SUBMISSION'::character varying, 'SUBMITTED'::character varying, 'CONFIRMED'::character varying, 'PAID_OUT'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS holded_configs (
    id uuid NOT NULL,
    api_key_ref character varying(100) NOT NULL,
    created_at timestamp(6) with time zone,
    holded_company_id character varying(50),
    holded_contact_id character varying(50),
    is_active boolean,
    is_synced boolean,
    last_sync_at timestamp(6) with time zone,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS incidents (
    id uuid NOT NULL,
    alert_recovered boolean NOT NULL,
    alert_sent boolean NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(1000),
    duration_seconds bigint,
    resolved_at timestamp(6) with time zone,
    service_name character varying(50) NOT NULL,
    severity character varying(255),
    started_at timestamp(6) with time zone,
    status character varying(255),
    tenant_id uuid,
    title character varying(200),
    updated_at timestamp(6) with time zone,
    CONSTRAINT incidents_severity_check CHECK (((severity)::text = ANY ((ARRAY['CRITICAL'::character varying, 'WARNING'::character varying, 'INFO'::character varying])::text[]))),
    CONSTRAINT incidents_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ACKNOWLEDGED'::character varying, 'RESOLVED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS infra_metric_snapshots (
    id uuid NOT NULL,
    active_tenants integer,
    collected_at timestamp(6) with time zone NOT NULL,
    cpu_percent double precision,
    created_at timestamp(6) with time zone,
    db_active_connections integer,
    db_max_connections integer,
    disk_percent double precision,
    disk_total_gb bigint,
    disk_used_gb bigint,
    ram_percent double precision,
    ram_total_mb bigint,
    ram_used_mb bigint
);

CREATE TABLE IF NOT EXISTS invoices (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    budget_id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    currency character varying(3),
    due_date timestamp(6) with time zone,
    error_message character varying(500),
    holded_invoice_id character varying(50),
    invoice_number character varying(20),
    invoice_pdf_url character varying(500),
    is_active boolean,
    paid_at timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    tax_amount numeric(10,2),
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    verifactu_status character varying(255),
    CONSTRAINT invoices_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT invoices_verifactu_status_check CHECK (((verifactu_status)::text = ANY ((ARRAY['NOT_REQUIRED'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS knowledge_bases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_base_id uuid NOT NULL,
    filename character varying(255) NOT NULL,
    content_text text,
    is_active boolean DEFAULT true NOT NULL,
    uploaded_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_base_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    entry_key character varying(100) NOT NULL,
    content text NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS landing_templates (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    slug character varying(60) NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS landing_versions (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone,
    landing_id uuid NOT NULL,
    published_at timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    styles text,
    version_number integer NOT NULL,
    CONSTRAINT landing_versions_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS landings (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    custom_domain character varying(200),
    domain_owner_email character varying(200),
    domain_owner_name character varying(150),
    domain_owner_phone character varying(30),
    domain_registrar character varying(100),
    domain_renewal_date date,
    domain_renewal_price numeric(8,2),
    domain_status character varying(255),
    domain_verified boolean,
    is_active boolean NOT NULL,
    managed_domain boolean,
    meta_description character varying(300),
    og_image_url character varying(500),
    published_version_id uuid,
    service_id uuid NOT NULL,
    slug character varying(100) NOT NULL,
    status character varying(255) NOT NULL,
    template_id uuid,
    tenant_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    updated_at timestamp(6) with time zone,
    view_count bigint DEFAULT 0 NOT NULL,
    CONSTRAINT landings_domain_status_check CHECK (((domain_status)::text = ANY ((ARRAY['NOT_CONFIGURED'::character varying, 'PENDING_PURCHASE'::character varying, 'PURCHASED'::character varying, 'DNS_PENDING'::character varying, 'VERIFIED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT landings_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS lead_activities (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    description text NOT NULL,
    due_date timestamp(6) with time zone,
    lead_id uuid NOT NULL,
    type character varying(255),
    user_id uuid NOT NULL,
    CONSTRAINT lead_activities_type_check CHECK (((type)::text = ANY ((ARRAY['CALL'::character varying, 'EMAIL'::character varying, 'MEETING'::character varying, 'NOTE'::character varying, 'TASK'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS leads (
    id uuid NOT NULL,
    assigned_to uuid,
    converted_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    email character varying(150),
    estimated_value numeric(12,2),
    is_active boolean NOT NULL,
    lost_reason character varying(255),
    name character varying(150) NOT NULL,
    phone character varying(20),
    source character varying(255),
    stage character varying(255),
    tags character varying(500),
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    has_whatsapp boolean,
    notes text,
    CONSTRAINT leads_source_check CHECK (((source)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'WEB'::character varying, 'REFERRAL'::character varying, 'MANUAL'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT leads_stage_check CHECK (((stage)::text = ANY ((ARRAY['NEW'::character varying, 'CONTACTED'::character varying, 'QUALIFIED'::character varying, 'PROPOSAL'::character varying, 'NEGOTIATION'::character varying, 'WON'::character varying, 'LOST'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS managed_domains (
    id uuid NOT NULL,
    auto_renew boolean NOT NULL,
    created_at timestamp(6) with time zone,
    dns_configured boolean NOT NULL,
    domain_name character varying(253) NOT NULL,
    expires_at timestamp(6) with time zone,
    landing_id uuid,
    provider character varying(30),
    provider_domain_id character varying(100),
    purchase_price numeric(10,2),
    registered_at timestamp(6) with time zone,
    registrant_email character varying(150),
    registrant_name character varying(150),
    registrant_nif character varying(20),
    registrant_phone character varying(20),
    renewal_notified_at timestamp(6) with time zone,
    sale_price numeric(10,2),
    status character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    tld character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT managed_domains_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_PURCHASE'::character varying, 'REGISTERING'::character varying, 'ACTIVE'::character varying, 'DNS_PENDING'::character varying, 'TRANSFER_IN'::character varying, 'TRANSFER_OUT'::character varying, 'EXPIRING_SOON'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS message_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    type character varying(20) NOT NULL,
    subject character varying(200),
    body text NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS monthly_invoices (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    created_at timestamp(6) with time zone,
    holded_invoice_id character varying(50),
    invoice_number character varying(20),
    invoice_pdf_url character varying(500),
    period character varying(7) NOT NULL,
    sepa_collected boolean NOT NULL,
    sepa_collection_date date,
    status character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    tenant_name character varying(200),
    tenant_nif character varying(50),
    tenant_address character varying(500),
    tenant_email character varying(200),
    CONSTRAINT monthly_invoices_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS nexe_service_configs (
    tenant_id uuid NOT NULL,
    service_key character varying(30) NOT NULL,
    config_json text,
    updated_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    token_hash character varying(255) NOT NULL,
    used boolean NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    budget_id uuid NOT NULL,
    checkout_url character varying(500),
    created_at timestamp(6) with time zone,
    currency character varying(3),
    error_message character varying(500),
    invoice_id uuid,
    is_active boolean,
    paid_at timestamp(6) with time zone,
    refunded_at timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    stripe_payment_intent_id character varying(100),
    stripe_session_id character varying(100),
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS phases (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    name character varying(100) NOT NULL,
    profile_id uuid NOT NULL,
    sort_order integer NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS prospect_campaigns (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by uuid NOT NULL,
    location character varying(100) NOT NULL,
    name character varying(100) NOT NULL,
    notes character varying(500),
    search_params text,
    sector character varying(50) NOT NULL,
    source character varying(255),
    status character varying(255),
    total_exported integer,
    total_found integer,
    updated_at timestamp(6) with time zone,
    CONSTRAINT prospect_campaigns_source_check CHECK (((source)::text = ANY ((ARRAY['GOOGLE_MAPS'::character varying, 'INSTAGRAM'::character varying, 'PAGINAS_AMARILLAS'::character varying, 'MANUAL'::character varying])::text[]))),
    CONSTRAINT prospect_campaigns_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS prospects (
    id uuid NOT NULL,
    address character varying(255),
    campaign_id uuid NOT NULL,
    city character varying(100),
    created_at timestamp(6) with time zone,
    description character varying(500),
    email character varying(100),
    external_id character varying(100),
    google_place_id character varying(100),
    google_rating numeric(2,1),
    google_reviews integer,
    has_instagram boolean,
    has_website boolean,
    has_whatsapp boolean,
    instagram character varying(100),
    lead_id uuid,
    name character varying(150) NOT NULL,
    notes character varying(500),
    phone character varying(20),
    postal_code character varying(10),
    sector character varying(50),
    source character varying(255),
    status character varying(255),
    updated_at timestamp(6) with time zone,
    website character varying(300),
    CONSTRAINT prospects_source_check CHECK (((source)::text = ANY ((ARRAY['GOOGLE_MAPS'::character varying, 'INSTAGRAM'::character varying, 'PAGINAS_AMARILLAS'::character varying, 'MANUAL'::character varying])::text[]))),
    CONSTRAINT prospects_status_check CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'CONTACTED'::character varying, 'QUALIFIED'::character varying, 'EXPORTED'::character varying, 'DISCARDED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS referral_codes (
    id uuid NOT NULL,
    code character varying(20) NOT NULL,
    created_at timestamp(6) with time zone,
    credit_applied boolean,
    owner_tenant_id uuid NOT NULL,
    referred_setup_free boolean,
    referrer_credit_months integer,
    used_at timestamp(6) with time zone,
    used_by_tenant_id uuid
);

CREATE TABLE IF NOT EXISTS restore_tasks (
    id uuid NOT NULL,
    backup_task_id uuid,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    error_message character varying(1000),
    requested_by uuid NOT NULL,
    sections text,
    status character varying(20) NOT NULL,
    tenant_id uuid NOT NULL,
    CONSTRAINT restore_tasks_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'ROLLED_BACK'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS scaling_recommendations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    is_active boolean,
    message character varying(500) NOT NULL,
    resolved_at timestamp(6) with time zone,
    sent_at timestamp(6) with time zone,
    severity character varying(255) NOT NULL,
    threshold double precision,
    trigger_value double precision,
    type character varying(255) NOT NULL,
    CONSTRAINT scaling_recommendations_severity_check CHECK (((severity)::text = ANY ((ARRAY['WARNING'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT scaling_recommendations_type_check CHECK (((type)::text = ANY ((ARRAY['UPGRADE_CPU'::character varying, 'UPGRADE_RAM'::character varying, 'UPGRADE_DISK'::character varying, 'SEPARATE_DB'::character varying, 'SEPARATE_N8N'::character varying, 'MIGRATE_HETZNER_CLOUD'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS scheduled_agent_tasks (
    id uuid NOT NULL,
    agent_slug character varying(50) NOT NULL,
    created_at timestamp(6) with time zone,
    error_message text,
    executed_at timestamp(6) with time zone,
    payload text,
    scheduled_at timestamp(6) with time zone NOT NULL,
    status character varying(20) NOT NULL,
    task_type character varying(50) NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT scheduled_agent_tasks_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'EXECUTED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS sector_phases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sector character varying(30) NOT NULL,
    phase_number integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    dependency_type character varying(20) NOT NULL,
    required_phases character varying(50),
    setup_price numeric(10,2) NOT NULL,
    monthly_price numeric(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS sector_pricing (
    id uuid NOT NULL,
    business_size character varying(20) NOT NULL,
    created_at timestamp(6) with time zone,
    monthly_complete numeric(10,2) NOT NULL,
    monthlyf1 numeric(10,2) NOT NULL,
    monthly_f1f2 numeric(10,2) NOT NULL,
    monthly_f1f2f3 numeric(10,2) NOT NULL,
    sector character varying(30) NOT NULL,
    setup_price numeric(10,2) NOT NULL,
    updated_at timestamp(6) with time zone,
    price_f1 numeric(10,2) DEFAULT 0,
    price_f2 numeric(10,2) DEFAULT 0,
    price_f3 numeric(10,2) DEFAULT 0,
    price_f4 numeric(10,2) DEFAULT 0,
    price_f5 numeric(10,2) DEFAULT 0,
    setup_f2 numeric(10,2),
    setup_f3 numeric(10,2),
    setup_f4 numeric(10,2),
    setup_f5 numeric(10,2),
    CONSTRAINT sector_pricing_business_size_check CHECK (((business_size)::text = ANY ((ARRAY['AUTONOMO'::character varying, 'PETIT'::character varying, 'MITJA'::character varying])::text[]))),
    CONSTRAINT sector_pricing_sector_check CHECK (((sector)::text = ANY ((ARRAY['PINTOR'::character varying, 'ELECTRICISTA'::character varying, 'FONTANER'::character varying, 'JARDINER'::character varying, 'NETEJA'::character varying, 'FISIOTERAPEUTA'::character varying, 'PSICOLEG'::character varying, 'NUTRICIONISTA'::character varying, 'PERRUQUERIA'::character varying, 'ESTETICA'::character varying, 'GESTORIA'::character varying, 'ACADEMIA'::character varying, 'TALLER_MECANIC'::character varying, 'VETERINARI'::character varying, 'PERRUQUERIA_CANINA'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS sepa_mandates (
    id uuid NOT NULL,
    account_holder_name character varying(100) NOT NULL,
    bic character varying(11),
    created_at timestamp(6) with time zone,
    iban character varying(34) NOT NULL,
    is_active boolean NOT NULL,
    mandate_id character varying(35) NOT NULL,
    revoked_at timestamp(6) with time zone,
    signed_at date NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS service_health (
    id uuid NOT NULL,
    checked_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone,
    error_message character varying(500),
    response_time_ms bigint,
    service_name character varying(50) NOT NULL,
    status character varying(255),
    tenant_id uuid,
    updated_at timestamp(6) with time zone,
    CONSTRAINT service_health_status_check CHECK (((status)::text = ANY ((ARRAY['UP'::character varying, 'DOWN'::character varying, 'DEGRADED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS service_profiles (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    description character varying(255),
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    slug character varying(60) NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS stripe_configs (
    id uuid NOT NULL,
    api_key_ref character varying(100) NOT NULL,
    created_at timestamp(6) with time zone,
    is_active boolean,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    webhook_secret character varying(100)
);

CREATE TABLE IF NOT EXISTS system_settings (
    key character varying(80) NOT NULL,
    description character varying(255),
    encrypted_value text NOT NULL,
    is_secret boolean NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS template_sections (
    id uuid NOT NULL,
    block_type character varying(30) NOT NULL,
    created_at timestamp(6) with time zone,
    default_props text,
    props_schema text NOT NULL,
    sort_order integer NOT NULL,
    template_id uuid NOT NULL,
    CONSTRAINT template_sections_block_type_check CHECK (((block_type)::text = ANY ((ARRAY['HERO'::character varying, 'TEXT'::character varying, 'SERVICES'::character varying, 'GALLERY'::character varying, 'CONTACT_FORM'::character varying, 'FAQ'::character varying, 'TESTIMONIALS'::character varying, 'CTA'::character varying, 'FOOTER'::character varying, 'MAP'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_activated_phase_agents (
    id uuid NOT NULL,
    agent_type character varying(30) NOT NULL,
    created_at timestamp(6) with time zone,
    label character varying(200),
    phase_id uuid NOT NULL,
    status character varying(30),
    tenant_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS tenant_activated_phases (
    id uuid NOT NULL,
    activated_at timestamp(6) with time zone,
    budget_id uuid,
    monthly_price numeric(10,2),
    phase_name character varying(200),
    phase_number integer NOT NULL,
    sector character varying(255) NOT NULL,
    setup_price_paid numeric(10,2),
    status character varying(30),
    tenant_id uuid NOT NULL,
    CONSTRAINT tenant_activated_phases_sector_check CHECK (((sector)::text = ANY ((ARRAY['PINTOR'::character varying, 'ELECTRICISTA'::character varying, 'FONTANER'::character varying, 'JARDINER'::character varying, 'NETEJA'::character varying, 'FISIOTERAPEUTA'::character varying, 'PSICOLEG'::character varying, 'NUTRICIONISTA'::character varying, 'PERRUQUERIA'::character varying, 'ESTETICA'::character varying, 'GESTORIA'::character varying, 'ACADEMIA'::character varying, 'TALLER_MECANIC'::character varying, 'VETERINARI'::character varying, 'PERRUQUERIA_CANINA'::character varying, 'RESTAURANTE'::character varying, 'INMOBILIARIA'::character varying, 'AGENCIA_IA'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_ai_configs (
    tenant_id uuid NOT NULL,
    max_tokens integer,
    preferred_model character varying(255) NOT NULL,
    temperature double precision,
    budget_alert_threshold integer,
    monthly_token_budget integer,
    reasoning_model character varying(255)
);

CREATE TABLE IF NOT EXISTS tenant_chat_links (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    is_active boolean NOT NULL,
    link_code character varying(20),
    link_code_expires_at timestamp(6) with time zone,
    telegram_chat_id bigint,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    agent_mode character varying(255) NOT NULL,
    email_address character varying(100),
    whatsapp_phone_number character varying(20),
    whatsapp_meta_phone_number_id character varying(30),
    CONSTRAINT tenant_chat_links_agent_mode_check CHECK (((agent_mode)::text = ANY ((ARRAY['AUTO'::character varying, 'HYBRID'::character varying, 'MANUAL'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_credentials (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    encrypted_value character varying(500) NOT NULL,
    field_id uuid NOT NULL,
    is_set boolean NOT NULL,
    last_verified_at timestamp(6) with time zone,
    rotated_at timestamp(6) with time zone,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS tenant_phases (
    id uuid NOT NULL,
    approval_status character varying(255) NOT NULL,
    approved_at timestamp(6) with time zone,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    implementation_status character varying(255) NOT NULL,
    invoice_amount numeric(10,2),
    invoice_id character varying(100),
    invoice_status character varying(255),
    paid_at timestamp(6) with time zone,
    payment_status character varying(255),
    phase_id uuid NOT NULL,
    profile_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT tenant_phases_approval_status_check CHECK (((approval_status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT tenant_phases_implementation_status_check CHECK (((implementation_status)::text = ANY ((ARRAY['NOT_STARTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[]))),
    CONSTRAINT tenant_phases_invoice_status_check CHECK (((invoice_status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT tenant_phases_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_profiles (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    is_active boolean NOT NULL,
    phase_status character varying(255) NOT NULL,
    profile_id uuid NOT NULL,
    started_at timestamp(6) with time zone,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT tenant_profiles_phase_status_check CHECK (((phase_status)::text = ANY ((ARRAY['CONFIGURING'::character varying, 'AWAITING_CONFIRMATION'::character varying, 'COMPLETED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_service_addons (
    id uuid NOT NULL,
    added_by uuid NOT NULL,
    approval_required boolean NOT NULL,
    approval_status character varying(255),
    created_at timestamp(6) with time zone NOT NULL,
    service_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    CONSTRAINT tenant_service_addons_approval_status_check CHECK (((approval_status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_services (
    id uuid NOT NULL,
    activated_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    monthly_price_locked numeric(10,2) NOT NULL,
    phase_id uuid,
    service_id uuid NOT NULL,
    setup_price_locked numeric(10,2) NOT NULL,
    status character varying(255) NOT NULL,
    status_changed_at timestamp(6) with time zone,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    catalog_version_locked integer NOT NULL,
    outdated boolean NOT NULL,
    outdated_at timestamp(6) with time zone,
    is_enabled boolean NOT NULL,
    CONSTRAINT tenant_services_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'AWAITING_CLIENT'::character varying, 'CONFIGURED'::character varying, 'VERIFIED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_telegram_configs (
    id uuid NOT NULL,
    bot_token_encrypted character varying(1000),
    bot_username character varying(255),
    connected_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    status character varying(255) NOT NULL,
    tenant_id uuid NOT NULL,
    webhook_registered boolean,
    CONSTRAINT tenant_telegram_configs_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONNECTED'::character varying, 'ERROR'::character varying, 'DISCONNECTED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenant_workflows (
    id uuid NOT NULL,
    config text,
    created_at timestamp(6) with time zone,
    error_message character varying(500),
    is_active boolean,
    last_run_at timestamp(6) with time zone,
    last_run_status character varying(255),
    n8n_webhook_url character varying(500),
    n8n_workflow_id character varying(50),
    status character varying(255),
    template_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT tenant_workflows_last_run_status_check CHECK (((last_run_status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'TIMEOUT'::character varying])::text[]))),
    CONSTRAINT tenant_workflows_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'DEPLOYED'::character varying, 'ACTIVE'::character varying, 'ERROR'::character varying, 'DISABLED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS tenants (
    id uuid NOT NULL,
    address character varying(255),
    contact_phone character varying(20),
    created_at timestamp(6) with time zone,
    email character varying(150),
    is_active boolean NOT NULL,
    name character varying(100) NOT NULL,
    nif character varying(20),
    phone character varying(20),
    preferred_channel character varying(20),
    slug character varying(60) NOT NULL,
    updated_at timestamp(6) with time zone,
    agent_system_prompt text,
    business_size character varying(20),
    contracted_phases text,
    sector character varying(30),
    is_free boolean DEFAULT false NOT NULL,
    CONSTRAINT tenants_business_size_check CHECK (((business_size)::text = ANY ((ARRAY['AUTONOMO'::character varying, 'PETIT'::character varying, 'MITJA'::character varying])::text[]))),
    CONSTRAINT tenants_preferred_channel_check CHECK (((preferred_channel)::text = ANY ((ARRAY['WHATSAPP'::character varying, 'TELEGRAM'::character varying, 'EMAIL'::character varying])::text[]))),
    CONSTRAINT tenants_sector_check CHECK (((sector)::text = ANY (ARRAY['PINTOR'::text, 'ELECTRICISTA'::text, 'FONTANER'::text, 'JARDINER'::text, 'NETEJA'::text, 'FISIOTERAPEUTA'::text, 'PSICOLEG'::text, 'NUTRICIONISTA'::text, 'PERRUQUERIA'::text, 'ESTETICA'::text, 'GESTORIA'::text, 'ACADEMIA'::text, 'TALLER_MECANIC'::text, 'VETERINARI'::text, 'PERRUQUERIA_CANINA'::text, 'RESTAURANTE'::text, 'INMOBILIARIA'::text, 'AGENCIA_IA'::text])))
);

CREATE TABLE IF NOT EXISTS tld_pricing (
    tld character varying(20) NOT NULL,
    cost_register numeric(10,2),
    cost_renew numeric(10,2),
    is_active boolean NOT NULL,
    sale_register numeric(10,2),
    sale_renew numeric(10,2),
    updated_at timestamp(6) with time zone
);

CREATE TABLE IF NOT EXISTS token_usage_logs (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    input_tokens integer NOT NULL,
    model character varying(100) NOT NULL,
    output_tokens integer NOT NULL,
    task_type character varying(50) NOT NULL,
    tenant_id uuid NOT NULL
);

ALTER TABLE token_usage_logs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME token_usage_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE IF NOT EXISTS users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    email character varying(150) NOT NULL,
    failed_attempts integer NOT NULL,
    is_active boolean NOT NULL,
    is_blocked boolean NOT NULL,
    last_login_at timestamp(6) with time zone,
    name character varying(100) NOT NULL,
    password_changed_at timestamp(6) with time zone,
    password_hash character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    tenant_id uuid,
    updated_at timestamp(6) with time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['SUPER_ADMIN'::character varying, 'ADMIN'::character varying, 'CLIENT'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS whatsapp_waba_configs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tenant_id uuid NOT NULL,
    waba_id character varying(255),
    phone_number_id character varying(255),
    access_token_encrypted character varying(1000),
    display_phone_number character varying(255),
    business_name character varying(255),
    status character varying(255) DEFAULT 'PENDING'::character varying NOT NULL,
    webhook_registered boolean DEFAULT false NOT NULL,
    connected_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_executions (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    error_message character varying(500),
    executed_at timestamp(6) with time zone,
    n8n_execution_id character varying(50),
    request_payload text,
    response_payload text,
    source_id character varying(100),
    status character varying(255),
    tenant_workflow_id uuid NOT NULL,
    trigger_type character varying(255),
    CONSTRAINT workflow_executions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'TIMEOUT'::character varying])::text[]))),
    CONSTRAINT workflow_executions_trigger_type_check CHECK (((trigger_type)::text = ANY ((ARRAY['FORM_SUBMIT'::character varying, 'LEAD_UPDATE'::character varying, 'COMMUNICATION'::character varying, 'SCHEDULED'::character varying, 'WEBHOOK'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS workflow_templates (
    id uuid NOT NULL,
    activation_type character varying(255),
    category character varying(255),
    created_at timestamp(6) with time zone,
    description character varying(300),
    is_active boolean,
    template_key character varying(50) NOT NULL,
    n8n_workflow_json text,
    name character varying(100) NOT NULL,
    setup_guide text,
    CONSTRAINT workflow_templates_activation_type_check CHECK (((activation_type)::text = ANY ((ARRAY['AUTOMATIC'::character varying, 'MANUAL'::character varying])::text[]))),
    CONSTRAINT workflow_templates_category_check CHECK (((category)::text = ANY ((ARRAY['BASIC'::character varying, 'ADVANCED'::character varying, 'BOT_IA'::character varying, 'SMTP'::character varying, 'WHATSAPP'::character varying])::text[])))
);

ALTER TABLE ONLY assets
    ADD CONSTRAINT assets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY backup_export_logs
    ADD CONSTRAINT backup_export_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY backup_records
    ADD CONSTRAINT backup_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY backup_tasks
    ADD CONSTRAINT backup_tasks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY budget_lines
    ADD CONSTRAINT budget_lines_pkey PRIMARY KEY (id);

ALTER TABLE ONLY budgets
    ADD CONSTRAINT budgets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY catalog_services
    ADD CONSTRAINT catalog_services_pkey PRIMARY KEY (id);

ALTER TABLE ONLY communication_requests
    ADD CONSTRAINT communication_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY contact_identifiers
    ADD CONSTRAINT contact_identifiers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY contact_leads
    ADD CONSTRAINT contact_leads_pkey PRIMARY KEY (id);

ALTER TABLE ONLY contacts
    ADD CONSTRAINT contacts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY conversations
    ADD CONSTRAINT conversations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY credential_audit_logs
    ADD CONSTRAINT credential_audit_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY credential_fields
    ADD CONSTRAINT credential_fields_pkey PRIMARY KEY (id);

ALTER TABLE ONLY demo_sessions
    ADD CONSTRAINT demo_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY demo_sessions
    ADD CONSTRAINT demo_sessions_token_key UNIQUE (token);

ALTER TABLE ONLY discounts
    ADD CONSTRAINT discounts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY domain_dns_records
    ADD CONSTRAINT domain_dns_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY early_adopter_programs
    ADD CONSTRAINT early_adopter_programs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gocardless_configs
    ADD CONSTRAINT gocardless_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gocardless_mandates
    ADD CONSTRAINT gocardless_mandates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gocardless_payments
    ADD CONSTRAINT gocardless_payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY holded_configs
    ADD CONSTRAINT holded_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sector_phases
    ADD CONSTRAINT idx_sector_phase_unique UNIQUE (sector, phase_number);

ALTER TABLE ONLY incidents
    ADD CONSTRAINT incidents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY infra_metric_snapshots
    ADD CONSTRAINT infra_metric_snapshots_pkey PRIMARY KEY (id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY knowledge_bases
    ADD CONSTRAINT knowledge_bases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY knowledge_bases
    ADD CONSTRAINT knowledge_bases_tenant_id_key UNIQUE (tenant_id);

ALTER TABLE ONLY knowledge_documents
    ADD CONSTRAINT knowledge_documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY knowledge_entries
    ADD CONSTRAINT knowledge_entries_knowledge_base_id_entry_key_key UNIQUE (knowledge_base_id, entry_key);

ALTER TABLE ONLY knowledge_entries
    ADD CONSTRAINT knowledge_entries_pkey PRIMARY KEY (id);

ALTER TABLE ONLY landing_templates
    ADD CONSTRAINT landing_templates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY landing_versions
    ADD CONSTRAINT landing_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY landings
    ADD CONSTRAINT landings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY lead_activities
    ADD CONSTRAINT lead_activities_pkey PRIMARY KEY (id);

ALTER TABLE ONLY leads
    ADD CONSTRAINT leads_pkey PRIMARY KEY (id);

ALTER TABLE ONLY managed_domains
    ADD CONSTRAINT managed_domains_pkey PRIMARY KEY (id);

ALTER TABLE ONLY message_templates
    ADD CONSTRAINT message_templates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY monthly_invoices
    ADD CONSTRAINT monthly_invoices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY nexe_service_configs
    ADD CONSTRAINT nexe_service_configs_pkey PRIMARY KEY (tenant_id, service_key);

ALTER TABLE ONLY password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY phases
    ADD CONSTRAINT phases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY prospect_campaigns
    ADD CONSTRAINT prospect_campaigns_pkey PRIMARY KEY (id);

ALTER TABLE ONLY prospects
    ADD CONSTRAINT prospects_pkey PRIMARY KEY (id);

ALTER TABLE ONLY referral_codes
    ADD CONSTRAINT referral_codes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY restore_tasks
    ADD CONSTRAINT restore_tasks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY scaling_recommendations
    ADD CONSTRAINT scaling_recommendations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY scheduled_agent_tasks
    ADD CONSTRAINT scheduled_agent_tasks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sector_phases
    ADD CONSTRAINT sector_phases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sector_pricing
    ADD CONSTRAINT sector_pricing_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sepa_mandates
    ADD CONSTRAINT sepa_mandates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY service_health
    ADD CONSTRAINT service_health_pkey PRIMARY KEY (id);

ALTER TABLE ONLY service_profiles
    ADD CONSTRAINT service_profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stripe_configs
    ADD CONSTRAINT stripe_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (key);

ALTER TABLE ONLY template_sections
    ADD CONSTRAINT template_sections_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_activated_phase_agents
    ADD CONSTRAINT tenant_activated_phase_agents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_activated_phases
    ADD CONSTRAINT tenant_activated_phases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_ai_configs
    ADD CONSTRAINT tenant_ai_configs_pkey PRIMARY KEY (tenant_id);

ALTER TABLE ONLY tenant_chat_links
    ADD CONSTRAINT tenant_chat_links_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_credentials
    ADD CONSTRAINT tenant_credentials_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_phases
    ADD CONSTRAINT tenant_phases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_profiles
    ADD CONSTRAINT tenant_profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_service_addons
    ADD CONSTRAINT tenant_service_addons_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_services
    ADD CONSTRAINT tenant_services_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_telegram_configs
    ADD CONSTRAINT tenant_telegram_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenant_workflows
    ADD CONSTRAINT tenant_workflows_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tenants
    ADD CONSTRAINT tenants_pkey PRIMARY KEY (id);

ALTER TABLE ONLY tld_pricing
    ADD CONSTRAINT tld_pricing_pkey PRIMARY KEY (tld);

ALTER TABLE ONLY token_usage_logs
    ADD CONSTRAINT token_usage_logs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY monthly_invoices
    ADD CONSTRAINT uk1u15dewo56v0ihcg4ds2oy71u UNIQUE (tenant_id, period);

ALTER TABLE ONLY managed_domains
    ADD CONSTRAINT uk1uflo2anbnxrlrfh10y25f3jt UNIQUE (domain_name);

ALTER TABLE ONLY service_profiles
    ADD CONSTRAINT uk2145oyimccgyf3klpkw6rmqxc UNIQUE (slug);

ALTER TABLE ONLY prospects
    ADD CONSTRAINT uk221xd9dg9a779lsq3ecllvxuv UNIQUE (google_place_id);

ALTER TABLE ONLY sepa_mandates
    ADD CONSTRAINT uk22xitwpqnq4wavgvhrb7t4h16 UNIQUE (mandate_id);

ALTER TABLE ONLY payments
    ADD CONSTRAINT uk24qk9mseaueib5d38726wq6rm UNIQUE (invoice_id);

ALTER TABLE ONLY contact_identifiers
    ADD CONSTRAINT uk3pjs5t522ov57av0qi39ukggf UNIQUE (tenant_id, channel, identifier);

ALTER TABLE ONLY tenant_services
    ADD CONSTRAINT uk3rxktxh2n2jw5xcrhxyxktpqv UNIQUE (tenant_id, service_id);

ALTER TABLE ONLY sepa_mandates
    ADD CONSTRAINT uk41b0nd4f3orqc9mj53vih7503 UNIQUE (tenant_id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT uk5mua3nq3mecic0v6ut92dcg8w UNIQUE (holded_invoice_id);

ALTER TABLE ONLY gocardless_configs
    ADD CONSTRAINT uk61xggls8m7x8jjhie5l9ff0th UNIQUE (tenant_id);

ALTER TABLE ONLY gocardless_mandates
    ADD CONSTRAINT uk65r2t8q9i01ywn7ug8gw8mt6m UNIQUE (tenant_id);

ALTER TABLE ONLY users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY tenant_telegram_configs
    ADD CONSTRAINT uk6utboog83yrhdry0kts4cyhfe UNIQUE (tenant_id);

ALTER TABLE ONLY tenant_chat_links
    ADD CONSTRAINT uk8flwypgym7vbhq7ognjf6ndmf UNIQUE (tenant_id);

ALTER TABLE ONLY tenant_activated_phases
    ADD CONSTRAINT uk8mj9p08pglf2ow0m1wm69hdp9 UNIQUE (tenant_id, sector, phase_number);

ALTER TABLE ONLY payments
    ADD CONSTRAINT uk8y1jcokiqmgv906rh3523js51 UNIQUE (budget_id);

ALTER TABLE ONLY landings
    ADD CONSTRAINT uka58v7c876m19207e03shnopau UNIQUE (custom_domain);

ALTER TABLE ONLY monthly_invoices
    ADD CONSTRAINT ukajpaq0400djbx3qnedyj9yh4v UNIQUE (holded_invoice_id);

ALTER TABLE ONLY sector_pricing
    ADD CONSTRAINT ukayy701k7i58wen0j09fg1gw5u UNIQUE (sector, business_size);

ALTER TABLE ONLY holded_configs
    ADD CONSTRAINT ukbot5gswg1luudvbta9ajs6ows UNIQUE (tenant_id);

ALTER TABLE ONLY stripe_configs
    ADD CONSTRAINT ukcixiaaynl89bvkjmh64u6urvj UNIQUE (tenant_id);

ALTER TABLE ONLY tenant_profiles
    ADD CONSTRAINT uke9b2pssnd5a40i3d0yp3wbmob UNIQUE (tenant_id, profile_id);

ALTER TABLE ONLY landing_templates
    ADD CONSTRAINT ukeeshjgpmn9jjl1yj6b9rscd6m UNIQUE (slug);

ALTER TABLE ONLY referral_codes
    ADD CONSTRAINT ukentgo51unoecb2ivf23bvunn7 UNIQUE (code);

ALTER TABLE ONLY workflow_templates
    ADD CONSTRAINT uki4goytbh7dm7h4s2denq5w1jl UNIQUE (template_key);

ALTER TABLE ONLY tenant_service_addons
    ADD CONSTRAINT uki51icn8scmhn8wv0cka6une0p UNIQUE (tenant_id, service_id);

ALTER TABLE ONLY service_profiles
    ADD CONSTRAINT uki7y2nogqfxo9n1cy2tghr3v4j UNIQUE (name);

ALTER TABLE ONLY landings
    ADD CONSTRAINT ukj3nfsdbyjcn1xivhshmo4eb2t UNIQUE (tenant_id, slug);

ALTER TABLE ONLY payments
    ADD CONSTRAINT ukk0ew22hb3du79tp0hdgu7qybp UNIQUE (stripe_session_id);

ALTER TABLE ONLY tenants
    ADD CONSTRAINT ukkn82rs0p55luybrg4n7x7di8 UNIQUE (slug);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT ukm67ipyeto3v6nixudjg7u8lun UNIQUE (budget_id);

ALTER TABLE ONLY tenant_phases
    ADD CONSTRAINT ukmyqf3ixao81uo8cbhudlx0fvt UNIQUE (tenant_id, phase_id);

ALTER TABLE ONLY catalog_services
    ADD CONSTRAINT uko11mp1q9vkl8owr8xk0myjx3o UNIQUE (slug);

ALTER TABLE ONLY landing_versions
    ADD CONSTRAINT ukprlvsjmf6csahmo6atqrdkl3n UNIQUE (landing_id, version_number);

ALTER TABLE ONLY payments
    ADD CONSTRAINT ukpuc8mkpduwb4ws7khxcoo0s3t UNIQUE (stripe_payment_intent_id);

ALTER TABLE ONLY tenant_chat_links
    ADD CONSTRAINT ukqk6tn3sip6q1xm6l5s5j1hjs0 UNIQUE (link_code);

ALTER TABLE ONLY tenant_credentials
    ADD CONSTRAINT uktfw10dk7pvu6in5orcwraixw6 UNIQUE (tenant_id, field_id);

ALTER TABLE ONLY sector_phases
    ADD CONSTRAINT uq_sector_phase UNIQUE (sector, phase_number);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY whatsapp_waba_configs
    ADD CONSTRAINT whatsapp_waba_configs_phone_number_id_key UNIQUE (phone_number_id);

ALTER TABLE ONLY whatsapp_waba_configs
    ADD CONSTRAINT whatsapp_waba_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY whatsapp_waba_configs
    ADD CONSTRAINT whatsapp_waba_configs_tenant_id_key UNIQUE (tenant_id);

ALTER TABLE ONLY workflow_executions
    ADD CONSTRAINT workflow_executions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY workflow_templates
    ADD CONSTRAINT workflow_templates_pkey PRIMARY KEY (id);

CREATE INDEX IF NOT EXISTS idx_budget_tenant_number ON budgets USING btree (tenant_id, budget_number);


--
-- Name: idx_budget_tenant_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_budget_tenant_status ON budgets USING btree (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_infra_metric_collected_at ON infra_metric_snapshots USING btree (collected_at);


--
-- Name: idx_knowledge_entries_kb; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_knowledge_entries_kb ON knowledge_entries USING btree (knowledge_base_id, category);

CREATE INDEX IF NOT EXISTS idx_sector_phase_sector ON sector_phases USING btree (sector, phase_number);


--
-- Name: idx_tenant_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant_workflows USING btree (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_tenant_template_active ON tenant_workflows USING btree (tenant_id, template_id, is_active);


--
-- Name: idx_token_usage_tenant_ts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_token_usage_tenant_ts ON token_usage_logs USING btree (tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idxi0xqm32tgkibasi42i172fjdv ON conversations USING btree (tenant_id, customer_identifier, channel);


--
-- Name: idxif3c8equjc1cxwp00fv59aeuv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idxif3c8equjc1cxwp00fv59aeuv ON referral_codes USING btree (owner_tenant_id);

CREATE INDEX IF NOT EXISTS idxq3xicrsow4tvnyu140auo134c ON conversations USING btree (tenant_id, pending_approval);


--
-- Name: knowledge_documents knowledge_documents_knowledge_base_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY knowledge_documents
    ADD CONSTRAINT knowledge_documents_knowledge_base_id_fkey FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE;

ALTER TABLE ONLY knowledge_entries
    ADD CONSTRAINT knowledge_entries_knowledge_base_id_fkey FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE;
