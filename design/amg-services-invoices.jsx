// amg-services-invoices.jsx — services catalog, invoices list, new invoice modal

function AMGServicesCatalog() {
  const services = [
    { name:'WhatsApp Bot AI', icon:'Bot', cat:'IA', setup:149, month:49, active:true, desc:'Respuestas 24/7 con tu documentación' },
    { name:'Landing auto', icon:'Globe', cat:'Web', setup:99, month:19, active:true, desc:'Página generada por IA a partir de un brief' },
    { name:'Workflow engine', icon:'Zap', cat:'Auto', setup:199, month:29, active:true, desc:'Conectar servicios sin código' },
    { name:'Facturación', icon:'Receipt', cat:'Admin', setup:49, month:19, active:true, desc:'Emisión + envío + Stripe' },
    { name:'Reservas calendario', icon:'Calendar', cat:'Auto', setup:79, month:15, active:true, desc:'Booking sync con Google Calendar' },
    { name:'CRM ligero', icon:'Users', cat:'Admin', setup:99, month:25, active:false, desc:'Leads, etapas, notas' },
    { name:'Analytics panel', icon:'Trending', cat:'Data', setup:49, month:12, active:true, desc:'KPIs de bot, landing y ventas' },
    { name:'Email transactional', icon:'Mail', cat:'Comm.', setup:29, month:9, active:true, desc:'Postmark integrado' },
    { name:'Portal cliente', icon:'Shield', cat:'Web', setup:0, month:0, active:true, desc:'Incluido con todos los planes' },
    { name:'SEO técnico', icon:'Search', cat:'Web', setup:199, month:39, active:false, desc:'Schema, sitemap, Core Web Vitals' },
    { name:'SMS alerts', icon:'Smartphone', cat:'Comm.', setup:19, month:7, active:false, desc:'Twilio / MessageBird' },
    { name:'Hosting landing', icon:'Layers', cat:'Web', setup:0, month:5, active:true, desc:'CDN global + SSL auto' },
  ];
  const cats = ['TODOS','IA','WEB','AUTO','ADMIN','COMM.','DATA'];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebar active="services"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Servicios" subtitle="Catálogo · 12 servicios">
          <AMGButton size="sm" icon={I.Plus}>NUEVO SERVICIO</AMGButton>
        </AMGTopbar>
        <div className="flex-1 overflow-auto amg-grid p-8">
          <div className="flex items-center gap-2 mb-6 border-b border-[rgba(255,107,0,0.12)]">
            {cats.map((c,i) => (
              <button key={c} className={`px-4 h-10 f-mono text-xs uppercase tracking-wider relative ${i===0 ? 'text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>
                {c}
                {i===0 && <span className="absolute bottom-[-1px] left-0 right-0 h-[2px] bg-[#FF6B00]"></span>}
              </button>
            ))}
            <div className="flex-1"></div>
            <span className="f-mono text-[11px] text-[#64748b] uppercase pb-2">9 activos · 3 inactivos</span>
          </div>

          <div className="grid grid-cols-4 gap-4">
            {services.map((s,i) => {
              const Ic = I[s.icon];
              return (
                <div key={i} className={`amg-card card-clip p-5 flex flex-col transition ${s.active ? 'border-l-2 border-l-[#FF6B00]' : 'opacity-60'}`}>
                  <div className="flex items-start justify-between mb-3">
                    <div className="w-11 h-11 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center">
                      <Ic size={18} stroke="#FF9A3C"/>
                    </div>
                    <div className={`relative inline-flex h-5 w-9 items-center rounded-full ${s.active ? 'bg-[#FF6B00]' : 'bg-[#212140]'}`}>
                      <span className={`block h-4 w-4 rounded-full bg-black ${s.active ? 'ml-[18px]' : 'ml-0.5 bg-[#64748b]'}`}></span>
                    </div>
                  </div>
                  <div className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#64748b]">{s.cat}</div>
                  <div className="f-display font-bold text-sm mt-1">{s.name.toUpperCase()}</div>
                  <div className="text-[12px] text-[#94a3b8] mt-1 flex-1">{s.desc}</div>
                  <div className="flex items-baseline gap-3 mt-3 pt-3 border-t border-[rgba(226,232,240,0.05)]">
                    <div>
                      <div className="f-mono text-[9px] uppercase text-[#64748b]">Setup</div>
                      <div className="f-mono text-sm text-[#e2e8f0]">€{s.setup}</div>
                    </div>
                    <div>
                      <div className="f-mono text-[9px] uppercase text-[#64748b]">/ mes</div>
                      <div className={`f-display font-bold text-base ${s.active ? 'text-[#FF9A3C]' : 'text-[#94a3b8]'}`}>€{s.month}</div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </main>
    </div>
  );
}

function AMGInvoicesList() {
  const invs = [
    ['2026-0114','Nebula Studio','12 Abr 2026','€1.240,00','PAID','success'],
    ['2026-0113','Cal Rovira','11 Abr 2026','€  490,00','PENDING','warning'],
    ['2026-0112','Fusta Vives','10 Abr 2026','€2.880,00','PAID','success'],
    ['2026-0111','Sínia Coffee','08 Abr 2026','€  290,00','PENDING','warning'],
    ['2026-0110','Codi Blau','05 Abr 2026','€1.140,00','PAID','success'],
    ['2026-0109','Bru & Co','03 Abr 2026','€  690,00','CANCELLED','danger'],
    ['2026-0108','Torrent Arq.','01 Abr 2026','€2.480,00','PAID','success'],
    ['2026-0107','Mango Digital','28 Mar 2026','€1.040,00','PAID','success'],
    ['2026-0106','Nebula Studio','15 Mar 2026','€1.240,00','PAID','success'],
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebar active="invoices"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Facturas" subtitle="Abril 2026 · 114 facturas">
          <AMGButton variant="outline" size="sm" icon={I.Download}>EXPORTAR XLS</AMGButton>
          <AMGButton size="sm" icon={I.Plus}>NUEVA FACTURA</AMGButton>
        </AMGTopbar>
        <div className="flex-1 overflow-auto amg-grid p-8">
          <div className="grid grid-cols-4 gap-4 mb-6">
            <AMGStat label="Total emitido" value="€28.4K" delta="+12%" tone="accent" icon={I.Trending}/>
            <AMGStat label="Pagadas" value="107" delta="+14" tone="success" icon={I.Check}/>
            <AMGStat label="Pendientes" value="7" tone="warning" icon={I.Clock}/>
            <AMGStat label="Vencidas" value="2" delta="−1" tone="danger" icon={I.Bell}/>
          </div>

          <div className="flex items-center gap-3 mb-4">
            <div className="relative flex-1 max-w-md">
              <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#64748b]"/>
              <input placeholder="Buscar número o cliente…" className="w-full h-10 bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] pl-9 pr-3 text-sm outline-none focus:border-[#FF6B00]"/>
            </div>
            {[['ESTADO','TODOS'],['PERIODO','ABR 2026']].map(([l,v],i) => (
              <button key={i} className="h-10 px-3 f-mono text-xs uppercase bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex items-center gap-2">
                <span className="text-[#64748b]">{l}:</span>
                <span className="text-[#FF9A3C]">{v}</span>
                <I.ChevDown size={12} className="text-[#64748b]"/>
              </button>
            ))}
          </div>

          <div className="amg-card card-clip overflow-hidden">
            <div className="grid grid-cols-[120px_1.4fr_140px_140px_130px_120px] gap-4 px-5 py-3 f-mono text-[10px] uppercase tracking-[0.18em] text-[#64748b] border-b border-[rgba(255,107,0,0.12)] bg-[#0d0d1a]/50">
              <div>Número</div><div>Cliente</div><div>Fecha</div><div className="text-right">Importe</div><div>Estado</div><div className="text-right">Acciones</div>
            </div>
            {invs.map((r,i) => (
              <div key={i} className="grid grid-cols-[120px_1.4fr_140px_140px_130px_120px] gap-4 px-5 py-3 items-center border-b border-[rgba(226,232,240,0.04)] hover:bg-[rgba(255,107,0,0.04)] group">
                <span className="f-mono text-sm text-[#FF9A3C]">#{r[0]}</span>
                <span className="text-sm">{r[1]}</span>
                <span className="f-mono text-xs text-[#94a3b8]">{r[2]}</span>
                <span className="f-mono text-sm text-[#e2e8f0] text-right">{r[3]}</span>
                <span><AMGBadge tone={r[5]}>{r[4]}</AMGBadge></span>
                <div className="flex items-center justify-end gap-1">
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]" title="Descargar PDF"><I.Download size={12}/></button>
                  <button className="w-7 h-7 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]" title="Enviar email"><I.Mail size={12}/></button>
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

function AMGInvoiceNew() {
  const lines = [
    ['WhatsApp Bot AI · GROWTH','1','€49,00','€49,00'],
    ['Landing auto','1','€19,00','€19,00'],
    ['Workflow engine · 3 workflows','3','€29,00','€87,00'],
    ['Setup inicial (una vez)','1','€149,00','€149,00'],
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden relative">
      <AMGSidebar active="invoices"/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Facturas / Nueva" subtitle="Crear factura"/>
        <div className="flex-1 overflow-hidden amg-grid relative">
          <div className="absolute inset-0 bg-black/60"></div>
          <div className="absolute inset-0 flex items-center justify-center p-8">
            <div className="amg-card card-clip w-[880px] max-h-[92%] flex flex-col">
              <div className="flex items-center justify-between p-6 border-b border-[rgba(255,107,0,0.12)]">
                <div>
                  <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Nueva factura</span>
                  <h2 className="f-display font-bold text-xl mt-1">FACTURA #2026-0115</h2>
                </div>
                <button className="w-9 h-9 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C]"><I.X size={16}/></button>
              </div>

              <div className="flex-1 overflow-auto p-6 space-y-5">
                <div className="grid grid-cols-3 gap-3">
                  <div className="col-span-1">
                    <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">Cliente</span>
                    <div className="h-10 bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex items-center px-3 gap-3">
                      <div className="w-6 h-6 bg-[#FF6B00] flex items-center justify-center text-[10px] f-display font-bold text-black">N</div>
                      <span className="text-sm flex-1">Nebula Studio</span>
                      <I.ChevDown size={12} className="text-[#64748b]"/>
                    </div>
                  </div>
                  <AMGInput label="Fecha emisión" value="12 / 04 / 2026" mono icon={I.Calendar}/>
                  <AMGInput label="Vencimiento" value="12 / 05 / 2026" mono icon={I.Calendar}/>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="f-mono uppercase text-[10px] tracking-[0.18em] text-[#FF9A3C]">Líneas · Servicios</span>
                    <button className="f-mono text-[11px] uppercase text-[#FF9A3C] flex items-center gap-1.5"><I.Plus size={12}/>AÑADIR LÍNEA</button>
                  </div>
                  <div className="border border-[rgba(255,107,0,0.14)]">
                    <div className="grid grid-cols-[1fr_80px_120px_120px_40px] gap-2 px-3 py-2 f-mono text-[10px] uppercase text-[#64748b] bg-[#0d0d1a]">
                      <div>Descripción</div><div>Cant.</div><div>Precio</div><div className="text-right">Total</div><div></div>
                    </div>
                    {lines.map((l,i) => (
                      <div key={i} className="grid grid-cols-[1fr_80px_120px_120px_40px] gap-2 px-3 py-2.5 items-center border-t border-[rgba(226,232,240,0.04)]">
                        <span className="text-sm">{l[0]}</span>
                        <span className="f-mono text-sm">{l[1]}</span>
                        <span className="f-mono text-sm text-[#94a3b8]">{l[2]}</span>
                        <span className="f-mono text-sm text-right">{l[3]}</span>
                        <button className="text-[#64748b] hover:text-[#ff6666]"><I.Trash size={12}/></button>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6 pt-3">
                  <div>
                    <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">Notas</span>
                    <textarea rows="4" defaultValue="Gracias por tu confianza. Esta factura se cobra automáticamente vía Stripe."
                              className="w-full bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] p-3 text-sm outline-none resize-none"/>
                  </div>
                  <div className="space-y-1 f-mono">
                    <div className="flex justify-between text-sm text-[#94a3b8]"><span>Subtotal</span><span>€304,00</span></div>
                    <div className="flex justify-between text-sm text-[#94a3b8]"><span>IVA · 21%</span><span>€63,84</span></div>
                    <div className="flex justify-between text-sm text-[#94a3b8]"><span>Descuento</span><span className="text-[#39d353]">−€0,00</span></div>
                    <div className="border-t border-[rgba(255,107,0,0.2)] mt-2 pt-2 flex justify-between items-baseline">
                      <span className="f-display font-bold">TOTAL</span>
                      <span className="f-display font-black text-2xl text-[#FF9A3C]">€367,84</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="border-t border-[rgba(255,107,0,0.12)] p-4 flex justify-between items-center">
                <label className="flex items-center gap-2 cursor-pointer">
                  <span className="w-4 h-4 bg-[#FF6B00] flex items-center justify-center"><I.Check size={10} stroke="#000"/></span>
                  <span className="text-sm">Enviar por email al cliente al confirmar</span>
                </label>
                <div className="flex gap-2">
                  <AMGButton variant="secondary" size="sm">GUARDAR BORRADOR</AMGButton>
                  <AMGButton variant="outline" size="sm" icon={I.Download}>PREVIEW PDF</AMGButton>
                  <AMGButton size="sm" icon={I.Check}>CONFIRMAR & EMITIR</AMGButton>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

Object.assign(window, { AMGServicesCatalog, AMGInvoicesList, AMGInvoiceNew });
