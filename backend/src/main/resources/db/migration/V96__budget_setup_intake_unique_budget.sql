-- Un pressupost acceptat no pot tenir més d'una fitxa de configuració
ALTER TABLE budget_setup_intakes ADD CONSTRAINT uq_budget_setup_intakes_budget_id UNIQUE (budget_id);
