'use client';

import { useState, useRef, useEffect, type FC } from 'react';
import { useEditorStore } from '@/store/editor';
import { BUSINESS_TYPES, updateLandingMeta } from '@/services/factory';
import { getCurrentUser } from '@/services/auth';

function extractColorsFromImage(img: HTMLImageElement): [string, string] {
  const canvas = document.createElement('canvas');
  const size = 80;
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext('2d')!;
  ctx.drawImage(img, 0, 0, size, size);
  const data = ctx.getImageData(0, 0, size, size).data;

  const buckets: Record<string, { r: number; g: number; b: number; count: number }> = {};
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i], g = data[i + 1], b = data[i + 2], a = data[i + 3];
    if (a < 128) continue; // transparent
    // Quantize to 32-step buckets to group similar colors
    const key = `${Math.round(r / 32) * 32},${Math.round(g / 32) * 32},${Math.round(b / 32) * 32}`;
    if (!buckets[key]) buckets[key] = { r: Math.round(r / 32) * 32, g: Math.round(g / 32) * 32, b: Math.round(b / 32) * 32, count: 0 };
    buckets[key].count++;
  }
  const sorted = Object.values(buckets)
    .filter((c) => {
      // Discard near-white and near-black (background/foreground noise)
      const lum = 0.299 * c.r + 0.587 * c.g + 0.114 * c.b;
      return lum > 20 && lum < 235;
    })
    .sort((a, b) => b.count - a.count);

  const toHex = (c: { r: number; g: number; b: number }) =>
    '#' + [c.r, c.g, c.b].map((v) => v.toString(16).padStart(2, '0')).join('');

  const primary = sorted[0] ?? { r: 255, g: 107, b: 0 };
  // Accent: first color that's visually distinct from primary (min 80 distance)
  const accent = sorted.find((c) => {
    const dr = c.r - primary.r, dg = c.g - primary.g, db = c.b - primary.b;
    return Math.sqrt(dr * dr + dg * dg + db * db) > 80;
  }) ?? sorted[1] ?? { r: 30, g: 41, b: 59 };

  return [toHex(primary), toHex(accent)];
}

const HEADING_FONTS = [
  { value: 'Inter, sans-serif',                label: 'Inter — minimalista modern' },
  { value: 'Poppins, sans-serif',              label: 'Poppins — geomètric versàtil' },
  { value: 'Montserrat, sans-serif',           label: 'Montserrat — modern clàssic' },
  { value: 'Playfair Display, serif',          label: 'Playfair Display — luxe elegant' },
  { value: 'Oswald, sans-serif',               label: 'Oswald — impacte fort' },
  { value: 'Raleway, sans-serif',              label: 'Raleway — contemporani' },
  { value: 'Nunito, sans-serif',               label: 'Nunito — amigable familiar' },
  { value: 'DM Sans, sans-serif',              label: 'DM Sans — clean premium' },
  { value: 'Cormorant Garamond, serif',        label: 'Cormorant Garamond — clàssic luxe' },
  { value: 'Space Grotesk, sans-serif',        label: 'Space Grotesk — tech · startup' },
  { value: 'Outfit, sans-serif',               label: 'Outfit — molt modern' },
  { value: 'Josefin Sans, sans-serif',         label: 'Josefin Sans — elegant geomètric' },
  { value: 'Rubik, sans-serif',                label: 'Rubik — modern arrodonit' },
  { value: 'Plus Jakarta Sans, sans-serif',    label: 'Plus Jakarta Sans — premium 2024' },
  { value: 'Roboto Slab, serif',               label: 'Roboto Slab — sòlid fiable' },
];

const BODY_FONTS = [
  { value: 'Inter, sans-serif',               label: 'Inter — UI estàndard' },
  { value: 'Lato, sans-serif',                label: 'Lato — humanista càlid' },
  { value: 'Open Sans, sans-serif',           label: 'Open Sans — universal' },
  { value: 'Roboto, sans-serif',              label: 'Roboto — Android / Google' },
  { value: 'Source Sans Pro, sans-serif',     label: 'Source Sans Pro — professional' },
  { value: 'Merriweather, serif',             label: 'Merriweather — serif llegible' },
  { value: 'Nunito, sans-serif',              label: 'Nunito — arrodonit net' },
  { value: 'DM Sans, sans-serif',             label: 'DM Sans — minimalista' },
  { value: 'Noto Sans, sans-serif',           label: 'Noto Sans — multilingüe (ca/es/en/de)' },
  { value: 'Rubik, sans-serif',               label: 'Rubik — modern còmode' },
  { value: 'Poppins, sans-serif',             label: 'Poppins — versàtil' },
  { value: 'Plus Jakarta Sans, sans-serif',   label: 'Plus Jakarta Sans — premium' },
];

const COLOR_FIELDS: Array<{ key: 'primaryColor' | 'accentColor'; label: string; hint: string }> = [
  { key: 'primaryColor', label: 'Color principal', hint: 'Botons, encapçalaments, links' },
  { key: 'accentColor',  label: 'Color d\'accent',  hint: 'Detalls, icones, ressaltats' },
];

const PALETTES: Array<{ name: string; primary: string; accent: string; bg: string; text: string }> = [
  { name: 'Taronja',    primary: '#FF6B00', accent: '#FF9A3C', bg: '#0d0d1a', text: '#f8f9fa' },
  { name: 'Blau Fosc',  primary: '#1a365d', accent: '#3182ce', bg: '#f8fafc', text: '#1e293b' },
  { name: 'Verd',       primary: '#276749', accent: '#f6ad55', bg: '#f7faf7', text: '#1a1a1a' },
  { name: 'Porpra',     primary: '#553c9a', accent: '#9f7aea', bg: '#fdfaff', text: '#1a202c' },
  { name: 'Vermell',    primary: '#c8423a', accent: '#f5a623', bg: '#fffbf5', text: '#1a1a1a' },
  { name: 'Negre+Or',   primary: '#1a202c', accent: '#f6b94b', bg: '#f8f8f8', text: '#1a202c' },
  { name: 'Gris',       primary: '#2d3748', accent: '#e53e3e', bg: '#ffffff', text: '#2d3748' },
  { name: 'Or',         primary: '#2c5282', accent: '#c8a951', bg: '#fafaf8', text: '#1a202c' },
];

export const PageStylesPanel: FC = () => {
  const styles = useEditorStore((s) => s.styles);
  const setStyles = useEditorStore((s) => s.setStyles);
  const landing = useEditorStore((s) => s.landing);
  const [logoUrl, setLogoUrl] = useState('');
  const [extracting, setExtracting] = useState(false);
  const [extractError, setExtractError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);
  const [metaDesc, setMetaDesc] = useState('');
  const [ogImage, setOgImage] = useState('');
  const [metaSaving, setMetaSaving] = useState(false);
  const user = getCurrentUser();

  useEffect(() => {
    if (landing) {
      setMetaDesc(landing.metaDescription ?? '');
      setOgImage(landing.ogImageUrl ?? '');
    }
  }, [landing?.id]);

  const saveMeta = async () => {
    if (!user?.tenantId || !landing?.id) return;
    setMetaSaving(true);
    try {
      await updateLandingMeta(user.tenantId, landing.id, {
        metaDescription: metaDesc || undefined,
        ogImageUrl: ogImage || undefined,
      });
    } finally {
      setMetaSaving(false);
    }
  };

  const handleExtract = (src: string) => {
    setExtracting(true);
    setExtractError('');
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const [primary, accent] = extractColorsFromImage(img);
        setStyles({ primaryColor: primary, accentColor: accent });
      } catch {
        setExtractError('No s\'han pogut extreure els colors');
      }
      setExtracting(false);
    };
    img.onerror = () => {
      setExtractError('No s\'ha pogut carregar la imatge (CORS?)');
      setExtracting(false);
    };
    img.src = src;
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const url = URL.createObjectURL(file);
    handleExtract(url);
  };

  return (
    <div className="p-3 space-y-5">
      <div className="f-mono text-label uppercase tracking-widest text-ink-3">Estils globals</div>

      {/* Colors del logo */}
      <div className="space-y-2 border border-border-medium rounded p-3">
        <div className="f-mono text-[9px] uppercase text-ink-3 tracking-wider">Importa colors del logo</div>
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="URL del logo..."
            value={logoUrl}
            onChange={(e) => setLogoUrl(e.target.value)}
            className="flex-1 bg-[#0d0d1a] border border-border-medium rounded px-2 h-8 text-xs text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
          />
          <button
            onClick={() => logoUrl.trim() && handleExtract(logoUrl.trim())}
            disabled={!logoUrl.trim() || extracting}
            className="px-3 h-8 text-xs bg-[#FF6B00] text-white rounded disabled:opacity-40"
          >
            {extracting ? '...' : '↓'}
          </button>
        </div>
        <button
          onClick={() => fileRef.current?.click()}
          className="w-full h-8 text-xs border border-border-medium rounded text-ink-2 hover:border-[#FF6B00] hover:text-ink-0 transition"
        >
          {extracting ? 'Extraient...' : 'Puja un fitxer de logo'}
        </button>
        <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
        {extractError && <div className="f-mono text-[9px] text-red-400">{extractError}</div>}
      </div>

      {/* Paletes predefinides */}
      <div className="space-y-2">
        <div className="f-mono text-[9px] uppercase text-ink-3 tracking-wider">Paleta ràpida</div>
        <div className="grid grid-cols-4 gap-1">
          {PALETTES.map((p) => (
            <button
              key={p.name}
              title={p.name}
              onClick={() => setStyles({ primaryColor: p.primary, accentColor: p.accent, bgColor: p.bg, textColor: p.text })}
              className="flex flex-col items-center gap-0.5 p-1.5 rounded border border-border-base hover:border-[#FF6B00] transition group"
            >
              <div className="flex gap-0.5">
                <div className="w-4 h-4 rounded-sm" style={{ background: p.primary }} />
                <div className="w-4 h-4 rounded-sm" style={{ background: p.accent }} />
              </div>
              <span className="f-mono text-[8px] text-ink-3 group-hover:text-ink-1 leading-none">{p.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Colors principals */}
      <div className="space-y-3">
        <div className="f-mono text-[9px] uppercase text-ink-3 tracking-wider">Colors</div>
        {COLOR_FIELDS.map(({ key, label, hint }) => (
          <div key={key}>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">{label}</label>
            <div className="flex gap-2 items-center">
              <input
                type="color"
                value={styles[key] ?? '#000000'}
                onChange={(e) => setStyles({ [key]: e.target.value })}
                className="w-8 h-8 p-0.5 border border-border-base cursor-pointer rounded shrink-0 bg-transparent"
              />
              <input
                type="text"
                value={styles[key] ?? ''}
                onChange={(e) => setStyles({ [key]: e.target.value })}
                className="flex-1 bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 font-mono"
              />
            </div>
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">{hint}</div>
          </div>
        ))}
      </div>

      {/* Tipografies */}
      <div className="space-y-3">
        <div className="f-mono text-[9px] uppercase text-ink-3 tracking-wider">Tipografia</div>

        <div>
          <label className="f-mono text-label uppercase text-ink-2 block mb-1">Títols i encapçalaments</label>
          <select
            value={styles.fontHeading ?? 'Montserrat, sans-serif'}
            onChange={(e) => setStyles({ fontHeading: e.target.value })}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
          >
            {HEADING_FONTS.map((f) => (
              <option key={f.value} value={f.value}>{f.label}</option>
            ))}
          </select>
          <p className="mt-1 f-mono text-[11px] text-ink-1 font-bold" style={{ fontFamily: styles.fontHeading }}>
            Títol d&apos;exemple — ABCDE
          </p>
        </div>

        <div>
          <label className="f-mono text-label uppercase text-ink-2 block mb-1">Text i cossos</label>
          <select
            value={styles.fontBody ?? 'Open Sans, sans-serif'}
            onChange={(e) => setStyles({ fontBody: e.target.value })}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
          >
            {BODY_FONTS.map((f) => (
              <option key={f.value} value={f.value}>{f.label}</option>
            ))}
          </select>
          <p className="mt-1 f-mono text-[10px] text-ink-2" style={{ fontFamily: styles.fontBody }}>
            Text de cos — el lorem ipsum en la tipografia escollida
          </p>
        </div>
      </div>

      {/* Cantonades */}
      <div>
        <label className="f-mono text-label uppercase text-ink-2 block mb-1">Cantonades</label>
        <div className="flex gap-2 items-center">
          <input
            type="range"
            min={0}
            max={24}
            value={parseInt(styles.borderRadius ?? '8') || 0}
            onChange={(e) => setStyles({ borderRadius: `${e.target.value}px` })}
            className="flex-1 accent-[#FF6B00]"
          />
          <span className="f-mono text-label text-ink-1 w-10 text-right">{styles.borderRadius ?? '8px'}</span>
        </div>
      </div>

      {/* Colors avançats */}
      <details className="group">
        <summary className="f-mono text-[9px] uppercase text-ink-3 tracking-wider cursor-pointer list-none flex items-center gap-1">
          <span className="group-open:rotate-90 transition-transform inline-block">▶</span>
          Colors avançats
        </summary>
        <div className="mt-3 space-y-3">
          {(['bgColor', 'textColor'] as const).map((key) => (
            <div key={key}>
              <label className="f-mono text-label uppercase text-ink-3 block mb-1">
                {key === 'bgColor' ? 'Color de fons' : 'Color de text'}
              </label>
              <div className="flex gap-2 items-center">
                <input
                  type="color"
                  value={styles[key] ?? '#ffffff'}
                  onChange={(e) => setStyles({ [key]: e.target.value })}
                  className="w-8 h-8 p-0.5 border border-border-base cursor-pointer rounded shrink-0 bg-transparent"
                />
                <input
                  type="text"
                  value={styles[key] ?? ''}
                  onChange={(e) => setStyles({ [key]: e.target.value })}
                  className="flex-1 bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 font-mono"
                />
              </div>
            </div>
          ))}
        </div>
      </details>

      {/* CSS personalitzat */}
      <details className="group">
        <summary className="f-mono text-[9px] uppercase text-ink-3 tracking-wider cursor-pointer list-none flex items-center gap-1">
          <span className="group-open:rotate-90 transition-transform inline-block">▶</span>
          CSS personalitzat
        </summary>
        <div className="mt-3">
          <textarea
            rows={6}
            placeholder={`.hero h1 { font-size: 4rem; }\n.sec { padding: 60px 0; }`}
            value={styles.customCss ?? ''}
            onChange={(e) => setStyles({ customCss: e.target.value })}
            className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 font-mono resize-y placeholder:text-ink-3"
          />
          <div className="f-mono text-[9px] text-ink-3 mt-1">S'injecta al &lt;head&gt; de la landing publicada. Té accés a les variables CSS: --p, --a, --bg, --tx.</div>
        </div>
      </details>

      {/* Analytics */}
      <details className="group">
        <summary className="f-mono text-[9px] uppercase text-ink-3 tracking-wider cursor-pointer list-none flex items-center gap-1">
          <span className="group-open:rotate-90 transition-transform inline-block">▶</span>
          Analytics &amp; Tracking
        </summary>
        <div className="mt-3 space-y-3">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Google Analytics 4 ID</label>
            <input
              type="text"
              placeholder="G-XXXXXXXXXX"
              value={styles.gaId ?? ''}
              onChange={(e) => setStyles({ gaId: e.target.value })}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3 font-mono"
            />
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">Mesurament ID de la propietat GA4. Injectat automàticament a la landing publicada.</div>
          </div>
        </div>
      </details>

      {/* Contacte ràpid */}
      <details className="group">
        <summary className="f-mono text-[9px] uppercase text-ink-3 tracking-wider cursor-pointer list-none flex items-center gap-1">
          <span className="group-open:rotate-90 transition-transform inline-block">▶</span>
          Contacte ràpid &amp; SEO local
        </summary>
        <div className="mt-3 space-y-3">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">WhatsApp</label>
            <input
              type="tel"
              placeholder="+34 654 048 164"
              value={styles.whatsappNumber ?? ''}
              onChange={(e) => setStyles({ whatsappNumber: e.target.value })}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
            />
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">Apareix com a botó flotant a la landing publicada</div>
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Telèfon</label>
            <input
              type="tel"
              placeholder="+34 971 000 000"
              value={styles.phone ?? ''}
              onChange={(e) => setStyles({ phone: e.target.value })}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
            />
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Adreça</label>
            <input
              type="text"
              placeholder="Carrer Major 1, Palma de Mallorca"
              value={styles.address ?? ''}
              onChange={(e) => setStyles({ address: e.target.value })}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
            />
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">Usada per a Schema.org — millora el SEO local a Google</div>
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Tipus de negoci (Schema.org)</label>
            <select
              value={styles.businessType ?? 'LocalBusiness'}
              onChange={(e) => setStyles({ businessType: e.target.value })}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0"
            >
              {BUSINESS_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </div>
        </div>
      </details>

      {/* Meta SEO */}
      <details className="group">
        <summary className="f-mono text-[9px] uppercase text-ink-3 tracking-wider cursor-pointer list-none flex items-center gap-1">
          <span className="group-open:rotate-90 transition-transform inline-block">▶</span>
          Meta SEO &amp; Xarxes socials
          {metaSaving && <span className="ml-auto text-ink-3 text-[8px]">Guardant...</span>}
        </summary>
        <div className="mt-3 space-y-3">
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">
              Meta description
              <span className="ml-1 text-ink-3 normal-case">{metaDesc.length}/160</span>
            </label>
            <textarea
              rows={3}
              maxLength={160}
              placeholder="Descripció breu que apareix a Google (max 160 caràcters)..."
              value={metaDesc}
              onChange={(e) => setMetaDesc(e.target.value)}
              onBlur={saveMeta}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3 resize-none"
            />
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">Apareix a Google i quan es comparteix a xarxes. Desa automàticament en sortir.</div>
          </div>
          <div>
            <label className="f-mono text-label uppercase text-ink-2 block mb-1">Imatge OG (Open Graph)</label>
            <input
              type="url"
              placeholder="https://..."
              value={ogImage}
              onChange={(e) => setOgImage(e.target.value)}
              onBlur={saveMeta}
              className="w-full bg-[#0d0d1a] border border-border-medium rounded p-2 text-xs text-ink-0 placeholder:text-ink-3"
            />
            <div className="f-mono text-[9px] text-ink-3 mt-0.5">Imatge que apareix quan es comparteix el link a WhatsApp, Facebook, etc. (1200×630px recomanat)</div>
          </div>
        </div>
      </details>
    </div>
  );
};
