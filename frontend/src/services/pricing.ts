// ── NexeLocal Formula Pricing Model ───────────────────────────────────────────
// Monthly = Σ(phase base costs) × sector_factor × size_factor

export interface PricingBreakdown {
  phaseSubtotals: Record<number, number>;
  baseTotal: number;
  sectorFactor: number;
  sizeFactor: number;
  monthlyTotal: number;
  setupTotal: number;
}

// ── Phase base costs (€/month) ────────────────────────────────────────────────

const COMPONENT_COSTS = {
  CAPTACIO:    25,
  RESPOSTA:    15,
  AGENDA:      20,
  SEGUIMENT:   25,
  COBRAMENTS:  15,
  FIDELITZACIO: 15,
  OPERACIONS:  20,
  DOCUMENTACIO: 30,
} as const;

// F1-F5 monthly base cost = sum of their components
export const F_PHASE_MONTHLY: Record<number, number> = {
  1: COMPONENT_COSTS.CAPTACIO + COMPONENT_COSTS.RESPOSTA,              // 40€
  2: COMPONENT_COSTS.AGENDA,                                             // 20€
  3: COMPONENT_COSTS.SEGUIMENT + COMPONENT_COSTS.COBRAMENTS,           // 40€
  4: COMPONENT_COSTS.FIDELITZACIO,                                       // 15€
  5: COMPONENT_COSTS.OPERACIONS + COMPONENT_COSTS.DOCUMENTACIO,        // 50€
};

// Setup = 2× first month as default
const SETUP_MULTIPLIER = 2;

// ── Sector factors ─────────────────────────────────────────────────────────────

type SectorCategory = 'OFICIS' | 'SALUT' | 'DENTAL_INMO' | 'GESTORIA';

export const SECTOR_CATEGORY: Record<string, SectorCategory> = {
  // Oficis — ×1.0
  ELECTRICISTA: 'OFICIS', PINTOR: 'OFICIS', FONTANER: 'OFICIS',
  REFORMES: 'OFICIS', JARDINER: 'OFICIS', NETEJA: 'OFICIS',
  TALLER_MECANIC: 'OFICIS', RESTAURANTE: 'OFICIS',
  // Salut — ×1.2
  FISIOTERAPEUTA: 'SALUT', PSICOLEG: 'SALUT', PERRUQUERIA: 'SALUT',
  ESTETICA: 'SALUT', ENTRENADOR: 'SALUT', NUTRICIONISTA: 'SALUT',
  VETERINARI: 'SALUT', PERRUQUERIA_CANINA: 'SALUT',
  // Dental / Inmobiliària — ×1.5
  DENTISTA: 'DENTAL_INMO', INMOBILIARIA: 'DENTAL_INMO',
  // Gestoria / Acadèmia / Legal — ×1.8
  GESTORIA: 'GESTORIA', ADVOCAT: 'GESTORIA', ACADEMIA: 'GESTORIA',
  AGENCIA_IA: 'GESTORIA',
};

export const CATEGORY_FACTORS: Record<SectorCategory, number> = {
  OFICIS:      1.0,
  SALUT:       1.2,
  DENTAL_INMO: 1.5,
  GESTORIA:    1.8,
};

export const CATEGORY_LABELS: Record<SectorCategory, string> = {
  OFICIS:      'Oficis',
  SALUT:       'Salut',
  DENTAL_INMO: 'Dental / Inmobiliària',
  GESTORIA:    'Gestoria / Acadèmia',
};

// ── Size factors ──────────────────────────────────────────────────────────────

export const SIZE_FACTORS: Record<string, number> = {
  AUTONOMO: 1.0,
  PETIT:    1.5,
  MITJA:    2.0,
  EMPRESA:  3.0,
};

// ── Calculator ────────────────────────────────────────────────────────────────

export function calculatePrice(
  phaseNums: number[],
  sector: string,
  businessSize: string,
): PricingBreakdown {
  const category = SECTOR_CATEGORY[sector] ?? 'OFICIS';
  const sectorFactor = CATEGORY_FACTORS[category];
  const sizeFactor   = SIZE_FACTORS[businessSize] ?? 1.0;

  const phaseSubtotals: Record<number, number> = {};
  let baseTotal = 0;

  for (const num of phaseNums) {
    const cost = F_PHASE_MONTHLY[num] ?? 0;
    phaseSubtotals[num] = cost;
    baseTotal += cost;
  }

  const monthlyTotal = Math.round(baseTotal * sectorFactor * sizeFactor);
  const setupTotal   = Math.round(monthlyTotal * SETUP_MULTIPLIER);

  return { phaseSubtotals, baseTotal, sectorFactor, sizeFactor, monthlyTotal, setupTotal };
}

export function getSectorFactor(sector: string): number {
  const category = SECTOR_CATEGORY[sector] ?? 'OFICIS';
  return CATEGORY_FACTORS[category];
}

export function getSectorCategoryLabel(sector: string): string {
  const category = SECTOR_CATEGORY[sector] ?? 'OFICIS';
  return CATEGORY_LABELS[category];
}
