// amg-v2.jsx — Phase-based admin (Prospecting · Budgets · Clients & Audit · Content Factory · Vault) + conversion landing

// ─────────── Phase-based sidebar ───────────
function AMGSidebarV2({ active = 'prospecting' }) {
  const items = [
    { id:'dashboard', label:'Dashboard', icon:I.Dashboard },
    { id:'prospecting', label:'Prospecting', icon:I.Search, badge:'12', tone:'accent' },
    { id:'budgets', label:'Pressupostos', icon:I.Receipt, badge:'4' },
    { id:'clients', label:'Clients & Audit', icon:I.Users, badge:'48' },
    { id:'factory', label:'Content Factory', icon:I.Layers },
    { id:'vault', label:'Vault', icon:I.Shield },
    { id:'invoices', label:'Factures', icon:I.CreditCard },
    { id:'settings', label:'Configuració', icon:I.Settings },
  ];
  return (
    <aside className="w-[240px] shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex flex-col">
      <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-5 gap-3">
        <div className="w-9 h-9 bg-[#FF6B00] btn-clip flex items-center justify-center">
          <span className="f-display font-black text-black text-sm">A</span>
        </div>
        <div className="flex flex-col leading-tight">
          <span className="f-display font-black text-sm tracking-[0.15em]">AMG</span>
          <span className="f-mono text-[9px] text-[#FF9A3C] tracking-[0.2em]">PORTAL · BACKOFFICE</span>
        </div>
      </div>
      <nav className="flex-1 p-3 space-y-1">
        <div className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#64748b] px-3 py-2">Cicle de vida</div>
        {items.map(it => {
          const isActive = it.id === active;
          return (
            <a key={it.id} className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider transition cursor-pointer
                 ${isActive ? 'bg-[rgba(255,107,0,0.10)] text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0] hover:bg-[rgba(255,255,255,0.03)]'}`}>
              {isActive && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
              <it.icon size={16}/>
              <span className="flex-1">{it.label}</span>
              {it.badge && (
                <span className={`f-mono text-[9px] px-1.5 h-4 flex items-center ${it.tone==='accent' ? 'bg-[rgba(255,107,0,0.18)] text-[#FF9A3C]' : 'bg-[#212140] text-[#94a3b8]'}`}>
                  {it.badge}
                </span>
              )}
            </a>
          );
        })}
      </nav>
      <div className="border-t border-[rgba(255,107,0,0.12)] p-4 flex items-center gap-3">
        <div className="w-9 h-9 bg-gradient-to-br from-[#FF6B00] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">AM</div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-semibold truncate">Antonio Mas</div>
          <div className="f-mono text-[10px] text-[#FF9A3C] truncate">FOUNDER</div>
        </div>
      </div>
    </aside>
  );
}

function AMGTopbarV2({ title, subtitle, children }) {
  return (
    <div className="h-16 shrink-0 border-b border-[rgba(255,107,0,0.12)] flex items-center px-8 gap-5 bg-[#0d0d1a]/80">
      <div className="flex-1">
        <div className="f-mono text-[10px] uppercase text-[#FF9A3C] tracking-[0.2em]">/ {title}</div>
        {subtitle && <div className="f-display font-bold text-lg leading-tight mt-0.5">{subtitle}</div>}
      </div>
      {children}
    </div>
  );
}

// ─────────── 01 · Prospecting ───────────
function AMGProspecting() {
  const leads = [
    { name:'Fontaneria Roca', sector:'Lampisteria', city:'Mataró', score:92, status:'HOT', signals:['Sense web pròpia','3 ressenyes negatives WhatsApp','Anuncis Google actius'], wa:'+34 678 234 ...' },
    { name:'Fisio Llevant', sector:'Fisioteràpia', city:'Cabrils', score:88, status:'HOT', signals:['Booking manual','Web obsoleta (2017)','Instagram actiu'], wa:'+34 692 110 ...' },
    { name:'Tallers Vidal', sector:'Mecànic', city:'Premià', score:74, status:'WARM', signals:['Excel facturació','Web bàsica','Fb actiu'], wa:'+34 661 445 ...' },
    { name:'Estètica Núria', sector:'Bellesa', city:'Argentona', score:71, status:'WARM', signals:['Cita per WhatsApp manual','3 empleades'], wa:'+34 654 998 ...' },
    { name:'Forn Can Bigues', sector:'Forn', city:'Vilassar', score:58, status:'COLD', signals:['Negoci familiar','Sense presència digital'], wa:'+34 637 552 ...' },
    { name:'Clínica Dental Vila', sector:'Dental', city:'Premià', score:81, status:'HOT', signals:['Software antic','Recordatoris manuals','5 doctores'], wa:'+34 688 770 ...' },
  ];
  const tone = (s) => s==='HOT' ? 'danger' : s==='WARM' ? 'warning' : 'neutral';
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="prospecting"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="prospecting" subtitle="Leads detectats per IA · Llevant area">
          <AMGButton variant="outline" size="sm" icon={I.Filter}>FILTRES</AMGButton>
          <AMGButton size="sm" icon={I.Sparkles}>EXECUTAR ESCANEIG IA</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-auto amg-grid p-8 space-y-5">
          <div className="grid grid-cols-4 gap-3">
            <AMGStat label="Leads detectats (7d)" value="34" delta="+12" tone="accent" icon={I.Sparkles}/>
            <AMGStat label="Score ≥ 80 · HOT" value="8" tone="danger" icon={I.Zap}/>
            <AMGStat label="Demos provisionades" value="3" tone="success" icon={I.Check}/>
            <AMGStat label="Conversió mitjana" value="42%" delta="+5%" tone="info" icon={I.Trending}/>
          </div>

          <div className="grid grid-cols-[1fr_360px] gap-4">
            <div className="amg-card card-clip overflow-hidden">
              <div className="grid grid-cols-[1.4fr_120px_80px_120px_140px] gap-3 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
                <div>Lead</div><div>Sector</div><div>Score</div><div>Estat</div><div className="text-right">Acció</div>
              </div>
              {leads.map((l,i) => (
                <div key={i} className="grid grid-cols-[1.4fr_120px_80px_120px_140px] gap-3 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)]">
                  <div>
                    <div className="text-sm font-semibold">{l.name}</div>
                    <div className="f-mono text-[10px] text-[#64748b]">{l.city} · {l.wa}</div>
                  </div>
                  <span className="f-mono text-xs text-[#94a3b8]">{l.sector}</span>
                  <div className="flex items-center gap-1.5">
                    <div className="w-8 h-1.5 bg-[#212140]"><div className="h-full bg-[#FF6B00]" style={{width:`${l.score}%`}}></div></div>
                    <span className="f-mono text-xs text-[#FF9A3C]">{l.score}</span>
                  </div>
                  <AMGBadge tone={tone(l.status)}>{l.status}</AMGBadge>
                  <div className="flex justify-end">
                    <AMGButton size="sm" icon={I.Zap}>PROVISIONAR</AMGButton>
                  </div>
                </div>
              ))}
            </div>

            <div className="amg-card card-clip p-5 self-start">
              <div className="flex items-center gap-2 mb-3">
                <I.Sparkles size={14} stroke="#FF9A3C"/>
                <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Anàlisi IA · Fontaneria Roca</span>
              </div>
              <div className="text-2xl f-display font-black text-[#FF9A3C]">SCORE 92</div>
              <p className="text-[13px] text-[#94a3b8] mt-2">Negoci local sense web pròpia, dependent de WhatsApp manual. Alta probabilitat de conversió en demo.</p>
              <div className="mt-4">
                <div className="f-mono text-[10px] uppercase text-[#64748b] mb-2">Senyals detectats</div>
                <div className="space-y-1.5">
                  {leads[0].signals.map((s,i) => (
                    <div key={i} className="flex items-start gap-2 text-[12px]">
                      <I.Check size={12} stroke="#39d353" className="mt-0.5 shrink-0"/>
                      <span>{s}</span>
                    </div>
                  ))}
                </div>
              </div>
              <div className="mt-4 p-3 border-l-2 border-l-[#FF6B00] bg-[rgba(255,107,0,0.05)]">
                <div className="f-mono text-[10px] uppercase text-[#FF9A3C] mb-1">Recomanació</div>
                <div className="text-[12px]">Provisionar demo del flux <span className="text-[#FF9A3C] f-mono">"Lampisteria · Cita & Pressupost"</span> (4 fases · 18h).</div>
              </div>
              <AMGButton className="w-full justify-center mt-4" icon={I.Zap}>PROVISIONAR DEMO</AMGButton>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── 02 · Budgets ───────────
function AMGBudgets() {
  const budgets = [
    ['#PRES-2026-018','Fontaneria Roca','Lampisteria · Cita & Pressupost','€1.840','SENT','warning','12 Abr'],
    ['#PRES-2026-017','Fisio Llevant','Fisio · Booking & Recordatoris','€2.290','ACCEPTED','success','11 Abr'],
    ['#PRES-2026-016','Tallers Vidal','Mecànic · Avís & Factura','€1.490','DRAFT','neutral','10 Abr'],
    ['#PRES-2026-015','Estètica Núria','Bellesa · Cita IA','€1.140','SENT','warning','08 Abr'],
    ['#PRES-2026-014','Clínica Dental Vila','Dental · Recordatoris IA','€2.880','ACCEPTED','success','05 Abr'],
    ['#PRES-2026-013','Forn Can Bigues','Custom flow','€  890','REJECTED','danger','02 Abr'],
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="budgets"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="pressupostos" subtitle="Ofertes per a leads provisionats">
          <AMGButton size="sm" icon={I.Plus}>NOU PRESSUPOST</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-auto amg-grid p-8 space-y-5">
          <div className="grid grid-cols-4 gap-3">
            <AMGStat label="Draft" value="3" tone="info" icon={I.Edit}/>
            <AMGStat label="Sent · pendent" value="4" tone="warning" icon={I.Clock}/>
            <AMGStat label="Accepted" value="11" delta="+3" tone="success" icon={I.Check}/>
            <AMGStat label="Win rate" value="68%" delta="+4%" tone="accent" icon={I.Trending}/>
          </div>

          <div className="grid grid-cols-3 gap-3">
            {[
              ['DRAFT','3','#94a3b8','rgba(148,163,184,0.08)'],
              ['SENT · esperant','4','#f0b429','rgba(240,180,41,0.08)'],
              ['ACCEPTED','11','#39d353','rgba(57,211,83,0.08)'],
            ].map(([t,n,col,bg],i) => (
              <div key={i} className="card-clip p-4" style={{background:bg, borderLeft:`2px solid ${col}`}}>
                <div className="flex items-center justify-between">
                  <span className="f-mono text-[10px] uppercase tracking-[0.18em]" style={{color:col}}>{t}</span>
                  <span className="f-display font-black text-2xl" style={{color:col}}>{n}</span>
                </div>
              </div>
            ))}
          </div>

          <div className="amg-card card-clip overflow-hidden">
            <div className="grid grid-cols-[140px_1.2fr_1.4fr_120px_120px_100px_100px] gap-3 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
              <div>Núm.</div><div>Client</div><div>Flux proposat</div><div className="text-right">Total</div><div>Estat</div><div>Data</div><div></div>
            </div>
            {budgets.map((b,i) => (
              <div key={i} className="grid grid-cols-[140px_1.2fr_1.4fr_120px_120px_100px_100px] gap-3 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)] group">
                <span className="f-mono text-xs text-[#FF9A3C]">{b[0]}</span>
                <span className="text-sm">{b[1]}</span>
                <span className="text-[13px] text-[#94a3b8]">{b[2]}</span>
                <span className="f-mono text-sm text-right">{b[3]}</span>
                <AMGBadge tone={b[5]}>{b[4]}</AMGBadge>
                <span className="f-mono text-[11px] text-[#64748b]">{b[6]}</span>
                <div className="flex justify-end gap-1">
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Mail size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={12}/></button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── 03 · Clients & Audit ───────────
function AMGClientsAudit() {
  const clients = [
    { name:'Nebula Studio', flow:'Estudi creatiu · Lead & Brief', health:94, phases:[1,1,1,1,2], next:'Onboarding training', color:'#FF6B00' },
    { name:'Cal Rovira', flow:'Restaurant · Reserva IA', health:88, phases:[1,1,1,2,0], next:'Fase 4 · Activar pagaments', color:'#58a6ff' },
    { name:'Fusta Vives', flow:'Fusteria · Pressupost auto', health:82, phases:[1,1,2,0,0], next:'Fase 3 · Plantilles', color:'#39d353' },
    { name:'Sínia Coffee', flow:'Cafeteria · Loyalty', health:62, phases:[1,1,1,3,0], next:'⚠ Bloquejat · Stripe', color:'#f0b429' },
    { name:'Codi Blau', flow:'Software · Onboarding', health:91, phases:[1,1,1,1,1], next:'Manteniment continu', color:'#FF9A3C' },
    { name:'Clínica Dental Vila', flow:'Dental · Recordatoris', health:78, phases:[1,1,2,0,0], next:'Fase 3 · Twilio', color:'#ff6666' },
  ];
  // phase: 0=pending, 1=delivered, 2=in-progress, 3=blocked
  const phaseColor = (p) => p===1 ? '#39d353' : p===2 ? '#FF6B00' : p===3 ? '#ff4444' : '#212140';
  const phaseLabel = (p) => p===1 ? '✓' : p===2 ? '◐' : p===3 ? '!' : '';
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="clients"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="clients & audit" subtitle="Cicle de vida · Health score · Service history">
          <AMGButton variant="outline" size="sm" icon={I.Download}>EXPORTAR AUDIT</AMGButton>
          <AMGButton size="sm" icon={I.Plus}>NOU CLIENT</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-auto amg-grid p-8 space-y-5">
          <div className="grid grid-cols-4 gap-3">
            <AMGStat label="Clients actius" value="48" delta="+3" tone="success" icon={I.Users}/>
            <AMGStat label="Health mitjà" value="84" delta="+2" tone="accent" icon={I.Activity}/>
            <AMGStat label="Fases en curs" value="23" tone="info" icon={I.Layers}/>
            <AMGStat label="Bloquejos" value="2" delta="−1" tone="danger" icon={I.Bell}/>
          </div>

          <div className="amg-card card-clip overflow-hidden">
            <div className="grid grid-cols-[1.6fr_1.6fr_100px_220px_1.4fr_80px] gap-3 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
              <div>Client</div><div>Flux contractat</div><div>Health</div><div>Fases (1 · 2 · 3 · 4 · 5)</div><div>Següent pas</div><div></div>
            </div>
            {clients.map((c,i) => (
              <div key={i} className="grid grid-cols-[1.6fr_1.6fr_100px_220px_1.4fr_80px] gap-3 px-5 py-4 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)]">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 flex items-center justify-center f-display font-bold text-sm text-black" style={{background:c.color}}>{c.name[0]}</div>
                  <div className="text-sm font-semibold">{c.name}</div>
                </div>
                <span className="text-[13px] text-[#94a3b8]">{c.flow}</span>
                <div className="flex items-center gap-2">
                  <div className="w-10 h-1.5 bg-[#212140]"><div className="h-full" style={{width:`${c.health}%`, background: c.health>=80 ? '#39d353' : c.health>=60 ? '#f0b429' : '#ff4444'}}></div></div>
                  <span className="f-mono text-xs">{c.health}</span>
                </div>
                <div className="flex gap-1.5">
                  {c.phases.map((p,j) => (
                    <div key={j} className="w-9 h-7 flex items-center justify-center f-mono text-xs font-bold border" style={{background: phaseColor(p)+'22', borderColor: phaseColor(p), color: p===0 ? '#64748b' : phaseColor(p)}}>
                      {phaseLabel(p) || j+1}
                    </div>
                  ))}
                </div>
                <span className={`f-mono text-[11px] uppercase tracking-wider ${c.next.startsWith('⚠') ? 'text-[#ff6666]' : 'text-[#94a3b8]'}`}>{c.next}</span>
                <div className="flex justify-end">
                  <button className="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.ArrowRight size={14}/></button>
                </div>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="amg-card card-clip p-5">
              <AMGSectionTitle eyebrow="Service history" title="Nebula Studio · timeline"/>
              <div className="space-y-3">
                {[
                  ['#01','Brief & discovery','Entregat','12 Mar','#39d353'],
                  ['#02','Setup workspace + n8n','Entregat','15 Mar','#39d353'],
                  ['#03','Construcció flux principal','Entregat','22 Mar','#39d353'],
                  ['#04','QA + deploy producció','Entregat','01 Abr','#39d353'],
                  ['#05','Onboarding training','En curs','08 Abr','#FF6B00'],
                ].map((p,i) => (
                  <div key={i} className="flex items-center gap-3">
                    <div className="w-8 h-8 flex items-center justify-center f-mono text-[10px] font-bold border" style={{background:p[4]+'22', borderColor:p[4], color:p[4]}}>{p[0]}</div>
                    <div className="flex-1">
                      <div className="text-sm font-semibold">{p[1]}</div>
                      <div className="f-mono text-[10px] text-[#64748b]">{p[2]} · {p[3]}</div>
                    </div>
                    <I.Check size={14} stroke={p[4]}/>
                  </div>
                ))}
              </div>
            </div>
            <div className="amg-card card-clip p-5">
              <AMGSectionTitle eyebrow="Audit log" title="Activitat recent"/>
              <div className="space-y-2 f-mono text-[11px]">
                {[
                  ['18:42','FASE_ENTREGADA','Nebula · Fase 4','#39d353'],
                  ['17:30','TRIGGER_DISPARAT','Nebula → Onboarding training','#FF9A3C'],
                  ['16:12','BLOQUEIG_DETECTAT','Sínia Coffee · Stripe credentials','#ff6666'],
                  ['15:05','FACTURA_EMESA','Cal Rovira · €490','#58a6ff'],
                  ['14:30','LEAD_PROVISIONAT','Fontaneria Roca → Demo','#FF9A3C'],
                  ['13:15','HEALTH_UPDATE','Codi Blau · 88 → 91','#39d353'],
                ].map((a,i) => (
                  <div key={i} className="flex gap-3 py-1.5 border-b border-[rgba(226,232,240,0.04)] last:border-b-0">
                    <span className="text-[#64748b] w-12">{a[0]}</span>
                    <span className="w-32" style={{color:a[3]}}>{a[1]}</span>
                    <span className="flex-1 text-[#94a3b8]">{a[2]}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── 04 · Content Factory ───────────
function AMGContentFactory() {
  const phases = [
    { n:1, name:'Brief & discovery', hours:2, autos:['Form intake','Slack notify'] },
    { n:2, name:'Setup workspace', hours:3, autos:['n8n provision','Vault init','GHL workspace'] },
    { n:3, name:'Construcció flux', hours:8, autos:['Bot training','Calendar sync','Stripe link'] },
    { n:4, name:'QA + deploy', hours:3, autos:['Smoke tests','DNS publish','Backup snapshot'] },
    { n:5, name:'Onboarding training', hours:2, autos:['Send guide','Loom record','Schedule check-in'] },
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="factory"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="content factory" subtitle="Editor de fluxos productitzats">
          <AMGButton variant="outline" size="sm" icon={I.Play}>PROVAR FLUX</AMGButton>
          <AMGButton size="sm" icon={I.Check}>PUBLICAR v1.4</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-hidden flex">
          {/* Left panel — flow list */}
          <div className="w-[280px] border-r border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/60 flex flex-col">
            <div className="p-4 border-b border-[rgba(255,107,0,0.12)] flex items-center justify-between">
              <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Fluxos · 8</span>
              <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Plus size={14}/></button>
            </div>
            <div className="flex-1 overflow-auto">
              {[
                ['Lampisteria · Cita & Pressupost','5 fases · 18h',true],
                ['Restaurant · Reserva IA','4 fases · 14h',false],
                ['Fisio · Booking & Recordatoris','5 fases · 20h',false],
                ['Mecànic · Avís & Factura','4 fases · 12h',false],
                ['Dental · Recordatoris IA','6 fases · 22h',false],
                ['Bellesa · Cita IA','4 fases · 14h',false],
                ['Cafeteria · Loyalty','5 fases · 16h',false],
                ['Custom · plantilla buida','— fases',false],
              ].map(([n,d,a],i) => (
                <div key={i} className={`px-4 py-3 border-l-2 cursor-pointer ${a ? 'border-l-[#FF6B00] bg-[rgba(255,107,0,0.08)]' : 'border-l-transparent hover:bg-white/[0.02]'}`}>
                  <div className={`text-sm ${a ? 'text-[#FF9A3C] font-semibold' : 'text-[#e2e8f0]'}`}>{n}</div>
                  <div className="f-mono text-[10px] text-[#64748b] mt-0.5">{d}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Editor */}
          <div className="flex-1 overflow-auto amg-grid p-8">
            <div className="flex items-start justify-between mb-6">
              <div>
                <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">FLUX · v1.4 · DRAFT</span>
                <h2 className="f-display font-black text-2xl mt-1">LAMPISTERIA · CITA & PRESSUPOST</h2>
                <p className="text-sm text-[#94a3b8] mt-1">5 fases · 18h totals · Preu base €1.840 · Mantenir €49/mes</p>
              </div>
              <div className="flex gap-2">
                <AMGButton variant="ghost" size="sm">VERSIONS</AMGButton>
                <AMGButton variant="outline" size="sm" icon={I.Edit}>EDITAR META</AMGButton>
              </div>
            </div>

            <div className="space-y-3">
              {phases.map((p,i) => (
                <div key={i} className="amg-card card-clip p-5">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center f-display font-black text-[#FF9A3C] text-lg">
                      {p.n}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        <span className="f-display font-bold text-base">{p.name.toUpperCase()}</span>
                        <AMGBadge tone="neutral">{p.hours}h</AMGBadge>
                      </div>
                      <div className="flex flex-wrap gap-1.5 mt-2">
                        {p.autos.map((a,j) => (
                          <span key={j} className="inline-flex items-center gap-1 px-2 h-5 f-mono text-[10px] bg-[#1a1a2e] border border-[rgba(255,107,0,0.2)] text-[#FF9A3C]">
                            <I.Zap size={9}/>{a}
                          </span>
                        ))}
                        <button className="inline-flex items-center gap-1 px-2 h-5 f-mono text-[10px] border border-dashed border-[rgba(255,107,0,0.3)] text-[#64748b] hover:text-[#FF9A3C] hover:border-[#FF9A3C]">
                          <I.Plus size={9}/>AFEGIR
                        </button>
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button className="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={14}/></button>
                      <button className="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={14}/></button>
                    </div>
                  </div>
                  {i < phases.length - 1 && (
                    <div className="ml-6 mt-3 mb-[-12px] flex items-center gap-2 f-mono text-[10px] text-[#64748b] uppercase">
                      <span className="w-px h-4 bg-[rgba(255,107,0,0.3)]"></span>
                      <I.ChevDown size={10} stroke="#FF6B00"/>
                      <span>en entregar → dispara fase {p.n+1}</span>
                    </div>
                  )}
                </div>
              ))}

              <button className="w-full p-4 border border-dashed border-[rgba(255,107,0,0.3)] f-mono text-xs uppercase text-[#FF9A3C] hover:bg-[rgba(255,107,0,0.05)] flex items-center justify-center gap-2">
                <I.Plus size={14}/>AFEGIR NOVA FASE
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── 05 · Vault ───────────
function AMGVault() {
  const creds = [
    { client:'Nebula Studio', service:'n8n workspace', key:'n8n_api_xxxxxxxx_8f2c', updated:'12 Abr', strength:'STRONG', rotated:false },
    { client:'Nebula Studio', service:'Stripe secret', key:'sk_live_xxxxxxxxxxxxxx', updated:'12 Abr', strength:'STRONG', rotated:false },
    { client:'Nebula Studio', service:'Google Calendar OAuth', key:'oauth_token_********', updated:'12 Abr', strength:'STRONG', rotated:false },
    { client:'Cal Rovira', service:'WhatsApp Business', key:'wa_token_xxxxxxxx_a01', updated:'09 Abr', strength:'STRONG', rotated:true },
    { client:'Fusta Vives', service:'n8n workspace', key:'n8n_api_xxxxxxxx_b39', updated:'02 Abr', strength:'STRONG', rotated:false },
    { client:'Sínia Coffee', service:'Stripe secret', key:'sk_live_———', updated:'—', strength:'MISSING', rotated:false },
    { client:'Clínica Dental', service:'Twilio SMS', key:'AC_xxxxxxxxxxx_45e', updated:'05 Abr', strength:'WEAK', rotated:true },
  ];
  const tone = (s) => s==='STRONG' ? 'success' : s==='WEAK' ? 'warning' : 'danger';
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="vault"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="vault" subtitle="Credencials encriptades · AES-256-GCM · per client">
          <span className="f-mono text-[10px] text-[#39d353] uppercase flex items-center gap-2 px-3 h-8 border border-[rgba(57,211,83,0.35)] bg-[rgba(57,211,83,0.08)]">
            <I.Lock size={12}/>VAULT BLOCKED · DESBLOQUEJA AMB 2FA
          </span>
          <AMGButton size="sm" icon={I.Plus}>NOVA CREDENCIAL</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-auto amg-grid p-8 space-y-5">
          <div className="grid grid-cols-4 gap-3">
            <AMGStat label="Credencials" value="142" tone="info" icon={I.Lock}/>
            <AMGStat label="Encriptació" value="OK" tone="success" icon={I.Shield}/>
            <AMGStat label="Pendents rotació" value="3" delta="−1" tone="warning" icon={I.Clock}/>
            <AMGStat label="Manquen" value="1" tone="danger" icon={I.Bell}/>
          </div>

          <div className="amg-card card-clip p-5 border-l-2 border-l-[#f0b429] bg-[rgba(240,180,41,0.05)]">
            <div className="flex items-center gap-3">
              <I.Bell size={16} stroke="#f0b429"/>
              <div className="flex-1">
                <div className="f-display font-bold text-sm text-[#f0b429]">3 CREDENCIALS NECESSITEN ROTACIÓ</div>
                <div className="text-[12px] text-[#94a3b8]">Política · rotació trimestral. La més antiga té 89 dies.</div>
              </div>
              <AMGButton size="sm" variant="outline">ROTAR ARA</AMGButton>
            </div>
          </div>

          <div className="amg-card card-clip overflow-hidden">
            <div className="grid grid-cols-[1.4fr_1.4fr_2fr_120px_120px_120px] gap-3 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
              <div>Client</div><div>Servei</div><div>Clau encriptada</div><div>Strength</div><div>Actualitzada</div><div className="text-right">Accions</div>
            </div>
            {creds.map((c,i) => (
              <div key={i} className="grid grid-cols-[1.4fr_1.4fr_2fr_120px_120px_120px] gap-3 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)] group">
                <span className="text-sm">{c.client}</span>
                <div className="flex items-center gap-2">
                  <I.Lock size={12} stroke="#FF9A3C"/>
                  <span className="f-mono text-xs text-[#94a3b8]">{c.service}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="f-mono text-xs text-[#FF9A3C] truncate">{c.key}</span>
                  {c.rotated && <AMGBadge tone="info" className="!h-4 !text-[9px]">ROTATED</AMGBadge>}
                </div>
                <AMGBadge tone={tone(c.strength)}>{c.strength}</AMGBadge>
                <span className="f-mono text-[11px] text-[#64748b]">{c.updated}</span>
                <div className="flex justify-end gap-1">
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]" title="Reveal"><I.Activity size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]" title="Rotate"><I.Zap size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#ff6666]" title="Revoke"><I.Trash size={12}/></button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── Conversion landing ───────────
function AMGConversionLanding() {
  return (
    <div className="amg w-full h-full bg-[#0d0d1a] overflow-auto">
      <header className="sticky top-0 z-30 backdrop-blur bg-[#0d0d1a]/80 border-b border-[rgba(255,107,0,0.12)]">
        <div className="max-w-[1200px] mx-auto px-10 h-16 flex items-center gap-8">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-[#FF6B00] btn-clip flex items-center justify-center"><span className="f-display font-black text-black text-xs">A</span></div>
            <div className="f-display font-black text-sm tracking-[0.15em]">AMG</div>
            <span className="f-mono text-[9px] text-[#FF9A3C] tracking-[0.25em]">ENGINYERIA DIGITAL</span>
          </div>
          <nav className="flex gap-6 f-mono text-[11px] uppercase tracking-wider">
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Com funciona</a>
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Resultats</a>
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Formació</a>
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Contacte</a>
          </nav>
          <div className="flex-1"></div>
          <AMGButton variant="ghost" size="sm">VEURE FUNCIONAMENT</AMGButton>
          <AMGButton size="sm" icon={I.ArrowRight}>SOL·LICITAR DEMO</AMGButton>
        </div>
      </header>

      {/* Hero */}
      <section className="relative amg-grid overflow-hidden">
        <div className="absolute inset-0" style={{background:'radial-gradient(ellipse at 75% 40%, rgba(255,107,0,0.18), transparent 50%)'}}></div>
        <div className="relative max-w-[1200px] mx-auto px-10 py-24 grid grid-cols-[1.3fr_1fr] gap-12 items-center">
          <div>
            <div className="flex items-center gap-2 mb-5">
              <span className="w-2 h-2 bg-[#FF6B00] amg-blink"></span>
              <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">Per a fontaners, fisios, cliniques i tallers</span>
            </div>
            <h1 className="f-display font-black text-[68px] leading-[0.96] tracking-tight">
              MÉS <span className="text-[#FF9A3C]">CLIENTS.</span><br/>
              MENYS <span className="underline decoration-[#FF6B00] decoration-4 underline-offset-4">CAOS.</span><br/>
              EL TEU NEGOCI,<br/>EN ORDRE.
            </h1>
            <p className="text-xl text-[#94a3b8] mt-6 max-w-xl leading-relaxed">
              Deixem el teu negoci funcionant sol — sense que tu hagis de seure davant del WhatsApp tot el dia.
            </p>
            <div className="flex items-center gap-3 mt-8">
              <AMGButton size="lg" icon={I.ArrowRight}>SOL·LICITAR DEMO</AMGButton>
              <AMGButton variant="outline" size="lg" icon={I.Play}>VEURE FUNCIONAMENT · 90s</AMGButton>
            </div>
            <div className="flex items-center gap-6 mt-10 f-mono text-[11px] uppercase text-[#64748b]">
              <span className="flex items-center gap-2"><I.Check size={12} stroke="#39d353"/>NO PAGUES FINS QUE FUNCIONA</span>
              <span className="flex items-center gap-2"><I.Check size={12} stroke="#39d353"/>EQUIP HUMÀ A LLEVANT</span>
            </div>
          </div>
          {/* Hero visual */}
          <div className="relative">
            <div className="amg-card card-clip p-6 relative">
              <div className="flex items-center justify-between mb-4">
                <span className="f-mono text-[10px] uppercase text-[#FF9A3C]">Avui · dilluns</span>
                <span className="f-mono text-[10px] text-[#39d353] flex items-center gap-1.5"><span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>EN MARXA</span>
              </div>
              <div className="space-y-2">
                {[
                  ['09:14','Cita confirmada','Maria Pous · revisió caldera','#39d353'],
                  ['10:32','Pressupost enviat','Joan Vila · €340','#58a6ff'],
                  ['11:08','Recordatori enviat','Cita demà · 4 clients','#FF9A3C'],
                  ['12:45','Factura cobrada','Cal Rovira · €490','#39d353'],
                  ['13:20','Nova cita','Pere F. · dijous 10h','#39d353'],
                  ['14:50','Pressupost acceptat','Ramon S. · €1.140','#39d353'],
                ].map((r,i) => (
                  <div key={i} className="flex items-center gap-3 p-2 border-l-2" style={{borderColor:r[3], background:r[3]+'08'}}>
                    <span className="f-mono text-[10px] text-[#64748b] w-12">{r[0]}</span>
                    <div className="flex-1">
                      <div className="text-[13px] font-semibold" style={{color:r[3]}}>{r[1]}</div>
                      <div className="f-mono text-[10px] text-[#94a3b8]">{r[2]}</div>
                    </div>
                    <I.Check size={12} stroke={r[3]}/>
                  </div>
                ))}
              </div>
              <div className="mt-3 pt-3 border-t border-[rgba(255,107,0,0.15)] flex items-center justify-between">
                <span className="f-mono text-[10px] text-[#64748b]">6 ACCIONS · 0 INTERVENCIÓ</span>
                <span className="f-display font-bold text-[#FF9A3C]">+€1.970</span>
              </div>
            </div>
            <div className="absolute -top-4 -right-4 amg-card card-clip p-3 w-44">
              <div className="f-mono text-[9px] text-[#64748b] uppercase">Avui has estalviat</div>
              <div className="f-display font-black text-2xl text-[#FF9A3C]">3h 40m</div>
            </div>
          </div>
        </div>
      </section>

      {/* Problem — el caos del WhatsApp */}
      <section className="relative py-24 border-y border-[rgba(255,107,0,0.12)]">
        <div className="max-w-[1200px] mx-auto px-10">
          <div className="grid grid-cols-2 gap-12 items-center">
            <div>
              <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#ff6666]">El problema</span>
              <h2 className="f-display font-black text-5xl leading-[1.0] mt-2">EL CAOS DEL <span className="text-[#ff6666]">WHATSAPP.</span></h2>
              <p className="text-lg text-[#94a3b8] mt-5">Cada dia comences amb 47 missatges sense respondre. Cites perdudes. Pressupostos a Excel. Pacients que no es presenten. Factures que no cobres.</p>
              <div className="mt-6 space-y-2 f-mono text-[12px] uppercase">
                {[
                  ['43%','de cites es perden per resposta tardana'],
                  ['2,5h','al dia gestionant WhatsApp manualment'],
                  ['€480/mes','en pressupostos no enviats a temps'],
                ].map(([n,t],i) => (
                  <div key={i} className="flex items-baseline gap-4 p-3 bg-[rgba(255,68,68,0.05)] border-l-2 border-l-[#ff4444]">
                    <span className="f-display font-black text-2xl text-[#ff6666]">{n}</span>
                    <span className="text-[#94a3b8] tracking-wider">{t}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="amg-card card-clip p-5 bg-[#13132a]">
              <div className="f-mono text-[10px] text-[#64748b] uppercase mb-3 flex items-center justify-between">
                <span>WhatsApp · Negoci</span>
                <span className="text-[#ff6666]">47 sense llegir</span>
              </div>
              <div className="space-y-2">
                {[
                  ['Maria P.','Hola, podríeu venir avui? gas no funciona','08:12'],
                  ['Joan V.','Quant em costaria canviar el dipòsit?','09:30'],
                  ['Pere F.','Cancel·lo la cita de demà','10:15'],
                  ['Ramon S.','Necessito factura de la setmana passada','11:02'],
                  ['Núria L.','Heu rebut el meu missatge?? 😡','11:50'],
                  ['Anna T.','Hola? Hola?','12:30'],
                ].map(([n,m,h],i) => (
                  <div key={i} className="p-2.5 bg-[#1a1a2e] border-l-2 border-l-[rgba(255,68,68,0.4)]">
                    <div className="flex justify-between items-baseline">
                      <span className="text-sm font-semibold">{n}</span>
                      <span className="f-mono text-[10px] text-[#64748b]">{h}</span>
                    </div>
                    <div className="text-[12px] text-[#94a3b8] mt-0.5">{m}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Solution */}
      <section className="relative py-24 amg-grid">
        <div className="max-w-[1200px] mx-auto px-10">
          <div className="text-center mb-16">
            <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">La solució</span>
            <h2 className="f-display font-black text-5xl mt-2 leading-[1.0]">EL TEU NEGOCI<br/><span className="text-[#FF9A3C]">FUNCIONANT SOL.</span></h2>
            <p className="text-lg text-[#94a3b8] mt-4 max-w-2xl mx-auto">Implementem un sistema a mida que respon, agenda, cobra i recorda — pel teu negoci, mentre tu dorms.</p>
          </div>

          <div className="grid grid-cols-3 gap-4">
            {[
              ['1','RESPON SOL','Un assistent intel·ligent contesta les preguntes habituals i recull dades del client. Tu només intervens quan val la pena.'],
              ['2','AGENDA SOL','Cites, reserves i visites es col·loquen al teu calendari. Recordatoris automàtics el dia abans.'],
              ['3','COBRA SOL','Pressupostos enviats automàticament. Factures generades en cobrar. Sense Excel, sense oblits.'],
            ].map(([n,t,d],i) => (
              <div key={i} className="amg-card card-clip p-7 group hover:border-l-2 hover:border-l-[#FF6B00] transition relative">
                <div className="f-display font-black text-7xl text-[#FF6B00]/15 absolute top-3 right-5">{n}</div>
                <div className="relative">
                  <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">FASE 0{n}</span>
                  <div className="f-display font-black text-xl mt-2">{t}</div>
                  <p className="text-[14px] text-[#94a3b8] mt-3 leading-relaxed">{d}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Demo visual */}
      <section className="relative py-24 border-y border-[rgba(255,107,0,0.12)] bg-[#13132a]/40">
        <div className="max-w-[1200px] mx-auto px-10">
          <div className="text-center mb-12">
            <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">Mira'l en acció</span>
            <h2 className="f-display font-black text-4xl mt-2">UN CLIENT ESCRIU.<br/>EL SISTEMA FA <span className="text-[#FF9A3C]">LA RESTA.</span></h2>
          </div>
          <div className="grid grid-cols-3 gap-5 items-start">
            {[
              { title:'1. EL CLIENT ESCRIU',msgs:[['Hola, em puc passar dijous?','user']], time:'10:14' },
              { title:'2. EL SISTEMA RESPON',msgs:[['Hola Maria! Tinc disponibilitat dijous a les 10h, 12h o 17h. Quina prefereixes?','bot'],['10h va bé 👍','user']], time:'10:14' },
              { title:'3. ES TANCA TOT SOL',msgs:[['Perfecte! Cita confirmada dijous 10h. T\'enviaré recordatori. Confirmaràs amb un emoji?','bot']], time:'10:14', extras:['📅 CITA AFEGIDA','🔔 RECORDATORI PROGRAMAT','📧 EMAIL ENVIAT'] },
            ].map((c,i) => (
              <div key={i} className="amg-card card-clip p-5">
                <div className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C] mb-3">{c.title}</div>
                <div className="space-y-2">
                  {c.msgs.map((m,j) => (
                    <div key={j} className={`max-w-[85%] p-2.5 text-[13px] ${m[1]==='user' ? 'bg-[#1a1a2e] border-l-2 border-l-[#94a3b8] ml-auto' : 'bg-[rgba(255,107,0,0.1)] border-l-2 border-l-[#FF6B00]'}`}>
                      {m[0]}
                    </div>
                  ))}
                </div>
                {c.extras && (
                  <div className="mt-4 pt-4 border-t border-[rgba(255,107,0,0.15)] space-y-1.5">
                    {c.extras.map((e,j) => <div key={j} className="f-mono text-[11px] text-[#39d353] flex items-center gap-2"><I.Check size={10}/>{e}</div>)}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Resultats */}
      <section className="relative py-24">
        <div className="max-w-[1200px] mx-auto px-10">
          <div className="grid grid-cols-2 gap-12 items-center">
            <div>
              <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">Resultats reals</span>
              <h2 className="f-display font-black text-5xl mt-2">CIFRES,<br/>NO PROMESES.</h2>
              <p className="text-lg text-[#94a3b8] mt-4">Mitjana dels primers 30 dies dels nostres clients.</p>
              <div className="mt-8 space-y-3">
                {[
                  ['Maria — Lampisteria Roca','"Vaig recuperar 18h a la setmana. La cunyada ja no respon WhatsApp pels dimecres a la nit."','M'],
                  ['Joan — Fisio Llevant','"Ompliem agenda dos dies abans. Abans érem nosaltres trucant cada client. Ara ja no."','J'],
                  ['Núria — Estètica','"Vaig pujar facturació un 32% sense fer més hores."','N'],
                ].map((t,i) => (
                  <div key={i} className="amg-card card-clip p-4 flex gap-3">
                    <div className="w-10 h-10 bg-[#FF6B00] flex items-center justify-center f-display font-black text-black shrink-0">{t[2]}</div>
                    <div>
                      <div className="text-[13px] text-[#e2e8f0]">{t[1]}</div>
                      <div className="f-mono text-[10px] text-[#FF9A3C] mt-1.5 uppercase tracking-wider">{t[0]}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              {[
                ['+38%','MÉS CITES','tancades automàticament'],
                ['−72%','MENYS TEMPS','responent WhatsApp'],
                ['+€1.4K','FACTURACIÓ','de mitjana al mes'],
                ['<2 min','RESPOSTA','24/7 al client'],
              ].map(([v,t,s],i) => (
                <div key={i} className="amg-card card-clip p-6 hover:border-l-2 hover:border-l-[#FF6B00] transition">
                  <div className="f-display font-black text-4xl text-[#FF9A3C]">{v}</div>
                  <div className="f-display font-bold text-sm mt-2">{t}</div>
                  <div className="f-mono text-[10px] text-[#64748b] uppercase mt-1">{s}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Formació · control total */}
      <section className="relative py-24 bg-[#13132a]/40 border-y border-[rgba(255,107,0,0.12)]">
        <div className="max-w-[1200px] mx-auto px-10">
          <div className="text-center mb-12">
            <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">No t'ho deixem en mans negres</span>
            <h2 className="f-display font-black text-4xl mt-2">CONTROL TOTAL.<br/>SEMPRE TEU.</h2>
            <p className="text-lg text-[#94a3b8] mt-3 max-w-2xl mx-auto">T'ensenyem a dominar el sistema. Tu mires què passa, decideixes què canviar i nosaltres ho implementem.</p>
          </div>
          <div className="grid grid-cols-4 gap-4">
            {[
              ['Play','VÍDEOS CURTS','5 minuts cada un. Tot el que necessites saber.'],
              ['Bell','ALERTES CLARES','Et avisem només quan cal la teva atenció.'],
              ['Activity','PANEL EN VIU','Veus tot el que el sistema fa pel teu negoci.'],
              ['Users','EQUIP HUMÀ','Una trucada i parlem. Sense bots, sense formularis.'],
            ].map(([ic,t,d],i) => {
              const Ic = I[ic];
              return (
                <div key={i} className="amg-card card-clip p-5">
                  <Ic size={20} stroke="#FF9A3C"/>
                  <div className="f-display font-bold text-sm mt-3">{t}</div>
                  <p className="text-[13px] text-[#94a3b8] mt-1">{d}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA final */}
      <section className="relative py-28 amg-grid">
        <div className="absolute inset-0" style={{background:'radial-gradient(ellipse at center, rgba(255,107,0,0.18), transparent 60%)'}}></div>
        <div className="relative max-w-[900px] mx-auto px-10 text-center">
          <span className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C]">Pas següent</span>
          <h2 className="f-display font-black text-6xl leading-[1.0] mt-3">VEUS-HO<br/><span className="text-[#FF9A3C]">ABANS DE PAGAR.</span></h2>
          <p className="text-xl text-[#94a3b8] mt-5">Et provisionarem una demo personalitzada amb el teu negoci. Sense compromís. Sense targeta.</p>
          <div className="flex items-center justify-center gap-3 mt-10">
            <AMGButton size="lg" icon={I.ArrowRight}>SOL·LICITAR DEMO</AMGButton>
            <AMGButton variant="outline" size="lg" icon={I.Play}>VEURE FUNCIONAMENT</AMGButton>
          </div>
          <div className="flex items-center justify-center gap-6 mt-8 f-mono text-[11px] uppercase text-[#64748b]">
            <span className="flex items-center gap-2"><I.Check size={12} stroke="#39d353"/>30 MIN DE TRUCADA</span>
            <span className="flex items-center gap-2"><I.Check size={12} stroke="#39d353"/>DEMO REAL · 48H</span>
            <span className="flex items-center gap-2"><I.Check size={12} stroke="#39d353"/>SENSE COMPROMÍS</span>
          </div>
        </div>
      </section>

      <footer className="border-t border-[rgba(255,107,0,0.15)] bg-[#0d0d1a] py-10">
        <div className="max-w-[1200px] mx-auto px-10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 bg-[#FF6B00] btn-clip flex items-center justify-center"><span className="f-display font-black text-black text-[10px]">A</span></div>
            <span className="f-display font-black text-sm">AMG ENGINYERIA DIGITAL</span>
          </div>
          <span className="f-mono text-[10px] text-[#64748b] uppercase tracking-wider">© 2026 · Llevant, Catalunya · hola@amg.cat</span>
        </div>
      </footer>
    </div>
  );
}

Object.assign(window, {
  AMGSidebarV2, AMGTopbarV2,
  AMGProspecting, AMGBudgets, AMGClientsAudit, AMGContentFactory, AMGVault,
  AMGConversionLanding,
});
