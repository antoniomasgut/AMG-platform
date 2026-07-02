-- Mòdul 12 v2.2: flag per enviar el pitch automàticament per email al generar l'informe IA
ALTER TABLE prospect_campaigns ADD COLUMN IF NOT EXISTS auto_send_email BOOLEAN NOT NULL DEFAULT false;
