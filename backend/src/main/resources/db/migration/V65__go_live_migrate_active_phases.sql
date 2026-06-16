-- Tenants ja configurats i en producció: propaga contracted_phases → active_phases
-- perquè l'agent continuï funcionant amb la nova lògica estricta.
-- Tenants nous (contractats via porta de pagament) tindran active_phases = NULL fins
-- que SUPER_ADMIN faci POST /tenants/{id}/go-live manualment.
UPDATE tenants
SET active_phases = contracted_phases
WHERE contracted_phases IS NOT NULL
  AND contracted_phases <> ''
  AND active_phases IS NULL;
