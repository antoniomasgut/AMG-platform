// amg-clients.jsx — clients list, editor drawer, setup wizard

function AMGClientsList() {
  const clients = [
    ['Nebula Studio','nebula.cat','GROWTH','ACTIVO','success','€1.240','Marta R.','#FF6B00'],
    ['Cal Rovira','calrovira.com','STARTER','ACTIVO','success','€  490','Jordi P.','#58a6ff'],
    ['Fusta Vives','fustavives.cat','SCALE','ACTIVO','success','€2.880','Lídia V.','#39d353'],
    ['Sínia Coffee','sinia.coffee','STARTER','SUSPENDIDO','warning','€  290','Oriol M.','#f0b429'],
    ['Codi Blau','codiblau.dev','GROWTH','ACTIVO','success','€1.140','Aina S.','#FF9A3C'],
    ['Bru & Co','brulegal.com','GROWTH','ACTIVO','success','€  890','Pau B.','#ff6666'],
    ['Torrent Arq.','torrent.studio','SCALE','ACTIVO','success','€2.480','Marc T.','#58a6ff'],
    ['Llum Dental','llumdental.es','STARTER','CANCELADO','danger','€    0','Clara L.','#64748b'],
    ['Mango Digital','mangodig.io','GROWTH','ACTIVO','success','€1.040','Roger F.','#FF6B00'],
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebar active="clients"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Clientes" subtitle="48 clientes · 46 activos">
          <AMGButton variant="outline" size="sm" icon={I.Download}>EXPORTAR</AMGButton>
          <AMGButton size="sm" icon={I.Plus}>NUEVO CLIENTE</AMGButton>
        </AMGTopbar>
        <div className="flex-1 overflow-auto amg-grid p-8">
          {/* Filters */}
          <div className="flex items-center gap-3 mb-5">
            <div className="relative flex-1 max-w-md">
              <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#64748b]"/>
              <input placeholder="Buscar por nombre, dominio o email…"
                     className="w-full h-10 bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] pl-9 pr-3 text-sm outline-none focus:border-[#FF6B00]"/>
            </div>
            {[['PLAN','TODOS'],['ESTADO','ACTIVO'],['ORDEN','MRR ↓']].map(([l,v],i) => (
              <button key={i} className="h-10 px-3 f-mono text-xs uppercase bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex items-center gap-2 hover:border-[#FF6B00] transition">
                <span className="text-[#64748b]">{l}:</span>
                <span className="text-[#FF9A3C]">{v}</span>
                <I.ChevDown size={12} className="text-[#64748b]"/>
              </button>
            ))}
            <div className="flex-1"></div>
            <span className="f-mono text-xs text-[#64748b] uppercase">9 de 48 resultados</span>
          </div>

          {/* Table */}
          <div className="amg-card card-clip overflow-hidden">
            <div className="grid grid-cols-[40px_1.6fr_120px_140px_120px_100px_60px] gap-4 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
              <div><input type="checkbox" className="accent-[#FF6B00]"/></div>
              <div>Cliente</div><div>Plan</div><div>Estado</div><div>Responsable</div><div className="text-right">MRR</div><div></div>
            </div>
            {clients.map((c,i) => (
              <div key={i} className="grid grid-cols-[40px_1.6fr_120px_140px_120px_100px_60px] gap-4 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)] group">
                <input type="checkbox" className="accent-[#FF6B00]"/>
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 flex items-center justify-center f-display font-bold text-sm text-black shrink-0" style={{background: c[7]}}>{c[0][0]}</div>
                  <div className="min-w-0">
                    <div className="text-sm font-semibold truncate">{c[0]}</div>
                    <div className="f-mono text-[10px] text-[#64748b] truncate">{c[1]}</div>
                  </div>
                </div>
                <div><AMGBadge tone={c[2]==='SCALE' ? 'accent' : c[2]==='GROWTH' ? 'info' : 'neutral'}>{c[2]}</AMGBadge></div>
                <div>
                  <span className="inline-flex items-center gap-2">
                    <span className={`w-1.5 h-1.5 rounded-full ${c[4]==='success' ? 'bg-[#39d353]' : c[4]==='warning' ? 'bg-[#f0b429]' : 'bg-[#ff4444]'}`}></span>
                    <span className="f-mono text-xs uppercase tracking-wider">{c[3]}</span>
                  </span>
                </div>
                <div className="text-sm text-[#94a3b8]">{c[6]}</div>
                <div className="f-mono text-sm text-[#e2e8f0] text-right">{c[5]}</div>
                <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition">
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.Edit size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.More size={12}/></button>
                </div>
              </div>
            ))}
            <div className="flex items-center justify-between px-5 py-3 bg-[#0d0d1a]/50 border-t border-[rgba(255,107,0,0.12)]">
              <span className="f-mono text-[11px] text-[#64748b] uppercase">Mostrando 1–9 de 48</span>
              <div className="flex gap-1">
                {['‹','1','2','3','4','5','›'].map((p,i) => (
                  <button key={i} className={`w-8 h-8 f-mono text-xs flex items-center justify-center ${p==='1' ? 'bg-[#FF6B00] text-black' : 'bg-[#1a1a2e] text-[#94a3b8] hover:text-[#FF9A3C]'}`}>{p}</button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ─────────── Client editor drawer ───────────
function AMGClientEditor() {
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden relative">
      <AMGSidebar active="clients"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Clientes / Nebula Studio" subtitle="Editor de cliente">
          <AMGButton variant="ghost" size="sm">CANCELAR</AMGButton>
          <AMGButton size="sm" icon={I.Check}>GUARDAR</AMGButton>
        </AMGTopbar>
        {/* Dimmed list behind */}
        <div className="flex-1 overflow-hidden amg-grid relative">
          <div className="p-8 opacity-30 pointer-events-none">
            <div className="h-10 w-80 bg-[#1a1a2e] mb-4"></div>
            <div className="amg-card card-clip h-96"></div>
          </div>
          <div className="absolute inset-0 bg-black/60"></div>

          {/* Drawer */}
          <aside className="absolute top-0 right-0 bottom-0 w-[720px] bg-[#0d0d1a] border-l border-[rgba(255,107,0,0.2)] shadow-[0_0_60px_rgba(0,0,0,0.5)] flex flex-col">
            <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-6 gap-4">
              <div className="w-10 h-10 bg-[#FF6B00] flex items-center justify-center f-display font-black text-black">N</div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="f-display font-bold text-lg">Nebula Studio</span>
                  <AMGBadge tone="accent">GROWTH</AMGBadge>
                  <AMGBadge tone="success"><span className="w-1 h-1 rounded-full bg-[#39d353]"></span>ACTIVO</AMGBadge>
                </div>
                <div className="f-mono text-[11px] text-[#64748b] uppercase">client_id · neb_7f3c_a104</div>
              </div>
              <button className="w-9 h-9 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.X size={16}/></button>
            </div>

            {/* Tabs */}
            <div className="flex border-b border-[rgba(255,107,0,0.12)] px-6">
              {['Datos empresa','Servicios','Landing','Credenciales','Facturación'].map((t,i) => (
                <button key={t} className={`px-4 h-12 f-mono text-xs uppercase tracking-wider relative ${i===0 ? 'text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>
                  {t}
                  {i===0 && <span className="absolute bottom-[-1px] left-0 right-0 h-[2px] bg-[#FF6B00]"></span>}
                </button>
              ))}
            </div>

            {/* Body */}
            <div className="flex-1 overflow-auto p-6 space-y-5">
              <div>
                <div className="f-mono text-[10px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Identidad</div>
                <div className="flex gap-4 items-start">
                  <div className="w-24 h-24 bg-[#1a1a2e] border border-[rgba(255,107,0,0.2)] flex flex-col items-center justify-center gap-1 cursor-pointer hover:border-[#FF6B00] transition shrink-0">
                    <I.Upload size={16} stroke="#FF9A3C"/>
                    <span className="f-mono text-[9px] uppercase text-[#94a3b8]">Logo</span>
                    <span className="f-mono text-[9px] text-[#64748b]">PNG · 512px</span>
                  </div>
                  <div className="flex-1 grid grid-cols-2 gap-3">
                    <AMGInput label="Nombre comercial" value="Nebula Studio"/>
                    <AMGInput label="Razón social" value="Nebula Studio SL"/>
                    <AMGInput label="CIF" value="B-87 234 561" mono/>
                    <AMGInput label="Email contacto" value="hola@nebula.cat" icon={I.Mail}/>
                  </div>
                </div>
              </div>

              <div>
                <div className="f-mono text-[10px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Dominio</div>
                <div className="grid grid-cols-2 gap-3">
                  <AMGInput label="Dominio personalizado" value="nebula.cat" icon={I.Globe}/>
                  <AMGInput label="Slug landing" value="nebula-studio" mono icon={I.Link} hint="URL pública: amg.cat/l/nebula-studio"/>
                </div>
                <div className="mt-3 flex items-center gap-3 p-3 bg-[rgba(57,211,83,0.08)] border-l-2 border-l-[#39d353]">
                  <I.Check size={14} stroke="#39d353"/>
                  <span className="text-sm text-[#39d353]">Dominio verificado · DNS apuntando correctamente</span>
                  <span className="f-mono text-[10px] text-[#64748b] ml-auto">hace 3 días</span>
                </div>
              </div>

              <div>
                <div className="f-mono text-[10px] uppercase tracking-[0.18em] text-[#FF9A3C] mb-3">Plan & facturación</div>
                <div className="grid grid-cols-3 gap-2 mb-3">
                  {[
                    ['STARTER','€29','2 servicios'],
                    ['GROWTH','€79','5 servicios',true],
                    ['SCALE','€149','Ilimitado'],
                  ].map(([n,p,d,active],i) => (
                    <div key={i} className={`card-clip p-4 cursor-pointer transition ${active ? 'bg-[rgba(255,107,0,0.12)] border border-[#FF6B00]' : 'amg-card hover:border-[#FF6B00]'}`}>
                      <div className="flex items-center justify-between">
                        <span className="f-display font-bold text-sm">{n}</span>
                        {active && <I.Check size={12} stroke="#FF9A3C"/>}
                      </div>
                      <div className={`f-display font-bold text-xl mt-1 ${active ? 'text-[#FF9A3C]' : 'text-[#e2e8f0]'}`}>{p}</div>
                      <div className="f-mono text-[10px] text-[#64748b] uppercase">{d}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="border-t border-[rgba(255,107,0,0.12)] p-4 flex justify-between items-center bg-[#0d0d1a]">
              <button className="f-mono text-[11px] uppercase text-[#ff6666] hover:text-[#ff8888] flex items-center gap-2">
                <I.Trash size={12}/>ELIMINAR CLIENTE
              </button>
              <div className="flex gap-2">
                <AMGButton variant="secondary" size="sm">CANCELAR</AMGButton>
                <AMGButton size="sm" icon={I.Check}>GUARDAR CAMBIOS</AMGButton>
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}

// ─────────── Setup wizard ───────────
function AMGSetupWizard() {
  const steps = [
    { n:1, t:'Verificar', d:'Datos de empresa y dominio', done:true },
    { n:2, t:'Conexiones', d:'WhatsApp, Stripe, Calendar', done:true, active:false },
    { n:3, t:'Credenciales', d:'OAuth y API keys', done:false, active:true },
    { n:4, t:'Completado', d:'Publicar y activar', done:false },
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebar active="clients"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Clientes / Nebula / Setup" subtitle="Wizard de onboarding"/>
        <div className="flex-1 overflow-auto amg-grid p-10">
          <div className="max-w-4xl mx-auto">
            {/* Stepper */}
            <div className="flex items-center justify-between mb-10 relative">
              <div className="absolute top-6 left-8 right-8 h-[2px] bg-[rgba(255,107,0,0.15)]"></div>
              <div className="absolute top-6 left-8 h-[2px] bg-[#FF6B00]" style={{width:'48%'}}></div>
              {steps.map((s,i) => (
                <div key={i} className="relative z-10 flex flex-col items-center">
                  <div className={`w-12 h-12 flex items-center justify-center f-display font-bold border-2 transition
                    ${s.done ? 'bg-[#FF6B00] border-[#FF6B00] text-black'
                      : s.active ? 'bg-[#0d0d1a] border-[#FF6B00] text-[#FF9A3C]'
                      : 'bg-[#1a1a2e] border-[rgba(226,232,240,0.12)] text-[#64748b]'}`}>
                    {s.done ? <I.Check size={18}/> : s.n}
                  </div>
                  <div className={`mt-3 text-center ${s.active ? 'text-[#FF9A3C]' : s.done ? 'text-[#e2e8f0]' : 'text-[#64748b]'}`}>
                    <div className="f-display font-bold text-sm">{s.t.toUpperCase()}</div>
                    <div className="f-mono text-[10px] text-[#64748b] uppercase mt-0.5">{s.d}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Current step body */}
            <div className="amg-card card-clip p-8">
              <div className="flex items-start justify-between mb-5">
                <div>
                  <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Paso 3 de 4</span>
                  <h2 className="f-display font-bold text-2xl mt-1">CREDENCIALES DE SERVICIO</h2>
                  <p className="text-sm text-[#94a3b8] mt-1">Conecta las cuentas externas. Se almacenan cifradas (AES-256) y solo se usan para los workflows activos.</p>
                </div>
                <div className="f-mono text-[10px] text-[#64748b] uppercase">3/5 conectadas</div>
              </div>

              <div className="space-y-3">
                {[
                  ['WHATSAPP BUSINESS','Enviar y recibir mensajes automatizados','Bot',true,'#25D366'],
                  ['STRIPE','Cobros y suscripciones','CreditCard',true,'#635bff'],
                  ['GOOGLE CALENDAR','Reservas y recordatorios','Calendar',true,'#4285f4'],
                  ['NOTION','Base de conocimiento del bot','Layers',false,'#e2e8f0'],
                  ['GOOGLE DRIVE','Adjuntos y documentos','Box',false,'#34a853'],
                ].map(([n,d,ico,conn,col],i) => {
                  const Ic = I[ico];
                  return (
                    <div key={i} className={`flex items-center gap-4 p-4 border ${conn ? 'border-[rgba(57,211,83,0.35)] bg-[rgba(57,211,83,0.05)]' : 'border-[rgba(255,107,0,0.2)] bg-[#1a1a2e]'}`}>
                      <div className="w-10 h-10 flex items-center justify-center" style={{background: `${col}22`, color: col}}>
                        <Ic size={18}/>
                      </div>
                      <div className="flex-1">
                        <div className="f-display font-bold text-sm">{n}</div>
                        <div className="text-[13px] text-[#94a3b8]">{d}</div>
                      </div>
                      {conn ? (
                        <div className="flex items-center gap-3">
                          <span className="f-mono text-[11px] text-[#39d353] uppercase flex items-center gap-1.5">
                            <I.Check size={12}/>CONECTADO
                          </span>
                          <button className="f-mono text-[11px] uppercase text-[#64748b] hover:text-[#FF9A3C]">DESCONECTAR</button>
                        </div>
                      ) : (
                        <AMGButton size="sm" variant="outline">CONECTAR OAUTH</AMGButton>
                      )}
                    </div>
                  );
                })}
              </div>

              <div className="flex items-center justify-between mt-8 pt-5 border-t border-[rgba(226,232,240,0.05)]">
                <AMGButton variant="ghost">← ATRÁS</AMGButton>
                <div className="flex items-center gap-3">
                  <span className="f-mono text-[11px] text-[#64748b] uppercase">Puedes completar esto después</span>
                  <AMGButton variant="secondary" size="sm">SALTAR</AMGButton>
                  <AMGButton size="sm" icon={I.ArrowRight}>CONTINUAR</AMGButton>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

Object.assign(window, { AMGClientsList, AMGClientEditor, AMGSetupWizard });
