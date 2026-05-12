// amg-admin.jsx — sidebar shell + admin dashboard

function AMGSidebar({ active = 'dashboard', collapsed = false }) {
  const items = [
    { id:'dashboard', label:'Dashboard', icon:I.Dashboard },
    { id:'clients', label:'Clientes', icon:I.Users, badge:'48' },
    { id:'services', label:'Servicios', icon:I.Box },
    { id:'invoices', label:'Facturas', icon:I.Receipt, badge:'7', tone:'warning' },
    { id:'activity', label:'Actividad', icon:I.Activity },
    { id:'settings', label:'Configuración', icon:I.Settings },
  ];
  const w = collapsed ? 'w-[72px]' : 'w-[240px]';
  return (
    <aside className={`${w} shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex flex-col transition-all`}>
      {/* Brand */}
      <div className={`h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center ${collapsed ? 'justify-center' : 'px-5'} gap-3`}>
        <div className="w-9 h-9 bg-[#FF6B00] btn-clip flex items-center justify-center shrink-0">
          <span className="f-display font-black text-black text-sm">A</span>
        </div>
        {!collapsed && (
          <div className="flex flex-col leading-tight">
            <span className="f-display font-black text-sm tracking-[0.15em]">AMG</span>
            <span className="f-mono text-[9px] text-[#FF9A3C] tracking-[0.2em]">ENGINYERIA DIG.</span>
          </div>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 p-3 space-y-1">
        {!collapsed && <div className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#64748b] px-3 py-2">Navegación</div>}
        {items.map(it => {
          const isActive = it.id === active;
          return (
            <a key={it.id} href="#"
               className={`relative flex items-center gap-3 ${collapsed ? 'justify-center px-0' : 'px-3'} h-10 f-mono text-xs uppercase tracking-wider transition
                 ${isActive
                   ? 'bg-[rgba(255,107,0,0.10)] text-[#FF9A3C]'
                   : 'text-[#94a3b8] hover:text-[#e2e8f0] hover:bg-[rgba(255,255,255,0.03)]'}`}>
              {isActive && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
              <it.icon size={16}/>
              {!collapsed && <span className="flex-1 text-left">{it.label}</span>}
              {!collapsed && it.badge && (
                <span className={`f-mono text-[9px] px-1.5 h-4 flex items-center ${it.tone==='warning' ? 'bg-[rgba(240,180,41,0.15)] text-[#f0b429]' : 'bg-[#212140] text-[#94a3b8]'}`}>
                  {it.badge}
                </span>
              )}
            </a>
          );
        })}
      </nav>

      {/* Footer */}
      <div className={`border-t border-[rgba(255,107,0,0.12)] ${collapsed ? 'p-3' : 'p-4'}`}>
        <div className={`flex items-center gap-3 ${collapsed ? 'justify-center' : ''}`}>
          <div className="w-9 h-9 bg-gradient-to-br from-[#FF6B00] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs shrink-0">AM</div>
          {!collapsed && (
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold truncate">Antonio Mas</div>
              <div className="flex items-center gap-1.5 mt-0.5">
                <AMGBadge tone="accent" className="!h-4 !text-[9px] !px-1.5">ADMIN</AMGBadge>
                <span className="f-mono text-[10px] text-[#64748b] truncate">antonio@amg.cat</span>
              </div>
            </div>
          )}
        </div>      </div>
    </aside>
  );
}

function AMGTopbar({ title, subtitle, children }) {
  return (
    <div className="h-16 shrink-0 border-b border-[rgba(255,107,0,0.12)] flex items-center px-8 gap-5 bg-[#0d0d1a]/80 backdrop-blur">
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <span className="f-mono text-[10px] uppercase text-[#FF9A3C] tracking-[0.2em]">/ admin /</span>
          <span className="f-mono text-[10px] uppercase text-[#94a3b8] tracking-[0.2em]">{title}</span>
        </div>
        {subtitle && <div className="f-display font-bold text-lg leading-tight mt-0.5">{subtitle}</div>}
      </div>
      <div className="relative w-72">
        <I.Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#64748b]"/>
        <input placeholder="Buscar cliente, factura, servicio…"
               className="w-full h-9 bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] pl-9 pr-16 text-sm outline-none focus:border-[#FF6B00]"/>
        <span className="absolute right-2 top-1/2 -translate-y-1/2 f-mono text-[10px] text-[#64748b] border border-[rgba(226,232,240,0.1)] px-1.5 py-0.5">⌘K</span>
      </div>
      <button className="relative w-9 h-9 flex items-center justify-center text-[#94a3b8] hover:text-[#FF9A3C] bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)]">
        <I.Bell size={16}/>
        <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-[#FF6B00] rounded-full"></span>
      </button>
      {children}
    </div>
  );
}

// ─────────── Admin Dashboard ───────────
function AMGAdminDashboard({ collapsed = false }) {
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      <AMGSidebar active="dashboard" collapsed={collapsed}/>
      <main className="flex-1 flex flex-col min-w-0">
        <AMGTopbar title="Dashboard" subtitle="Vista general del sistema">
          <AMGButton size="sm" icon={I.Plus}>NUEVO CLIENTE</AMGButton>
        </AMGTopbar>
        <div className="flex-1 overflow-auto amg-grid">
          <div className="p-8 space-y-6">
            {/* Hero row */}
            <div className="grid grid-cols-4 gap-4">
              <AMGStat label="Clientes activos" value="48" delta="+3" tone="success" icon={I.Users}/>
              <AMGStat label="MRR · mensual" value="€12.4K" delta="+8.2%" tone="accent" icon={I.Trending}/>
              <AMGStat label="Facturas pendientes" value="7" delta="−2" tone="warning" icon={I.Receipt}/>
              <AMGStat label="Servicios activos" value="126" delta="+11" tone="info" icon={I.Box}/>
            </div>

            {/* Chart + recent clients */}
            <div className="grid grid-cols-3 gap-4">
              <div className="col-span-2 amg-card card-clip p-5">
                <AMGSectionTitle eyebrow="Ingresos" title="MRR · últimos 12 meses">
                  <div className="flex gap-1 f-mono text-[10px] uppercase">
                    {['6m','12m','TODO'].map((p,i) => (
                      <button key={p} className={`px-2 h-6 ${i===1 ? 'bg-[rgba(255,107,0,0.12)] text-[#FF9A3C] border border-[rgba(255,107,0,0.35)]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>{p}</button>
                    ))}
                  </div>
                </AMGSectionTitle>
                <AMGRevenueChart/>
              </div>

              <div className="amg-card card-clip p-5">
                <AMGSectionTitle eyebrow="Actividad" title="Últimos clientes">
                  <a href="#" className="f-mono text-[10px] uppercase text-[#FF9A3C]">VER TODOS →</a>
                </AMGSectionTitle>
                <div className="space-y-2.5 mt-2">
                  {[
                    ['Nebula Studio','GROWTH','hace 2h','#FF6B00'],
                    ['Cal Rovira','STARTER','hace 5h','#58a6ff'],
                    ['Fusta Vives','SCALE','ayer','#39d353'],
                    ['Sínia Coffee','STARTER','ayer','#f0b429'],
                    ['Codi Blau','GROWTH','hace 2d','#FF9A3C'],
                  ].map((c,i) => (
                    <div key={i} className="flex items-center gap-3">
                      <div className="w-8 h-8 flex items-center justify-center f-display font-bold text-xs text-black" style={{background: c[3]}}>{c[0][0]}</div>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold truncate">{c[0]}</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase">{c[1]} · {c[2]}</div>
                      </div>
                      <AMGBadge tone="success" className="!h-5"><span className="w-1 h-1 rounded-full bg-[#39d353]"></span></AMGBadge>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Invoices + quick actions */}
            <div className="grid grid-cols-3 gap-4">
              <div className="col-span-2 amg-card card-clip p-5">
                <AMGSectionTitle eyebrow="Facturación" title="Últimas facturas">
                  <AMGButton variant="ghost" size="sm" icon={I.Download}>EXPORTAR CSV</AMGButton>
                </AMGSectionTitle>
                <div className="space-y-0 -mx-2">
                  <div className="grid grid-cols-[80px_1fr_100px_100px_24px] gap-3 px-2 py-2 f-mono text-[10px] uppercase tracking-wider text-[#64748b] border-b border-[rgba(226,232,240,0.05)]">
                    <span>Núm.</span><span>Cliente</span><span>Importe</span><span>Estado</span><span></span>
                  </div>
                  {[
                    ['2026-0114','Nebula Studio','€1.240','PAID','success'],
                    ['2026-0113','Cal Rovira','€  490','PENDING','warning'],
                    ['2026-0112','Fusta Vives','€2.880','PAID','success'],
                    ['2026-0111','Sínia Coffee','€  290','PENDING','warning'],
                    ['2026-0110','Codi Blau','€1.140','PAID','success'],
                    ['2026-0109','Bru & Co','€  690','CANCELLED','danger'],
                  ].map((r,i) => (
                    <div key={i} className="grid grid-cols-[80px_1fr_100px_100px_24px] gap-3 px-2 h-11 items-center text-sm hover:bg-[rgba(255,107,0,0.04)] border-b border-[rgba(226,232,240,0.04)]">
                      <span className="f-mono text-[#FF9A3C] text-xs">#{r[0]}</span>
                      <span>{r[1]}</span>
                      <span className="f-mono text-[#e2e8f0]">{r[2]}</span>
                      <span><AMGBadge tone={r[4]}>{r[3]}</AMGBadge></span>
                      <button className="text-[#64748b] hover:text-[#FF9A3C]"><I.More size={14}/></button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-4">
                <div className="amg-card card-clip p-5">
                  <AMGSectionTitle eyebrow="Atajos" title="Acciones rápidas"/>
                  <div className="space-y-2">
                    <button className="w-full flex items-center gap-3 p-3 bg-[#1a1a2e] hover:bg-[#212140] border-l-2 border-l-[#FF6B00] text-left transition">
                      <I.Plus size={14} className="text-[#FF9A3C]"/>
                      <div className="flex-1">
                        <div className="f-display text-sm font-bold">Nuevo cliente</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase">Wizard de 4 pasos</div>
                      </div>
                      <I.ArrowRight size={12} className="text-[#64748b]"/>
                    </button>
                    <button className="w-full flex items-center gap-3 p-3 bg-[#1a1a2e] hover:bg-[#212140] text-left transition">
                      <I.Receipt size={14} className="text-[#94a3b8]"/>
                      <div className="flex-1">
                        <div className="f-display text-sm font-bold">Nueva factura</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase">Emitir + PDF</div>
                      </div>
                      <I.ArrowRight size={12} className="text-[#64748b]"/>
                    </button>
                    <button className="w-full flex items-center gap-3 p-3 bg-[#1a1a2e] hover:bg-[#212140] text-left transition">
                      <I.Globe size={14} className="text-[#94a3b8]"/>
                      <div className="flex-1">
                        <div className="f-display text-sm font-bold">Generar landing</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase">IA · 1 min</div>
                      </div>
                      <I.ArrowRight size={12} className="text-[#64748b]"/>
                    </button>
                  </div>
                </div>

                <div className="amg-card card-clip p-5">
                  <div className="flex items-center justify-between mb-3">
                    <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Estado del sistema</span>
                    <span className="flex items-center gap-1.5 f-mono text-[10px] text-[#39d353] uppercase">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>OPERATIVO
                    </span>
                  </div>
                  <div className="space-y-1.5 f-mono text-[11px]">
                    <div className="flex justify-between"><span className="text-[#94a3b8]">API</span><span className="text-[#39d353]">99.98%</span></div>
                    <div className="flex justify-between"><span className="text-[#94a3b8]">WA Bots</span><span className="text-[#39d353]">100%</span></div>
                    <div className="flex justify-between"><span className="text-[#94a3b8]">Stripe</span><span className="text-[#39d353]">OK</span></div>
                    <div className="flex justify-between"><span className="text-[#94a3b8]">Landings CDN</span><span className="text-[#f0b429]">DEGRADED</span></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function AMGRevenueChart() {
  const months = ['May','Jun','Jul','Ago','Sep','Oct','Nov','Dic','Ene','Feb','Mar','Abr'];
  const values = [4.2, 4.8, 5.4, 6.1, 6.8, 7.5, 8.1, 8.8, 9.5, 10.6, 11.4, 12.4];
  const max = 14;
  return (
    <div className="mt-4">
      <div className="flex items-baseline gap-3 mb-3">
        <span className="f-display font-black text-3xl text-[#FF9A3C]">€12.4K</span>
        <span className="f-mono text-[11px] text-[#39d353]">+8.2% vs marzo</span>
      </div>
      <div className="h-44 flex items-end gap-2 relative border-b border-[rgba(226,232,240,0.08)] pb-1">
        {/* horizontal grid */}
        <div className="absolute inset-0 flex flex-col justify-between pointer-events-none">
          {[0,1,2,3].map(i => <div key={i} className="border-t border-[rgba(255,107,0,0.04)]"></div>)}
        </div>
        {values.map((v,i) => {
          const h = (v / max) * 100;
          const isLast = i === values.length - 1;
          return (
            <div key={i} className="flex-1 flex flex-col items-center gap-1 relative z-10">
              <div className={`w-full ${isLast ? 'bg-[#FF6B00]' : 'bg-[rgba(255,107,0,0.35)]'} relative`} style={{height: `${h}%`}}>
                {isLast && <div className="absolute -top-1 left-0 right-0 h-[2px] bg-[#FF9A3C]"></div>}
              </div>
            </div>
          );
        })}
      </div>
      <div className="flex gap-2 mt-2">
        {months.map((m,i) => (
          <div key={i} className={`flex-1 f-mono text-[10px] uppercase text-center ${i === months.length-1 ? 'text-[#FF9A3C]' : 'text-[#64748b]'}`}>{m}</div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { AMGSidebar, AMGTopbar, AMGAdminDashboard, AMGRevenueChart });
