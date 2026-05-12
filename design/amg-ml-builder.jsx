// amg-ml-builder.jsx — Micro-landing Builder: templates → sections → form → automations

// ─────────── Templates list ───────────
function AMGMLTemplates() {
  const tpls = [
    { name:'Lampisteria · Cita ràpida', sections:4, leads:142, conv:'12.4%', updated:'Avui', status:'PUBLISHED', color:'#FF6B00' },
    { name:'Fisio · Reserva online', sections:5, leads:98, conv:'9.8%', updated:'Ahir', status:'PUBLISHED', color:'#58a6ff' },
    { name:'Dental · Primera visita', sections:6, leads:67, conv:'14.2%', updated:'2d', status:'PUBLISHED', color:'#39d353' },
    { name:'Estètica · Reserva', sections:4, leads:0, conv:'—', updated:'5d', status:'DRAFT', color:'#f0b429' },
    { name:'Mecànic · Cita taller', sections:3, leads:0, conv:'—', updated:'7d', status:'DRAFT', color:'#FF9A3C' },
    { name:'Cafeteria · Loyalty', sections:5, leads:34, conv:'8.1%', updated:'10d', status:'ARCHIVED', color:'#94a3b8' },
  ];
  const tone = (s) => s==='PUBLISHED' ? 'success' : s==='DRAFT' ? 'warning' : 'neutral';
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="factory"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="micro-landings · plantilles" subtitle="Editor de plantilles · seccions · formulari · automatitzacions">
          <AMGButton variant="outline" size="sm" icon={I.Download}>EXPORTAR</AMGButton>
          <AMGButton size="sm" icon={I.Plus}>NOVA PLANTILLA</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-auto amg-grid p-8 space-y-5">
          <div className="grid grid-cols-4 gap-3">
            <AMGStat label="Plantilles" value="6" tone="accent" icon={I.Layers}/>
            <AMGStat label="Publicades" value="3" tone="success" icon={I.Globe}/>
            <AMGStat label="Leads (30d)" value="341" delta="+48" tone="info" icon={I.Users}/>
            <AMGStat label="Conv. mitjana" value="11.2%" delta="+1.4%" tone="accent" icon={I.Trending}/>
          </div>

          <div className="grid grid-cols-3 gap-4">
            {tpls.map((t,i) => (
              <div key={i} className="amg-card card-clip overflow-hidden hover:border-l-2 hover:border-l-[#FF6B00] cursor-pointer transition group">
                <div className="h-28 relative overflow-hidden" style={{background:`linear-gradient(135deg, ${t.color}33 0%, ${t.color}11 100%)`}}>
                  <div className="absolute inset-0 amg-grid-sm opacity-40"></div>
                  <div className="absolute top-3 left-3"><AMGBadge tone={tone(t.status)}>{t.status}</AMGBadge></div>
                  <div className="absolute bottom-3 left-4">
                    <div className="f-display font-black text-2xl" style={{color:t.color}}>{t.name.split('·')[0].trim()}</div>
                    <div className="f-mono text-[9px] text-[#94a3b8] uppercase tracking-[0.2em]">{t.name.split('·')[1]?.trim()}</div>
                  </div>
                </div>
                <div className="p-4 grid grid-cols-3 gap-2 border-t border-[rgba(255,107,0,0.12)]">
                  <div>
                    <div className="f-mono text-[9px] uppercase text-[#64748b]">Seccions</div>
                    <div className="f-display font-bold text-lg text-[#e2e8f0]">{t.sections}</div>
                  </div>
                  <div>
                    <div className="f-mono text-[9px] uppercase text-[#64748b]">Leads</div>
                    <div className="f-display font-bold text-lg text-[#FF9A3C]">{t.leads}</div>
                  </div>
                  <div>
                    <div className="f-mono text-[9px] uppercase text-[#64748b]">Conversió</div>
                    <div className="f-display font-bold text-lg text-[#39d353]">{t.conv}</div>
                  </div>
                </div>
                <div className="px-4 py-2 flex items-center justify-between border-t border-[rgba(226,232,240,0.04)]">
                  <span className="f-mono text-[10px] text-[#64748b]">Actualitzat · {t.updated}</span>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition">
                    <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
                    <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Globe size={12}/></button>
                    <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={12}/></button>
                  </div>
                </div>
              </div>
            ))}

            {/* New template card */}
            <button className="card-clip border border-dashed border-[rgba(255,107,0,0.35)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.04)] flex flex-col items-center justify-center gap-3 min-h-[260px] transition">
              <div className="w-12 h-12 bg-[rgba(255,107,0,0.12)] btn-clip flex items-center justify-center">
                <I.Plus size={20} stroke="#FF9A3C"/>
              </div>
              <div className="f-display font-bold text-sm text-[#FF9A3C]">CREAR NOVA PLANTILLA</div>
              <div className="f-mono text-[10px] text-[#64748b] uppercase tracking-wider">Començar des de zero o duplicar una existent</div>
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── Builder · canvas + sections ───────────
function AMGMLBuilder() {
  const sections = [
    { id:'hero', kind:'HERO', title:'Lampisteria Roca', sub:'Reparem la teva caldera en 24h', locked:true },
    { id:'s1', kind:'TEXT', title:'Per què escollir-nos', sub:'Bloc de text · 3 columnes' },
    { id:'s2', kind:'IMAGE_TEXT', title:'El nostre equip', sub:'Imatge esquerra · text dreta' },
    { id:'s3', kind:'GALLERY', title:'Treballs realitzats', sub:'Galeria de 6 imatges' },
    { id:'s4', kind:'TESTIMONIAL', title:'Què diuen els clients', sub:'3 testimonis amb foto' },
    { id:'form', kind:'FORM', title:'Demana cita', sub:'Formulari · 4 camps · automatitzacions actives', locked:true },
  ];
  const sectionIcon = { HERO:I.Sparkles, TEXT:I.Edit, IMAGE_TEXT:I.Image, GALLERY:I.Layers, TESTIMONIAL:I.Users, FORM:I.Mail };
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="factory"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="builder · lampisteria · cita ràpida" subtitle="Editor de plantilla · 6 seccions · v1.2 · DRAFT">
          <AMGButton variant="ghost" size="sm" icon={I.Activity}>HISTÒRIC</AMGButton>
          <AMGButton variant="outline" size="sm" icon={I.Play}>PREVIEW</AMGButton>
          <AMGButton size="sm" icon={I.Globe}>PUBLICAR</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-hidden flex">
          {/* Left — Sections list */}
          <div className="w-[300px] border-r border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/60 flex flex-col">
            <div className="p-4 border-b border-[rgba(255,107,0,0.12)] flex items-center justify-between">
              <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Seccions · 6</span>
              <span className="f-mono text-[9px] text-[#64748b]">DRAG TO REORDER</span>
            </div>
            <div className="flex-1 overflow-auto p-3 space-y-2">
              {sections.map((s,i) => {
                const Ic = sectionIcon[s.kind];
                const active = s.id === 's2';
                return (
                  <div key={s.id} className={`p-3 cursor-pointer transition border-l-2 ${active ? 'bg-[rgba(255,107,0,0.10)] border-l-[#FF6B00]' : 'bg-[#13132a] border-l-transparent hover:border-l-[rgba(255,107,0,0.3)]'}`}>
                    <div className="flex items-center gap-2">
                      <I.Menu size={12} stroke="#64748b"/>
                      <div className="w-6 h-6 bg-[rgba(255,107,0,0.12)] flex items-center justify-center">
                        <Ic size={12} stroke="#FF9A3C"/>
                      </div>
                      <span className="f-mono text-[10px] uppercase tracking-wider text-[#FF9A3C] flex-1">{s.kind}</span>
                      {s.locked && <I.Lock size={10} stroke="#64748b"/>}
                    </div>
                    <div className="text-[13px] font-semibold mt-2 text-[#e2e8f0] truncate">{s.title}</div>
                    <div className="f-mono text-[10px] text-[#64748b] truncate">{s.sub}</div>
                  </div>
                );
              })}

              <button className="w-full p-3 border border-dashed border-[rgba(255,107,0,0.3)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.05)] flex items-center justify-center gap-2 f-mono text-[10px] uppercase text-[#FF9A3C]">
                <I.Plus size={12}/>AFEGIR SECCIÓ
              </button>
            </div>
            <div className="p-3 border-t border-[rgba(255,107,0,0.12)]">
              <div className="f-mono text-[9px] uppercase text-[#64748b] mb-2 px-2">Tipus disponibles</div>
              <div className="grid grid-cols-3 gap-1">
                {['HERO','TEXT','IMAGE','GALLERY','VIDEO','CTA','PRICING','FAQ','FORM'].map((k,i) => (
                  <button key={i} className="aspect-square flex flex-col items-center justify-center gap-1 bg-[#13132a] border border-[rgba(255,107,0,0.12)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.05)]">
                    <span className="f-mono text-[8px] text-[#FF9A3C]">{k}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Canvas */}
          <div className="flex-1 overflow-auto amg-grid p-8">
            <div className="max-w-[640px] mx-auto space-y-3">
              {/* Hero (locked, preview) */}
              <div className="amg-card card-clip overflow-hidden border-l-2 border-l-[rgba(255,107,0,0.3)]">
                <div className="px-4 py-2 flex items-center justify-between bg-[rgba(255,107,0,0.06)] border-b border-[rgba(255,107,0,0.12)]">
                  <div className="flex items-center gap-2">
                    <I.Sparkles size={11} stroke="#FF9A3C"/>
                    <span className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#FF9A3C]">SECCIÓ 01 · HERO</span>
                    <I.Lock size={9} stroke="#64748b"/>
                    <span className="f-mono text-[9px] text-[#64748b] uppercase">obligatòria</span>
                  </div>
                </div>
                <div className="p-8 amg-grid-sm">
                  <div className="text-[11px] f-mono text-[#FF9A3C] uppercase tracking-[0.2em]">Lampisteria · Mataró</div>
                  <div className="f-display font-black text-2xl mt-2 leading-tight">REPAREM LA TEVA<br/><span className="text-[#FF9A3C]">CALDERA EN 24H.</span></div>
                  <div className="text-[12px] text-[#94a3b8] mt-2">+ de 12 anys d'experiència · Garantia 2 anys</div>
                  <div className="flex gap-2 mt-4">
                    <button className="px-4 h-8 bg-[#FF6B00] text-black f-mono text-[10px] font-bold uppercase btn-clip">DEMANAR CITA</button>
                    <button className="px-4 h-8 border border-[#FF9A3C] text-[#FF9A3C] f-mono text-[10px] uppercase btn-clip">TRUCAR ARA</button>
                  </div>
                </div>
              </div>

              {/* Custom · Text */}
              <div className="amg-card card-clip overflow-hidden">
                <div className="px-4 py-2 flex items-center justify-between bg-[rgba(255,107,0,0.04)] border-b border-[rgba(255,107,0,0.12)]">
                  <div className="flex items-center gap-2">
                    <I.Edit size={11} stroke="#FF9A3C"/>
                    <span className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#FF9A3C]">SECCIÓ 02 · TEXT</span>
                  </div>
                  <div className="flex gap-1">
                    <button className="w-6 h-6 text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={10}/></button>
                    <button className="w-6 h-6 text-[#94a3b8] hover:text-[#ff6666]"><I.Trash size={10}/></button>
                  </div>
                </div>
                <div className="p-6">
                  <input className="w-full bg-transparent f-display font-black text-lg outline-none border-b border-dashed border-[rgba(255,107,0,0.2)] pb-1" defaultValue="PER QUÈ ESCOLLIR-NOS"/>
                  <div className="grid grid-cols-3 gap-3 mt-4">
                    {['Resposta < 2h','Garantia 2 anys','Pressupost gratuït'].map((t,i) => (
                      <div key={i} className="text-[11px] text-[#94a3b8] p-2 border border-dashed border-[rgba(255,107,0,0.2)]">{t}</div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Custom · Image+Text · ACTIVE */}
              <div className="card-clip overflow-hidden border-2 border-[#FF6B00] bg-[#13132a] shadow-[0_0_0_4px_rgba(255,107,0,0.1)]">
                <div className="px-4 py-2 flex items-center justify-between bg-[#FF6B00] border-b border-[rgba(255,107,0,0.12)]">
                  <div className="flex items-center gap-2">
                    <I.Image size={11} stroke="#000"/>
                    <span className="f-mono text-[9px] uppercase tracking-[0.2em] text-black font-bold">SECCIÓ 03 · IMAGE + TEXT · EDITANT</span>
                  </div>
                  <div className="flex gap-1">
                    <button className="w-6 h-6 text-black hover:bg-black/10"><I.Edit size={10}/></button>
                    <button className="w-6 h-6 text-black hover:bg-black/10"><I.Trash size={10}/></button>
                  </div>
                </div>
                <div className="p-6 grid grid-cols-2 gap-4">
                  <div className="amg-ph aspect-[4/3] flex items-center justify-center f-mono text-[10px] text-[#FF9A3C] uppercase">📷 IMATGE EQUIP</div>
                  <div>
                    <input className="w-full bg-transparent f-display font-bold text-base outline-none border-b border-dashed border-[rgba(255,107,0,0.3)] pb-1" defaultValue="EL NOSTRE EQUIP"/>
                    <textarea className="w-full bg-transparent text-[12px] text-[#94a3b8] outline-none border-b border-dashed border-[rgba(255,107,0,0.2)] mt-3 resize-none h-20" defaultValue="Som 4 lampistes amb més de 12 anys d'experiència. Tots certificats i amb garantia."/>
                  </div>
                </div>
              </div>

              {/* Custom · Gallery */}
              <div className="amg-card card-clip overflow-hidden">
                <div className="px-4 py-2 flex items-center justify-between bg-[rgba(255,107,0,0.04)] border-b border-[rgba(255,107,0,0.12)]">
                  <div className="flex items-center gap-2">
                    <I.Layers size={11} stroke="#FF9A3C"/>
                    <span className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#FF9A3C]">SECCIÓ 04 · GALLERY</span>
                  </div>
                </div>
                <div className="p-6 grid grid-cols-3 gap-2">
                  {[1,2,3,4,5,6].map(n => <div key={n} className="amg-ph aspect-square"></div>)}
                </div>
              </div>

              {/* Form (locked, with automations) */}
              <div className="card-clip overflow-hidden border-l-2 border-l-[#39d353] bg-[#13132a]">
                <div className="px-4 py-2 flex items-center justify-between bg-[rgba(57,211,83,0.08)] border-b border-[rgba(57,211,83,0.2)]">
                  <div className="flex items-center gap-2">
                    <I.Mail size={11} stroke="#39d353"/>
                    <span className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#39d353]">SECCIÓ FINAL · FORMULARI</span>
                    <I.Lock size={9} stroke="#64748b"/>
                  </div>
                  <span className="f-mono text-[9px] text-[#39d353]">3 AUTOMATITZACIONS ACTIVES</span>
                </div>
                <div className="p-6">
                  <div className="f-display font-black text-lg mb-3">DEMANA CITA</div>
                  <div className="space-y-2">
                    {[
                      ['Nom','text'],
                      ['Telèfon','tel'],
                      ['Què necessites?','select · 4 opcions'],
                      ['Missatge','textarea'],
                    ].map((f,i) => (
                      <div key={i} className="flex items-center gap-3 p-2.5 bg-[#0d0d1a] border border-[rgba(255,107,0,0.12)]">
                        <I.Menu size={10} stroke="#64748b"/>
                        <span className="text-[12px] flex-1">{f[0]}</span>
                        <span className="f-mono text-[9px] text-[#FF9A3C] uppercase">{f[1]}</span>
                        <button className="w-5 h-5 text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={9}/></button>
                      </div>
                    ))}
                    <button className="w-full p-2 border border-dashed border-[rgba(255,107,0,0.3)] f-mono text-[10px] uppercase text-[#FF9A3C] hover:bg-[rgba(255,107,0,0.05)]">
                      + AFEGIR CAMP
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right — Inspector */}
          <div className="w-[320px] border-l border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/60 flex flex-col">
            <div className="p-4 border-b border-[rgba(255,107,0,0.12)]">
              <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Inspector · Secció 03</span>
              <div className="f-display font-bold text-base mt-1">IMAGE + TEXT</div>
            </div>
            <div className="flex-1 overflow-auto p-4 space-y-5">
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Layout</div>
                <div className="grid grid-cols-3 gap-1">
                  {['IMG-L','IMG-R','IMG-TOP'].map((l,i) => (
                    <button key={i} className={`p-2 f-mono text-[9px] uppercase border ${i===0 ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.1)] text-[#FF9A3C]' : 'border-[rgba(255,107,0,0.12)] text-[#94a3b8] hover:border-[#FF9A3C]'}`}>{l}</button>
                  ))}
                </div>
              </div>
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Imatge</div>
                <div className="amg-ph p-4 flex items-center justify-center f-mono text-[10px] text-[#FF9A3C] uppercase">📷 PUJAR IMATGE</div>
                <div className="grid grid-cols-2 gap-1 mt-2">
                  <button className="px-2 h-7 f-mono text-[9px] uppercase bg-[#13132a] border border-[rgba(255,107,0,0.12)] hover:border-[#FF9A3C]">UPLOAD</button>
                  <button className="px-2 h-7 f-mono text-[9px] uppercase bg-[#13132a] border border-[rgba(255,107,0,0.12)] hover:border-[#FF9A3C]">URL</button>
                </div>
              </div>
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Títol</div>
                <input className="w-full bg-[#13132a] border border-[rgba(255,107,0,0.2)] px-3 h-9 text-sm" defaultValue="EL NOSTRE EQUIP"/>
              </div>
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Text</div>
                <textarea className="w-full bg-[#13132a] border border-[rgba(255,107,0,0.2)] px-3 py-2 text-sm h-24" defaultValue="Som 4 lampistes amb més de 12 anys..."/>
              </div>
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Botó (opcional)</div>
                <input className="w-full bg-[#13132a] border border-[rgba(255,107,0,0.2)] px-3 h-9 text-sm" placeholder="Etiqueta del botó"/>
              </div>
              <div>
                <div className="f-mono text-[9px] uppercase tracking-wider text-[#64748b] mb-2">Visibilitat</div>
                <div className="flex items-center justify-between p-2.5 bg-[#13132a] border border-[rgba(255,107,0,0.12)]">
                  <span className="text-[12px]">Mostrar secció</span>
                  <div className="w-9 h-5 bg-[#FF6B00] relative"><div className="absolute right-0.5 top-0.5 w-4 h-4 bg-black"></div></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── Form Automations ───────────
function AMGMLAutomations() {
  const flows = [
    { trigger:'FORM_SUBMIT', name:'Notificar al propietari', steps:[
      { kind:'EMAIL', detail:'A: marc@lampisteria-roca.cat · Subject: Nou lead { {nom} }', enabled:true },
    ], active:true },
    { trigger:'FORM_SUBMIT', name:'Confirmació al client', steps:[
      { kind:'EMAIL', detail:'A: { {email} } · Template: confirmacio_cita', enabled:true },
    ], active:true },
    { trigger:'FORM_SUBMIT', name:'WhatsApp al lead', steps:[
      { kind:'WHATSAPP', detail:'+34 678 234 *** · Template: hola_{nom}_v2', enabled:true },
    ], active:true },
    { trigger:'FORM_SUBMIT', name:'Avís intern Telegram', steps:[
      { kind:'TELEGRAM', detail:'Channel: @amg_leads · Bot: AMGNotifyBot', enabled:false },
    ], active:false },
  ];
  const triggers = ['FORM_SUBMIT','FIELD_CHANGED','TIME_DELAY','LEAD_QUALIFIED'];
  const actions = [
    { kind:'EMAIL', icon:I.Mail, color:'#58a6ff', label:'Email', desc:'Enviar correu (SMTP / Resend)' },
    { kind:'WHATSAPP', icon:I.Smartphone, color:'#39d353', label:'WhatsApp', desc:'Missatge via WhatsApp Business API' },
    { kind:'TELEGRAM', icon:I.Bot, color:'#58a6ff', label:'Telegram', desc:'Avís a canal o usuari' },
    { kind:'WEBHOOK', icon:I.Link, color:'#FF9A3C', label:'Webhook', desc:'POST a URL custom' },
    { kind:'SLACK', icon:I.Bell, color:'#FF9A3C', label:'Slack', desc:'Missatge a canal #leads' },
    { kind:'CRM', icon:I.Users, color:'#FF9A3C', label:'CRM', desc:'Crear lead a HubSpot/Pipedrive' },
    { kind:'CALENDAR', icon:I.Calendar, color:'#FF9A3C', label:'Calendar', desc:'Crear event Google Calendar' },
    { kind:'SMS', icon:I.Smartphone, color:'#FF9A3C', label:'SMS', desc:'SMS via Twilio' },
  ];
  const actionStyle = (k) => {
    const a = actions.find(x => x.kind === k);
    return a || { color:'#94a3b8', icon:I.Zap };
  };
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebarV2 active="factory"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbarV2 title="builder · automatitzacions" subtitle="Lampisteria · Cita ràpida · 4 automatitzacions configurades">
          <AMGButton variant="ghost" size="sm">VEURE LOGS</AMGButton>
          <AMGButton variant="outline" size="sm" icon={I.Play}>PROVAR ENVIAMENT</AMGButton>
          <AMGButton size="sm" icon={I.Check}>DESAR CANVIS</AMGButton>
        </AMGTopbarV2>
        <div className="flex-1 overflow-hidden flex">
          {/* Left — actions library */}
          <div className="w-[280px] border-r border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/60 flex flex-col">
            <div className="p-4 border-b border-[rgba(255,107,0,0.12)]">
              <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Trigger</span>
              <select className="w-full bg-[#13132a] border border-[rgba(255,107,0,0.2)] px-3 h-9 text-sm mt-2">
                {triggers.map((t,i) => <option key={i}>{t}</option>)}
              </select>
              <div className="f-mono text-[10px] text-[#64748b] mt-2">Es dispara quan un lead envia el formulari</div>
            </div>
            <div className="p-4 flex-1 overflow-auto">
              <div className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C] mb-3">Accions disponibles</div>
              <div className="space-y-2">
                {actions.map((a,i) => (
                  <div key={i} className="p-2.5 bg-[#13132a] border border-[rgba(255,107,0,0.12)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.05)] cursor-grab flex items-center gap-2.5 group">
                    <div className="w-8 h-8 flex items-center justify-center" style={{background:a.color+'22', border:`1px solid ${a.color}55`}}>
                      <a.icon size={13} stroke={a.color}/>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-[12px] font-semibold">{a.label}</div>
                      <div className="f-mono text-[9px] text-[#64748b] truncate">{a.desc}</div>
                    </div>
                    <I.Plus size={12} stroke="#64748b" className="opacity-0 group-hover:opacity-100"/>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Canvas */}
          <div className="flex-1 overflow-auto amg-grid p-8">
            {/* Trigger node */}
            <div className="max-w-[760px] mx-auto">
              <div className="card-clip p-5 bg-gradient-to-br from-[rgba(255,107,0,0.18)] to-[rgba(255,107,0,0.04)] border border-[#FF6B00]">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-[#FF6B00] flex items-center justify-center btn-clip">
                    <I.Zap size={18} stroke="#000"/>
                  </div>
                  <div>
                    <div className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">TRIGGER</div>
                    <div className="f-display font-black text-lg mt-0.5">QUAN UN LEAD ENVIA EL FORMULARI</div>
                    <div className="f-mono text-[10px] text-[#94a3b8] mt-1">FORM_SUBMIT · plantilla · Lampisteria · Cita ràpida · variables { '{nom}' } { '{telefon}' } { '{email}' } { '{missatge}' }</div>
                  </div>
                </div>
              </div>

              {/* Down arrow */}
              <div className="flex justify-center py-3">
                <div className="flex flex-col items-center gap-1">
                  <div className="w-px h-6 bg-[rgba(255,107,0,0.4)]"></div>
                  <I.ChevDown size={14} stroke="#FF6B00"/>
                  <span className="f-mono text-[9px] text-[#FF9A3C] uppercase tracking-wider">Executar 3 accions en paral·lel</span>
                </div>
              </div>

              {/* Action flows */}
              <div className="space-y-3">
                {flows.map((f,i) => {
                  const st = actionStyle(f.steps[0].kind);
                  const Ic = st.icon;
                  return (
                    <div key={i} className={`amg-card card-clip p-4 ${!f.active ? 'opacity-50' : ''}`}>
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 flex items-center justify-center shrink-0" style={{background:st.color+'22', border:`1px solid ${st.color}55`}}>
                          <Ic size={14} stroke={st.color}/>
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="f-mono text-[10px] uppercase tracking-[0.18em]" style={{color:st.color}}>{f.steps[0].kind}</span>
                            {f.active ? <AMGBadge tone="success">ACTIU</AMGBadge> : <AMGBadge tone="neutral">PAUSAT</AMGBadge>}
                          </div>
                          <div className="text-sm font-semibold mt-1">{f.name}</div>
                          <div className="f-mono text-[10px] text-[#64748b] mt-0.5 truncate">{f.steps[0].detail}</div>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <div className={`w-9 h-5 relative ${f.active ? 'bg-[#39d353]' : 'bg-[#212140]'}`}>
                            <div className={`absolute top-0.5 w-4 h-4 bg-black ${f.active ? 'right-0.5' : 'left-0.5'}`}></div>
                          </div>
                          <button className="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
                          <button className="w-8 h-8 flex items-center justify-center text-[#94a3b8] hover:text-[#ff6666]"><I.Trash size={12}/></button>
                        </div>
                      </div>
                    </div>
                  );
                })}

                {/* Add new */}
                <button className="w-full p-4 border border-dashed border-[rgba(255,107,0,0.3)] hover:border-[#FF6B00] hover:bg-[rgba(255,107,0,0.05)] flex items-center justify-center gap-2 f-mono text-xs uppercase text-[#FF9A3C]">
                  <I.Plus size={14}/>AFEGIR ACCIÓ · ARROSSEGA O FES CLIC
                </button>
              </div>

              {/* Recent runs */}
              <div className="mt-8">
                <div className="flex items-center justify-between mb-3">
                  <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Execucions recents · 24h</span>
                  <span className="f-mono text-[10px] text-[#39d353]">14 OK · 0 ERROR</span>
                </div>
                <div className="amg-card card-clip overflow-hidden">
                  {[
                    ['18:42','Maria P.','EMAIL → marc@…','✓ 142ms','#39d353'],
                    ['18:42','Maria P.','EMAIL → maria@…','✓ 89ms','#39d353'],
                    ['18:42','Maria P.','WHATSAPP → +34 678…','✓ 380ms','#39d353'],
                    ['16:18','Joan V.','EMAIL → marc@…','✓ 134ms','#39d353'],
                    ['16:18','Joan V.','WHATSAPP → +34 692…','✓ 412ms','#39d353'],
                    ['14:05','Pere F.','TELEGRAM (paused)','— skip','#64748b'],
                  ].map((r,i) => (
                    <div key={i} className="grid grid-cols-[60px_1fr_1.4fr_1fr] gap-3 px-4 py-2.5 f-mono text-[11px] border-b border-[rgba(226,232,240,0.04)] last:border-b-0">
                      <span className="text-[#64748b]">{r[0]}</span>
                      <span>{r[1]}</span>
                      <span className="text-[#94a3b8] truncate">{r[2]}</span>
                      <span style={{color:r[4]}} className="text-right">{r[3]}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

Object.assign(window, { AMGMLTemplates, AMGMLBuilder, AMGMLAutomations });
