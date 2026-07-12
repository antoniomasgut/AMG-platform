-- Actualitza la CHECK constraint de scaling_recommendations.type per incloure els tipus nous
-- (MEMORY_NO_SWAP, CONTAINER_UNHEALTHY). A producció ddl-auto=validate, així que la constraint
-- auto-generada per Hibernate no incloïa els valors nous → INSERT fallava amb violació de constraint.
ALTER TABLE scaling_recommendations DROP CONSTRAINT IF EXISTS scaling_recommendations_type_check;
ALTER TABLE scaling_recommendations ADD CONSTRAINT scaling_recommendations_type_check
  CHECK (type IN (
    'UPGRADE_CPU',
    'UPGRADE_RAM',
    'UPGRADE_DISK',
    'SEPARATE_DB',
    'SEPARATE_N8N',
    'MIGRATE_HETZNER_CLOUD',
    'MEMORY_NO_SWAP',
    'CONTAINER_UNHEALTHY'
  ));
