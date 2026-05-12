// amg-primitives.jsx — shared icons + small UI atoms + foundation artboards

// ─────────── Icons (Lucide-style, 1.6 stroke) ───────────
const Icon = ({ d, size = 16, stroke = 'currentColor', fill = 'none', children, className = '' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke={stroke}
       strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className={className}>
    {children || <path d={d} />}
  </svg>
);

const I = {
  Dashboard: (p) => <Icon {...p}><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></Icon>,
  Users: (p) => <Icon {...p}><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></Icon>,
  Box: (p) => <Icon {...p}><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><path d="M3.27 6.96 12 12.01l8.73-5.05"/><path d="M12 22.08V12"/></Icon>,
  Receipt: (p) => <Icon {...p}><path d="M4 2v20l2-1.5L8 22l2-1.5L12 22l2-1.5L16 22l2-1.5L20 22V2l-2 1.5L16 2l-2 1.5L12 2l-2 1.5L8 2 6 3.5 4 2z"/><path d="M8 7h8M8 11h8M8 15h5"/></Icon>,
  Settings: (p) => <Icon {...p}><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33h.01a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h.01a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.01a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></Icon>,
  Plus: (p) => <Icon {...p}><path d="M12 5v14M5 12h14"/></Icon>,
  Search: (p) => <Icon {...p}><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></Icon>,
  Bell: (p) => <Icon {...p}><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></Icon>,
  Chevron: (p) => <Icon {...p}><path d="m9 18 6-6-6-6"/></Icon>,
  ChevDown: (p) => <Icon {...p}><path d="m6 9 6 6 6-6"/></Icon>,
  Menu: (p) => <Icon {...p}><path d="M3 12h18M3 6h18M3 18h18"/></Icon>,
  X: (p) => <Icon {...p}><path d="M18 6 6 18M6 6l12 12"/></Icon>,
  Check: (p) => <Icon {...p}><path d="M20 6 9 17l-5-5"/></Icon>,
  Download: (p) => <Icon {...p}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="M7 10l5 5 5-5"/><path d="M12 15V3"/></Icon>,
  Edit: (p) => <Icon {...p}><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></Icon>,
  Trash: (p) => <Icon {...p}><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></Icon>,
  More: (p) => <Icon {...p}><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></Icon>,
  Bot: (p) => <Icon {...p}><rect x="3" y="8" width="18" height="12" rx="2"/><path d="M12 8V4M8 4h8"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></Icon>,
  Zap: (p) => <Icon {...p}><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></Icon>,
  Globe: (p) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></Icon>,
  Mail: (p) => <Icon {...p}><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></Icon>,
  Lock: (p) => <Icon {...p}><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></Icon>,
  Building: (p) => <Icon {...p}><rect x="4" y="2" width="16" height="20" rx="1"/><path d="M9 22v-4h6v4M8 6h.01M12 6h.01M16 6h.01M8 10h.01M12 10h.01M16 10h.01M8 14h.01M12 14h.01M16 14h.01"/></Icon>,
  CreditCard: (p) => <Icon {...p}><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/></Icon>,
  ArrowUp: (p) => <Icon {...p}><path d="m18 15-6-6-6 6"/></Icon>,
  ArrowRight: (p) => <Icon {...p}><path d="M5 12h14M13 5l7 7-7 7"/></Icon>,
  Terminal: (p) => <Icon {...p}><path d="m4 17 6-6-6-6M12 19h8"/></Icon>,
  Layers: (p) => <Icon {...p}><path d="m12 2 10 6-10 6L2 8l10-6z"/><path d="m2 17 10 6 10-6M2 12l10 6 10-6"/></Icon>,
  Sparkles: (p) => <Icon {...p}><path d="m12 3-1.5 4.5L6 9l4.5 1.5L12 15l1.5-4.5L18 9l-4.5-1.5L12 3zM19 14l-.75 2.25L16 17l2.25.75L19 20l.75-2.25L22 17l-2.25-.75L19 14zM5 16l-.5 1.5L3 18l1.5.5L5 20l.5-1.5L7 18l-1.5-.5L5 16z"/></Icon>,
  Calendar: (p) => <Icon {...p}><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></Icon>,
  Filter: (p) => <Icon {...p}><path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z"/></Icon>,
  Upload: (p) => <Icon {...p}><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/></Icon>,
  Link: (p) => <Icon {...p}><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></Icon>,
  Image: (p) => <Icon {...p}><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.09-3.09a2 2 0 0 0-2.82 0L6 21"/></Icon>,
  Shield: (p) => <Icon {...p}><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></Icon>,
  Smartphone: (p) => <Icon {...p}><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></Icon>,
  Trending: (p) => <Icon {...p}><path d="m23 6-9.5 9.5-5-5L1 18"/><path d="M17 6h6v6"/></Icon>,
  Activity: (p) => <Icon {...p}><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></Icon>,
  Clock: (p) => <Icon {...p}><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></Icon>,
  Play: (p) => <Icon {...p}><path d="M5 3v18l15-9z"/></Icon>,
};

// ─────────── Primitives used across screens ───────────

const AMGButton = ({ variant = 'primary', size = 'md', icon: Ico, children, className = '', onClick }) => {
  const base = 'f-mono uppercase inline-flex items-center gap-2 font-semibold transition-all select-none';
  const sizes = { sm: 'px-3 h-8 text-[11px]', md: 'px-5 h-10 text-xs', lg: 'px-7 h-12 text-sm' };
  const variants = {
    primary: 'btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black',
    secondary: 'btn-clip bg-[#1a1a2e] hover:bg-[#212140] text-[#e2e8f0] border border-[rgba(255,107,0,0.35)]',
    ghost: 'text-[#e2e8f0] hover:text-[#FF9A3C] hover:bg-[rgba(255,107,0,0.08)]',
    danger: 'btn-clip bg-[#ff4444] hover:bg-[#ff6666] text-black',
    outline: 'btn-clip bg-transparent text-[#FF9A3C] border border-[#FF6B00]/60 hover:bg-[#FF6B00]/10',
  };
  return (
    <button onClick={onClick} className={`${base} ${sizes[size]} ${variants[variant]} ${className}`}>
      {Ico && <Ico size={size === 'sm' ? 12 : 14} />}
      {children}
    </button>
  );
};

const AMGBadge = ({ tone = 'neutral', mono = true, children, className = '' }) => {
  const tones = {
    neutral: 'bg-[#212140] text-[#94a3b8] border border-[rgba(226,232,240,0.1)]',
    accent: 'bg-[rgba(255,107,0,0.12)] text-[#FF9A3C] border border-[rgba(255,107,0,0.35)]',
    success: 'bg-[rgba(57,211,83,0.12)] text-[#39d353] border border-[rgba(57,211,83,0.35)]',
    danger: 'bg-[rgba(255,68,68,0.12)] text-[#ff6666] border border-[rgba(255,68,68,0.35)]',
    info: 'bg-[rgba(88,166,255,0.12)] text-[#58a6ff] border border-[rgba(88,166,255,0.35)]',
    warning: 'bg-[rgba(240,180,41,0.12)] text-[#f0b429] border border-[rgba(240,180,41,0.35)]',
  };
  return (
    <span className={`${mono ? 'f-mono' : ''} inline-flex items-center gap-1.5 px-2 h-[22px] text-[10px] uppercase font-semibold tracking-wider ${tones[tone]} ${className}`}>
      {children}
    </span>
  );
};

const AMGInput = ({ label, placeholder, value, mono, icon: Ico, hint, error, className = '' }) => (
  <label className={`block ${className}`}>
    {label && <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">{label}</span>}
    <div className={`relative flex items-center h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus-within:border-[#FF6B00] transition`}>
      {Ico && <div className="pl-3 text-[#64748b]"><Ico size={14}/></div>}
      <input type="text" defaultValue={value} placeholder={placeholder}
             className={`${mono ? 'f-mono' : ''} flex-1 bg-transparent outline-none px-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b]`} />
    </div>
    {hint && <span className="block mt-1 text-[11px] text-[#64748b]">{hint}</span>}
    {error && <span className="block mt-1 text-[11px] text-[#ff6666]">{error}</span>}
  </label>
);

const AMGStat = ({ label, value, delta, tone = 'accent', icon: Ico }) => {
  const colors = {
    accent: 'text-[#FF9A3C]',
    success: 'text-[#39d353]',
    danger: 'text-[#ff6666]',
    info: 'text-[#58a6ff]',
  };
  return (
    <div className="card-clip amg-card p-5 relative overflow-hidden" style={{'--pl':'20px'}}>
      <div className="flex items-start justify-between">
        <span className="f-mono uppercase text-[10px] tracking-[0.18em] text-[#64748b]">{label}</span>
        {Ico && <div className={`${colors[tone]} opacity-80`}><Ico size={16}/></div>}
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className={`f-display font-bold text-3xl ${colors[tone]}`}>{value}</span>
      </div>
      {delta && (
        <div className="mt-2 flex items-center gap-1.5 text-[11px]">
          <span className={delta.startsWith('+') ? 'text-[#39d353]' : 'text-[#ff6666]'}>{delta}</span>
          <span className="f-mono text-[#64748b] uppercase tracking-wider">vs mes anterior</span>
        </div>
      )}
      <div className="absolute top-0 right-0 w-[2px] h-6 bg-[#FF6B00]"></div>
    </div>
  );
};

const AMGSectionTitle = ({ eyebrow, title, children }) => (
  <div className="flex items-end justify-between mb-4">
    <div>
      {eyebrow && <span className="f-mono uppercase text-[10px] tracking-[0.2em] text-[#FF9A3C]">{eyebrow}</span>}
      <h3 className="f-display text-lg font-bold tracking-wider text-[#e2e8f0] mt-1">{title}</h3>
    </div>
    {children}
  </div>
);

// ─────────── Foundation artboards ───────────

function AMGColorSystem() {
  const tokens = [
    { name: 'accent', hex: '#FF6B00', use: 'CTAs · bordes activos · acentos' },
    { name: 'accent-light', hex: '#FF9A3C', use: 'Hover · gradientes · énfasis' },
    { name: 'bg-0', hex: '#0d0d1a', use: 'Fondo base de la app' },
    { name: 'bg-1', hex: '#13132a', use: 'Cards · sidebar · superficie' },
    { name: 'bg-2', hex: '#1a1a2e', use: 'Inputs · superficie elevada' },
    { name: 'bg-3', hex: '#212140', use: 'Hover states · pills' },
  ];
  const semantic = [
    { name: 'success', hex: '#39d353', use: 'OK · pagado · activo' },
    { name: 'danger', hex: '#ff4444', use: 'Error · cancelado · crítico' },
    { name: 'info', hex: '#58a6ff', use: 'Información · badges' },
    { name: 'warning', hex: '#f0b429', use: 'Suspendido · pendiente' },
  ];
  const text = [
    { name: 'ink-0', hex: '#e2e8f0', use: 'Texto principal' },
    { name: 'ink-1', hex: '#94a3b8', use: 'Labels · subtítulos' },
    { name: 'ink-2', hex: '#64748b', use: 'Secundario · hints' },
  ];
  return (
    <div className="amg amg-grid w-full h-full p-10 overflow-auto">
      <div className="mb-6">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">01 · Color tokens</span>
        <h2 className="f-display text-3xl font-bold mt-2">PALETA</h2>
        <p className="text-[#94a3b8] text-sm mt-1 max-w-xl">Dark mode obligatorio. Naranja como único acento cromático — todo lo demás son neutros fríos con toques violeta.</p>
      </div>
      <div className="grid grid-cols-3 gap-3 mb-6">
        {tokens.map(t => (
          <div key={t.name} className="amg-card card-clip p-4">
            <div className="h-16 w-full mb-3 border border-[rgba(226,232,240,0.08)]" style={{background:t.hex}}></div>
            <div className="f-display font-bold text-sm">{t.name}</div>
            <div className="f-mono text-[11px] text-[#FF9A3C] mt-0.5">{t.hex}</div>
            <div className="text-[12px] text-[#94a3b8] mt-1.5">{t.use}</div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-4 gap-3 mb-6">
        {semantic.map(t => (
          <div key={t.name} className="amg-card card-clip p-3">
            <div className="h-10 w-full mb-2" style={{background:t.hex}}></div>
            <div className="f-display font-bold text-xs">{t.name}</div>
            <div className="f-mono text-[10px] text-[#94a3b8]">{t.hex}</div>
            <div className="text-[11px] text-[#94a3b8] mt-1">{t.use}</div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-3 gap-3">
        {text.map(t => (
          <div key={t.name} className="amg-card card-clip p-4 flex items-center gap-4">
            <div className="w-12 h-12 border border-[rgba(226,232,240,0.08)]" style={{background:t.hex}}></div>
            <div>
              <div className="f-display font-bold text-sm">{t.name}</div>
              <div className="f-mono text-[11px] text-[#FF9A3C]">{t.hex}</div>
              <div className="text-[12px] text-[#94a3b8]">{t.use}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function AMGTypeSystem() {
  return (
    <div className="amg amg-grid w-full h-full p-10 overflow-auto">
      <div className="mb-6">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">02 · Typography</span>
        <h2 className="f-display text-3xl font-bold mt-2">TIPOGRAFÍA</h2>
      </div>
      <div className="space-y-5">
        <div className="amg-card card-clip p-6">
          <div className="flex items-baseline justify-between mb-3">
            <span className="f-mono text-[11px] text-[#FF9A3C] uppercase tracking-wider">Orbitron · Display</span>
            <span className="f-mono text-[11px] text-[#64748b]">500 · 700 · 900</span>
          </div>
          <div className="f-display font-black text-5xl">AMG ENGINYERIA</div>
          <div className="f-display font-bold text-3xl mt-2 text-[#FF9A3C]">Digital · Futuro presente</div>
          <div className="f-display font-medium text-xl mt-2 text-[#94a3b8]">Section headings · marca · títulos</div>
        </div>

        <div className="amg-card card-clip p-6">
          <div className="flex items-baseline justify-between mb-3">
            <span className="f-mono text-[11px] text-[#FF9A3C] uppercase tracking-wider">Rajdhani · Body</span>
            <span className="f-mono text-[11px] text-[#64748b]">300 · 400 · 500 · 600 · 700</span>
          </div>
          <p className="text-xl font-light mb-2">Plataforma SaaS B2B que automatiza la presencia digital de pymes.</p>
          <p className="text-base mb-2">WhatsApp con IA, landings generadas automáticamente, workflows de trabajo y facturación — todo en un solo portal administrado.</p>
          <p className="text-sm text-[#94a3b8]">Rajdhani cubre cuerpos de texto, navegación, forms y párrafos largos. Su condensación vertical permite densidad sin sacrificar legibilidad.</p>
        </div>

        <div className="amg-card card-clip p-6">
          <div className="flex items-baseline justify-between mb-3">
            <span className="f-mono text-[11px] text-[#FF9A3C] uppercase tracking-wider">Share Tech Mono · Labels</span>
            <span className="f-mono text-[11px] text-[#64748b]">400</span>
          </div>
          <div className="f-mono text-lg mb-1">SUBSCRIPTION_ID · c7f3_a104</div>
          <div className="f-mono text-sm text-[#94a3b8]">STATUS: ACTIVE  ·  MRR: €1.240  ·  NEXT: 2026-05-12</div>
          <div className="f-mono text-xs text-[#64748b] mt-2">Utilizado en labels, badges técnicos, IDs, códigos y fechas.</div>
        </div>

        <div className="grid grid-cols-4 gap-2">
          {[
            ['H1', 'f-display text-3xl font-black'],
            ['H2', 'f-display text-2xl font-bold'],
            ['H3', 'f-display text-lg font-bold'],
            ['Body', 'text-base'],
            ['Body sm', 'text-sm text-[#94a3b8]'],
            ['Caption', 'text-xs text-[#64748b]'],
            ['Label', 'f-mono text-[11px] uppercase tracking-[0.18em] text-[#FF9A3C]'],
            ['Code', 'f-mono text-[11px] text-[#e2e8f0]'],
          ].map(([n, c]) => (
            <div key={n} className="amg-card card-clip p-3">
              <div className="f-mono text-[9px] uppercase text-[#64748b] mb-1">{n}</div>
              <div className={c}>Sample</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function AMGSpacingSystem() {
  const icons = ['Dashboard','Users','Box','Receipt','Settings','Bell','Bot','Zap','Globe','Mail','Lock','Shield','Activity','Trending','Calendar','Link'];
  return (
    <div className="amg amg-grid w-full h-full p-10 overflow-auto">
      <div className="mb-6">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">03 · Spacing, radii, icons</span>
        <h2 className="f-display text-3xl font-bold mt-2">SISTEMA VISUAL</h2>
      </div>

      <div className="grid grid-cols-2 gap-5">
        <div className="amg-card card-clip p-6">
          <div className="f-mono text-[11px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Spacing scale · 4px base</div>
          <div className="space-y-2">
            {[['1','4px'],['2','8px'],['3','12px'],['4','16px'],['5','20px'],['6','24px'],['8','32px'],['10','40px']].map(([n,v]) => (
              <div key={n} className="flex items-center gap-3">
                <span className="f-mono w-10 text-xs text-[#94a3b8]">{n}</span>
                <div className="h-2 bg-[#FF6B00]" style={{width: v}}></div>
                <span className="f-mono text-[11px] text-[#64748b]">{v}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="amg-card card-clip p-6">
          <div className="f-mono text-[11px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Corner treatment · clip-path</div>
          <p className="text-xs text-[#94a3b8] mb-4">Sin border-radius. Los contenedores primarios usan clip-path con esquina cortada a 45° para reforzar el estilo angular cíber.</p>
          <div className="grid grid-cols-3 gap-3">
            <div className="aspect-square bg-[#FF6B00] btn-clip flex items-center justify-center text-black f-mono text-[10px]">BTN · 10px</div>
            <div className="aspect-square amg-card card-clip flex items-center justify-center f-mono text-[10px] text-[#FF9A3C]">CARD · 14px</div>
            <div className="aspect-square bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex items-center justify-center f-mono text-[10px] text-[#FF9A3C]">INPUT · 0</div>
          </div>
        </div>

        <div className="amg-card card-clip p-6 col-span-2">
          <div className="flex items-center justify-between mb-3">
            <div className="f-mono text-[11px] uppercase tracking-[0.18em] text-[#FF9A3C]">Iconography · Lucide · 1.6 stroke</div>
            <span className="f-mono text-[11px] text-[#64748b]">16 · 20 · 24 sizes</span>
          </div>
          <div className="grid grid-cols-8 gap-2">
            {icons.map(k => {
              const C = I[k];
              return (
                <div key={k} className="aspect-square bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex flex-col items-center justify-center gap-1 p-2">
                  <C size={20} stroke="#FF9A3C"/>
                  <span className="f-mono text-[9px] text-[#64748b] uppercase">{k}</span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="amg-card card-clip p-6 col-span-2">
          <div className="f-mono text-[11px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Grid decoration</div>
          <div className="amg-grid h-28 flex items-center justify-center">
            <span className="f-mono text-xs text-[#94a3b8]">Líneas naranja · 4% opacidad · 40px grid en superficies principales</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────── Component sheet artboards ───────────

function AMGButtonSheet() {
  return (
    <div className="amg amg-grid w-full h-full p-8 overflow-auto">
      <div className="mb-5">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">Buttons</span>
        <h2 className="f-display text-2xl font-bold mt-1">BOTONES</h2>
      </div>

      <div className="space-y-4">
        <div className="amg-card card-clip p-5">
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-3">Primary · clip corner</div>
          <div className="flex flex-wrap gap-2 items-center">
            <AMGButton size="sm">ACTION SM</AMGButton>
            <AMGButton>CREAR CLIENTE</AMGButton>
            <AMGButton size="lg" icon={I.Plus}>NUEVA FACTURA</AMGButton>
            <AMGButton icon={I.ArrowRight}>CONTINUAR</AMGButton>
          </div>
        </div>

        <div className="amg-card card-clip p-5">
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-3">Secondary / Outline / Ghost</div>
          <div className="flex flex-wrap gap-2 items-center">
            <AMGButton variant="secondary">CANCELAR</AMGButton>
            <AMGButton variant="outline" icon={I.Download}>EXPORTAR</AMGButton>
            <AMGButton variant="ghost">SALIR</AMGButton>
            <AMGButton variant="danger" icon={I.Trash}>ELIMINAR</AMGButton>
          </div>
        </div>

        <div className="amg-card card-clip p-5">
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-3">Icon-only & toggles</div>
          <div className="flex gap-2 items-center">
            <button className="w-10 h-10 btn-clip bg-[#FF6B00] text-black flex items-center justify-center"><I.Plus size={16}/></button>
            <button className="w-10 h-10 btn-clip bg-[#1a1a2e] border border-[rgba(255,107,0,0.35)] text-[#FF9A3C] flex items-center justify-center"><I.Edit size={14}/></button>
            <button className="w-10 h-10 btn-clip bg-[#1a1a2e] border border-[rgba(226,232,240,0.1)] text-[#94a3b8] flex items-center justify-center"><I.More size={14}/></button>
            <div className="h-6 w-[1px] bg-[rgba(226,232,240,0.1)] mx-2"></div>
            <div className="relative inline-flex h-6 w-11 items-center bg-[#FF6B00] rounded-full">
              <span className="ml-5 block h-5 w-5 rounded-full bg-black"></span>
            </div>
            <span className="f-mono text-xs text-[#94a3b8]">ACTIVO</span>
            <div className="relative inline-flex h-6 w-11 items-center bg-[#212140] rounded-full ml-3">
              <span className="ml-0.5 block h-5 w-5 rounded-full bg-[#64748b]"></span>
            </div>
            <span className="f-mono text-xs text-[#64748b]">INACTIVO</span>
          </div>
        </div>

        <div className="amg-card card-clip p-5">
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-3">States</div>
          <div className="flex flex-wrap gap-2 items-center">
            <AMGButton>DEFAULT</AMGButton>
            <AMGButton className="bg-[#FF9A3C]">HOVER</AMGButton>
            <button className="btn-clip bg-[#FF6B00]/40 text-black/60 h-10 px-5 f-mono text-xs uppercase font-semibold cursor-not-allowed">DISABLED</button>
            <button className="btn-clip bg-[#FF6B00] text-black h-10 px-5 f-mono text-xs uppercase font-semibold inline-flex items-center gap-2">
              <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin"></span>
              CARGANDO
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function AMGInputSheet() {
  return (
    <div className="amg amg-grid w-full h-full p-8 overflow-auto">
      <div className="mb-5">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">Forms</span>
        <h2 className="f-display text-2xl font-bold mt-1">INPUTS & FORMS</h2>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <AMGInput label="Email corporativo" placeholder="tu@empresa.com" icon={I.Mail} hint="Usaremos este correo para el magic link"/>
        <AMGInput label="Dominio personalizado" placeholder="tucliente.com" icon={I.Globe} />
        <AMGInput label="Password" placeholder="••••••••" icon={I.Lock} error="La contraseña debe tener 8+ caracteres"/>
        <AMGInput label="Client ID" value="amg_7f3c_a104b" mono icon={I.Terminal}/>

        <div className="col-span-2">
          <label className="block">
            <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">Descripción</span>
            <textarea rows="3" className="w-full bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus:border-[#FF6B00] outline-none p-3 text-sm text-[#e2e8f0] resize-none"
                      placeholder="Resumen breve del cliente…"/>
          </label>
        </div>

        <div>
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">Plan</span>
          <div className="flex gap-0 border border-[rgba(255,107,0,0.2)]">
            {['STARTER','GROWTH','SCALE'].map((p,i) => (
              <button key={p} className={`flex-1 h-10 f-mono text-xs uppercase tracking-wider transition ${i===1 ? 'bg-[#FF6B00] text-black' : 'bg-[#1a1a2e] text-[#94a3b8] hover:text-[#FF9A3C]'}`}>{p}</button>
            ))}
          </div>
        </div>

        <div>
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">Estado</span>
          <div className="relative h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] flex items-center px-3">
            <span className="text-sm flex-1 f-mono">ACTIVO</span>
            <I.ChevDown size={14} className="text-[#64748b]"/>
          </div>
        </div>

        <div className="col-span-2 amg-card card-clip p-5">
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-3">Checkbox & radio</div>
          <div className="flex flex-wrap gap-5 items-center">
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <span className="w-4 h-4 bg-[#FF6B00] flex items-center justify-center"><I.Check size={10} stroke="#000"/></span>
              <span className="text-sm">Aceptar condiciones</span>
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <span className="w-4 h-4 border border-[rgba(255,107,0,0.35)] bg-[#1a1a2e]"></span>
              <span className="text-sm text-[#94a3b8]">Newsletter técnico</span>
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <span className="w-4 h-4 rounded-full border-2 border-[#FF6B00] flex items-center justify-center"><span className="w-2 h-2 rounded-full bg-[#FF6B00]"></span></span>
              <span className="text-sm">Facturación mensual</span>
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <span className="w-4 h-4 rounded-full border-2 border-[rgba(226,232,240,0.2)]"></span>
              <span className="text-sm text-[#94a3b8]">Facturación anual · −15%</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  );
}

function AMGCardSheet() {
  return (
    <div className="amg amg-grid w-full h-full p-8 overflow-auto">
      <div className="mb-5">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">Cards · Badges · Tabs</span>
        <h2 className="f-display text-2xl font-bold mt-1">CONTENEDORES</h2>
      </div>

      <div className="space-y-5">
        <div>
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Badges · estados</div>
          <div className="flex flex-wrap gap-2">
            <AMGBadge tone="success"><span className="w-1.5 h-1.5 rounded-full bg-[#39d353]"></span>ACTIVO</AMGBadge>
            <AMGBadge tone="warning">SUSPENDIDO</AMGBadge>
            <AMGBadge tone="danger">CANCELADO</AMGBadge>
            <AMGBadge tone="accent">PAID</AMGBadge>
            <AMGBadge tone="warning">PENDING</AMGBadge>
            <AMGBadge tone="neutral">DRAFT</AMGBadge>
            <AMGBadge tone="info">INFO</AMGBadge>
            <AMGBadge tone="accent">ADMIN</AMGBadge>
            <AMGBadge tone="success">v2.14.0</AMGBadge>
          </div>
        </div>

        <div>
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Tabs</div>
          <div className="flex gap-0 border-b border-[rgba(255,107,0,0.14)]">
            {['Datos','Servicios','Landing','Credenciales','Facturación'].map((t,i) => (
              <button key={t} className={`px-4 h-10 f-mono text-xs uppercase tracking-wider relative ${i===0 ? 'text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>
                {t}
                {i===0 && <span className="absolute bottom-[-1px] left-0 right-0 h-[2px] bg-[#FF6B00]"></span>}
              </button>
            ))}
          </div>
        </div>

        <div>
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Cards</div>
          <div className="grid grid-cols-3 gap-3">
            <AMGStat label="MRR" value="€12.4K" delta="+8.2%" tone="accent" icon={I.Trending}/>
            <AMGStat label="Clientes" value="48" delta="+3" tone="success" icon={I.Users}/>
            <AMGStat label="Pendientes" value="7" delta="−2" tone="info" icon={I.Receipt}/>
          </div>
        </div>

        <div>
          <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Feature card</div>
          <div className="grid grid-cols-2 gap-3">
            <div className="amg-card card-clip p-5 group hover:border-l-2 hover:border-l-[#FF6B00] transition">
              <div className="w-10 h-10 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center mb-3">
                <I.Bot size={18} stroke="#FF9A3C"/>
              </div>
              <div className="f-display font-bold text-base">WHATSAPP BOT AI</div>
              <div className="text-[13px] text-[#94a3b8] mt-1">Respuestas inteligentes 24/7 entrenadas con tu documentación.</div>
              <div className="flex items-baseline gap-2 mt-3 f-mono">
                <span className="text-[#FF9A3C] text-lg">€49</span>
                <span className="text-[10px] text-[#64748b] uppercase">/mes · €149 setup</span>
              </div>
            </div>
            <div className="amg-card card-clip p-5 border-l-2 border-l-[#FF6B00]">
              <div className="w-10 h-10 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center mb-3">
                <I.Zap size={18} stroke="#FF9A3C"/>
              </div>
              <div className="f-display font-bold text-base">WORKFLOW AUTOMATION</div>
              <div className="text-[13px] text-[#94a3b8] mt-1">Conecta Calendar, Stripe, Drive y Notion sin escribir código.</div>
              <div className="flex items-baseline gap-2 mt-3 f-mono">
                <span className="text-[#FF9A3C] text-lg">€29</span>
                <span className="text-[10px] text-[#64748b] uppercase">/mes</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function AMGAlertSheet() {
  return (
    <div className="amg amg-grid w-full h-full p-8 overflow-auto">
      <div className="mb-5">
        <span className="f-mono uppercase text-[11px] tracking-[0.2em] text-[#FF9A3C]">Feedback</span>
        <h2 className="f-display text-2xl font-bold mt-1">ALERTS · TOASTS · MODAL</h2>
      </div>

      <div className="space-y-3 mb-5">
        {[
          { tone:'success', icon:I.Check, title:'Cliente creado', msg:'Nebula Studio ha sido añadido con el plan GROWTH.' },
          { tone:'warning', icon:I.Bell, title:'Factura pendiente', msg:'Tienes 3 facturas vencidas hace más de 15 días.' },
          { tone:'danger', icon:I.X, title:'Error al conectar OAuth', msg:'No pudimos verificar las credenciales de Google Calendar.' },
          { tone:'info', icon:I.Sparkles, title:'Nueva funcionalidad', msg:'El editor de landings ahora soporta componentes custom.' },
        ].map((a,i) => {
          const colors = {
            success: { bar:'#39d353', bg:'rgba(57,211,83,0.08)', text:'#39d353' },
            warning: { bar:'#f0b429', bg:'rgba(240,180,41,0.08)', text:'#f0b429' },
            danger:  { bar:'#ff4444', bg:'rgba(255,68,68,0.08)', text:'#ff6666' },
            info:    { bar:'#58a6ff', bg:'rgba(88,166,255,0.08)', text:'#58a6ff' },
          }[a.tone];
          return (
            <div key={i} className="flex gap-4 p-4 border border-[rgba(226,232,240,0.08)]" style={{background: colors.bg}}>
              <div className="w-1 self-stretch" style={{background: colors.bar}}></div>
              <div className="flex gap-3 flex-1">
                <div className="w-8 h-8 flex items-center justify-center" style={{color: colors.text}}><a.icon size={16}/></div>
                <div className="flex-1">
                  <div className="f-display font-bold text-sm" style={{color: colors.text}}>{a.title}</div>
                  <div className="text-[13px] text-[#94a3b8]">{a.msg}</div>
                </div>
                <button className="text-[#64748b] hover:text-[#e2e8f0]"><I.X size={14}/></button>
              </div>
            </div>
          );
        })}
      </div>

      <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Modal (confirmación)</div>
      <div className="relative h-56 bg-black/40 border border-[rgba(226,232,240,0.05)] flex items-center justify-center">
        <div className="amg-card card-clip w-[420px] p-6">
          <div className="flex items-start gap-4">
            <div className="w-10 h-10 bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] flex items-center justify-center text-[#ff6666]">
              <I.Trash size={16}/>
            </div>
            <div className="flex-1">
              <div className="f-display font-bold text-base">ELIMINAR CLIENTE</div>
              <p className="text-[13px] text-[#94a3b8] mt-1">Vas a eliminar <span className="f-mono text-[#FF9A3C]">nebula-studio</span>. Esta acción no se puede deshacer.</p>
              <div className="flex justify-end gap-2 mt-5">
                <AMGButton variant="secondary" size="sm">CANCELAR</AMGButton>
                <AMGButton variant="danger" size="sm">ELIMINAR</AMGButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  I, Icon,
  AMGButton, AMGBadge, AMGInput, AMGStat, AMGSectionTitle,
  AMGColorSystem, AMGTypeSystem, AMGSpacingSystem,
  AMGButtonSheet, AMGInputSheet, AMGCardSheet, AMGAlertSheet,
});
