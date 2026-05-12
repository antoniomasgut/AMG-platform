// amg-factory-hub.jsx — Content Factory hub: Onboardings · Fases · Automatitzacions · Micro-landings

function AMGFactoryHub() {
  const [tab, setTab] = React.useState('onboardings');
  const tabs = [
    { id:'onboardings', label:'Onboardings', icon:I.Sparkles, count:6 },
    { id:'phases', label:'Fases · llibreria', icon:I.Layers, count:24 },
    { id:'autos', label:'Automatitzacions', icon:I.Zap, count:38 },
    { id:'landings', label:'Micro-landings', icon:I.Globe, count:6 },
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="factory"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="content factory" subtitle="Llibreria de tot el que es pot configurar i activar per client">
          <AMGButton variant="outline" size="sm" icon={I.Download}>EXPORTAR</AMGButton>
          <AMGButton size="sm" icon={I.Plus}>NOU · {tab.toUpperCase()}</AMGButton>
        </AMGTopbarV2>

        {/* Tabs */}
        <div className="border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/80 px-8 flex items-center gap-1">
          {tabs.map(t => {
            const active = t.id === tab;
            return (
              <button key={t.id} onClick={()=>setTab(t.id)}
                className={`relative h-12 px-5 flex items-center gap-2 f-mono text-[11px] uppercase tracking-wider transition
                  ${active ? 'text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>
                <t.icon size={13}/>
                <span>{t.label}</span>
                <span className={`f-mono text-[9px] px-1.5 h-4 flex items-center ${active ? 'bg-[rgba(255,107,0,0.18)] text-[#FF9A3C]' : 'bg-[#212140] text-[#94a3b8]'}`}>{t.count}</span>
                {active && <span className="absolute left-0 right-0 bottom-[-1px] h-[2px] bg-[#FF6B00]"></span>}
              </button>
            );
          })}
        </div>

        <div className="flex-1 overflow-auto amg-grid p-8">
          {tab === 'onboardings' && <OnboardingsTab/>}
          {tab === 'phases' && <PhasesLibTab/>}
          {tab === 'autos' && <AutosLibTab/>}
          {tab === 'landings' && <LandingsLibTab/>}
        </div>
      </main>
    </div>
  );
}

function OnboardingsTab() {
  const obs = [
    { name:'Lampisteria · Cita & Pressupost', sector:'Lampisteria', phases:5, hours:18, autos:11, ml:1, status:'PUBLISHED', v:'v1.4', clients:8 },
    { name:'Fisio · Booking & Recordatoris', sector:'Salut', phases:5, hours:20, autos:14, ml:1, status:'PUBLISHED', v:'v2.1', clients:5 },
    { name:'Restaurant · Reserva IA', sector:'Hostaleria', phases:4, hours:14, autos:9, ml:1, status:'PUBLISHED', v:'v1.2', clients:3 },
    { name:'Dental · Recordatoris IA', sector:'Salut', phases:6, hours:22, autos:16, ml:1, status:'PUBLISHED', v:'v1.0', clients:2 },
    { name:'Mecànic · Avís & Factura', sector:'Tallers', phases:4, hours:12, autos:7, ml:1, status:'DRAFT', v:'v0.3', clients:0 },
    { name:'Bellesa · Cita IA', sector:'Estètica', phases:4, hours:14, autos:8, ml:1, status:'DRAFT', v:'v0.5', clients:0 },
  ];
  const tone = (s) => s==='PUBLISHED' ? 'success' : s==='DRAFT' ? 'warning' : 'neutral';
  return (
    <div className="space-y-5">
      <div className="grid grid-cols-4 gap-3">
        <AMGStat label="Onboardings" value="6" tone="accent" icon={I.Sparkles}/>
        <AMGStat label="Publicats" value="4" tone="success" icon={I.Globe}/>
        <AMGStat label="En clients actius" value="18" delta="+3" tone="info" icon={I.Users}/>
        <AMGStat label="Hores totals catàleg" value="100h" tone="accent" icon={I.Clock}/>
      </div>

      <div className="amg-card card-clip p-4 border-l-2 border-l-[#FF6B00] bg-[rgba(255,107,0,0.04)] flex items-center gap-3">
        <I.Sparkles size={16} stroke="#FF9A3C"/>
        <div className="flex-1">
          <div className="f-display font-bold text-sm">UN ONBOARDING ÉS UN PRODUCTE COMPLET</div>
          <div className="text-[12px] text-[#94a3b8]">Inclou les fases, les automatitzacions associades i la micro-landing per captar leads. Tot empaquetat per provisionar a un client en un clic.</div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        {obs.map((o,i) => (
          <div key={i} className="amg-card card-clip overflow-hidden hover:border-l-2 hover:border-l-[#FF6B00] transition group">
            <div className="px-5 py-4 flex items-start justify-between border-b border-[rgba(255,107,0,0.12)]">
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1.5">
                  <AMGBadge tone={tone(o.status)}>{o.status}</AMGBadge>
                  <span className="f-mono text-[10px] text-[#FF9A3C]">{o.v}</span>
                  <span className="f-mono text-[10px] text-[#64748b]">· {o.sector}</span>
                </div>
                <div className="f-display font-bold text-base">{o.name}</div>
              </div>
              <div className="flex gap-1 opacity-0 group-hover:opacity-100">
                <button className="w-7 h-7 text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
                <button className="w-7 h-7 text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={12}/></button>
              </div>
            </div>
            <div className="grid grid-cols-4 gap-3 p-5 border-b border-[rgba(226,232,240,0.04)]">
              {[
                ['FASES', o.phases, I.Layers],
                ['HORES', o.hours+'h', I.Clock],
                ['AUTOS', o.autos, I.Zap],
                ['LANDING', o.ml, I.Globe],
              ].map(([l,v,Ic],j) => (
                <div key={j}>
                  <div className="flex items-center gap-1.5">
                    <Ic size={10} stroke="#FF9A3C"/>
                    <span className="f-mono text-[9px] uppercase text-[#64748b]">{l}</span>
                  </div>
                  <div className="f-display font-bold text-lg text-[#FF9A3C] mt-0.5">{v}</div>
                </div>
              ))}
            </div>
            <div className="px-5 py-3 flex items-center justify-between">
              <span className="f-mono text-[10px] text-[#64748b]">Actiu en <span className="text-[#FF9A3C]">{o.clients}</span> clients</span>
              <AMGButton size="sm" variant="ghost" icon={I.ArrowRight}>OBRIR</AMGButton>
            </div>
          </div>
        ))}

        <button className="card-clip border border-dashed border-[rgba(255,107,0,0.35)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] flex flex-col items-center justify-center gap-3 min-h-[260px] transition">
          <div className="w-12 h-12 bg-[rgba(255,107,0,0.12)] btn-clip flex items-center justify-center"><I.Plus size={20} stroke="#FF9A3C"/></div>
          <div className="f-display font-bold text-sm text-[#FF9A3C]">CREAR ONBOARDING</div>
          <div className="f-mono text-[10px] text-[#64748b] uppercase tracking-wider">Combina fases · automatitzacions · micro-landing</div>
        </button>
      </div>
    </div>
  );
}

function PhasesLibTab() {
  const phases = [
    ['BRIEF','Brief & discovery','2h','Form intake · Slack notify','9 onboardings'],
    ['SETUP','Setup workspace','3h','n8n · Vault · GHL','9 onboardings'],
    ['BUILD-FLOW','Construcció flux','8h','Bot training · Calendar','7 onboardings'],
    ['BUILD-BOT','Entrenament bot WA','6h','OpenAI fine-tune','5 onboardings'],
    ['QA','QA + deploy','3h','Smoke tests · DNS','9 onboardings'],
    ['ONBOARD','Onboarding training','2h','Send guide · Loom','9 onboardings'],
    ['LANDING','Publicar micro-landing','2h','DNS · CDN','6 onboardings'],
    ['STRIPE','Configurar pagaments','3h','Stripe · webhooks','4 onboardings'],
    ['CAL','Sincronització Calendar','2h','Google · Outlook','6 onboardings'],
    ['REVIEWS','Captació ressenyes','2h','GMB · Trustpilot','3 onboardings'],
  ];
  return (
    <div className="space-y-5">
      <div className="amg-card card-clip p-4 border-l-2 border-l-[#FF6B00] bg-[rgba(255,107,0,0.04)] text-[12px]">
        <span className="text-[#FF9A3C] f-mono uppercase">FASES = blocs reutilitzables.</span> Es defineixen una vegada i s'incorporen a múltiples onboardings amb hores i automatitzacions ajustables.
      </div>
      <div className="amg-card card-clip overflow-hidden">
        <div className="grid grid-cols-[110px_1.4fr_80px_1.6fr_140px_100px] gap-3 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
          <div>Codi</div><div>Nom</div><div>Hores</div><div>Automatitzacions per defecte</div><div>Reutilització</div><div className="text-right">Accions</div>
        </div>
        {phases.map((p,i) => (
          <div key={i} className="grid grid-cols-[110px_1.4fr_80px_1.6fr_140px_100px] gap-3 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] last:border-b-0 hover:bg-[rgba(255,107,0,0.04)] group">
            <span className="f-mono text-[10px] text-[#FF9A3C] bg-[rgba(255,107,0,0.1)] px-2 py-0.5 inline-block w-fit">{p[0]}</span>
            <span className="text-sm font-semibold">{p[1]}</span>
            <span className="f-mono text-xs text-[#94a3b8]">{p[2]}</span>
            <span className="f-mono text-[10px] text-[#94a3b8]">{p[3]}</span>
            <span className="f-mono text-[10px] text-[#64748b]">{p[4]}</span>
            <div className="flex justify-end gap-1 opacity-0 group-hover:opacity-100">
              <button className="w-7 h-7 text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
              <button className="w-7 h-7 text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={12}/></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function AutosLibTab() {
  const cats = [
    ['NOTIFICACIÓ', I.Bell, '#58a6ff', [
      ['notify-owner','Email al propietari','EMAIL','9 fluxos'],
      ['notify-slack','Slack #leads','SLACK','6 fluxos'],
      ['notify-telegram','Telegram channel','TELEGRAM','4 fluxos'],
    ]],
    ['CLIENT', I.Users, '#39d353', [
      ['confirm-email','Email confirmació','EMAIL','9 fluxos'],
      ['confirm-wa','WhatsApp confirmació','WHATSAPP','7 fluxos'],
      ['reminder-24h','Recordatori 24h abans','WHATSAPP','7 fluxos'],
      ['review-request','Demanar ressenya','EMAIL+WA','3 fluxos'],
    ]],
    ['INTEGRACIÓ', I.Link, '#FF9A3C', [
      ['cal-create','Crear event Calendar','CALENDAR','6 fluxos'],
      ['stripe-link','Generar link pagament','STRIPE','4 fluxos'],
      ['crm-create-lead','Crear lead a CRM','HUBSPOT','5 fluxos'],
      ['drive-folder','Crear carpeta Drive','GDRIVE','3 fluxos'],
    ]],
  ];
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-3 gap-3">
        <AMGStat label="Plantilles automació" value="38" tone="accent" icon={I.Zap}/>
        <AMGStat label="Execucions (24h)" value="284" delta="+18" tone="info" icon={I.Activity}/>
        <AMGStat label="Errors" value="0" tone="success" icon={I.Check}/>
      </div>

      {cats.map(([title, Ic, color, items],i) => (
        <div key={i}>
          <div className="flex items-center gap-2 mb-3">
            <Ic size={14} stroke={color}/>
            <span className="f-mono text-[11px] uppercase tracking-[0.2em]" style={{color}}>{title}</span>
            <div className="flex-1 h-px bg-[rgba(255,107,0,0.12)]"></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {items.map((a,j) => (
              <div key={j} className="amg-card card-clip p-4 hover:border-l-2 hover:border-l-[#FF6B00] transition group">
                <div className="flex items-center justify-between">
                  <span className="f-mono text-[10px] text-[#FF9A3C] bg-[rgba(255,107,0,0.1)] px-2 py-0.5">{a[0]}</span>
                  <span className="f-mono text-[9px]" style={{color}}>{a[2]}</span>
                </div>
                <div className="text-sm font-semibold mt-2">{a[1]}</div>
                <div className="flex items-center justify-between mt-2">
                  <span className="f-mono text-[10px] text-[#64748b]">{a[3]}</span>
                  <button className="f-mono text-[10px] text-[#94a3b8] hover:text-[#FF9A3C] flex items-center gap-1 opacity-0 group-hover:opacity-100">EDITAR <I.ArrowRight size={9}/></button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function LandingsLibTab() {
  const tpls = [
    ['Lampisteria · Cita ràpida','Lampisteria',4,142,'12.4%','PUBLISHED','#FF6B00'],
    ['Fisio · Reserva online','Salut',5,98,'9.8%','PUBLISHED','#58a6ff'],
    ['Dental · Primera visita','Salut',6,67,'14.2%','PUBLISHED','#39d353'],
    ['Restaurant · Reserva taula','Hostaleria',5,52,'10.1%','PUBLISHED','#FF9A3C'],
    ['Estètica · Reserva','Bellesa',4,0,'—','DRAFT','#f0b429'],
    ['Mecànic · Cita taller','Tallers',3,0,'—','DRAFT','#94a3b8'],
  ];
  const tone = (s) => s==='PUBLISHED' ? 'success' : 'warning';
  return (
    <div className="space-y-5">
      <div className="grid grid-cols-3 gap-3">
        <AMGStat label="Plantilles" value="6" tone="accent" icon={I.Globe}/>
        <AMGStat label="Leads (30d)" value="341" delta="+48" tone="info" icon={I.Users}/>
        <AMGStat label="Conv. mitjana" value="11.2%" delta="+1.4%" tone="accent" icon={I.Trending}/>
      </div>
      <div className="grid grid-cols-3 gap-4">
        {tpls.map((t,i) => (
          <div key={i} className="amg-card card-clip overflow-hidden hover:border-l-2 hover:border-l-[#FF6B00] transition group">
            <div className="h-24 relative overflow-hidden" style={{background:`linear-gradient(135deg, ${t[6]}33 0%, ${t[6]}11 100%)`}}>
              <div className="absolute inset-0 amg-grid-sm opacity-40"></div>
              <div className="absolute top-3 left-3"><AMGBadge tone={tone(t[5])}>{t[5]}</AMGBadge></div>
              <div className="absolute bottom-3 left-4">
                <div className="f-display font-black text-xl" style={{color:t[6]}}>{t[0].split('·')[0].trim()}</div>
              </div>
            </div>
            <div className="p-4 grid grid-cols-3 gap-2 border-t border-[rgba(255,107,0,0.12)]">
              <div><div className="f-mono text-[9px] uppercase text-[#64748b]">Seccions</div><div className="f-display font-bold text-base">{t[2]}</div></div>
              <div><div className="f-mono text-[9px] uppercase text-[#64748b]">Leads</div><div className="f-display font-bold text-base text-[#FF9A3C]">{t[3]}</div></div>
              <div><div className="f-mono text-[9px] uppercase text-[#64748b]">Conv.</div><div className="f-display font-bold text-base text-[#39d353]">{t[4]}</div></div>
            </div>
          </div>
        ))}
        <button className="card-clip border border-dashed border-[rgba(255,107,0,0.35)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] flex flex-col items-center justify-center gap-2 min-h-[200px]">
          <div className="w-10 h-10 bg-[rgba(255,107,0,0.12)] btn-clip flex items-center justify-center"><I.Plus size={16} stroke="#FF9A3C"/></div>
          <div className="f-display font-bold text-xs text-[#FF9A3C]">NOVA PLANTILLA</div>
        </button>
      </div>
    </div>
  );
}

Object.assign(window, { AMGFactoryHub });
