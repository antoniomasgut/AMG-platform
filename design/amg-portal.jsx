// amg-portal.jsx — Client Portal dashboard + Login

function AMGPortalDashboard() {
  const services = [
    { name:'WhatsApp Bot AI', icon:'Bot', used:1240, total:2000, unit:'msgs', status:'active' },
    { name:'Landing auto', icon:'Globe', used:1, total:1, unit:'páginas', status:'active' },
    { name:'Workflow engine', icon:'Zap', used:3, total:5, unit:'workflows', status:'active' },
    { name:'Reservas calendario', icon:'Calendar', used:42, total:200, unit:'bookings', status:'active' },
  ];
  return (
    <div className="amg flex w-full h-full bg-[#0d0d1a] overflow-hidden">
      {/* Portal sidebar — simpler than admin */}
      <aside className="w-[240px] shrink-0 bg-[#13132a] border-r border-[rgba(255,107,0,0.12)] flex flex-col">
        <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-5 gap-3">
          <div className="w-9 h-9 bg-[#FF6B00] btn-clip flex items-center justify-center shrink-0">
            <span className="f-display font-black text-black text-sm">N</span>
          </div>
          <div className="flex flex-col leading-tight">
            <span className="f-display font-bold text-sm">Nebula Studio</span>
            <span className="f-mono text-[9px] text-[#FF9A3C] tracking-[0.2em]">PORTAL · GROWTH</span>
          </div>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          <div className="f-mono text-[9px] uppercase tracking-[0.2em] text-[#64748b] px-3 py-2">Mi cuenta</div>
          {[
            ['Dashboard','Dashboard',true],
            ['Servicios','Box'],
            ['Facturas','Receipt'],
            ['Landing pública','Globe'],
            ['Soporte','Bell'],
            ['Configuración','Settings'],
          ].map(([l,ic,a]) => {
            const Ic = I[ic];
            return (
              <a key={l} className={`relative flex items-center gap-3 px-3 h-10 f-mono text-xs uppercase tracking-wider ${a ? 'bg-[rgba(255,107,0,0.10)] text-[#FF9A3C]' : 'text-[#94a3b8] hover:text-[#e2e8f0]'}`}>
                {a && <span className="absolute left-0 top-0 bottom-0 w-[2px] bg-[#FF6B00]"></span>}
                <Ic size={14}/>{l}
              </a>
            );
          })}
        </nav>
        <div className="p-4 border-t border-[rgba(255,107,0,0.12)]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-gradient-to-br from-[#58a6ff] to-[#FF9A3C] btn-clip flex items-center justify-center text-black font-bold text-xs">MR</div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-semibold truncate">Marta Ruiz</div>
              <div className="f-mono text-[10px] text-[#64748b] truncate">marta@nebula.cat</div>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-w-0">
        <div className="h-16 border-b border-[rgba(255,107,0,0.12)] flex items-center px-8 gap-5">
          <div className="flex-1">
            <span className="f-mono text-[10px] uppercase text-[#FF9A3C] tracking-[0.2em]">/ portal /</span>
            <div className="f-display font-bold text-lg leading-tight mt-0.5">Buenas tardes, Marta</div>
          </div>
          <AMGButton variant="outline" size="sm" icon={I.Globe}>VER MI LANDING</AMGButton>
          <AMGButton size="sm" icon={I.Bell}>SOPORTE</AMGButton>
        </div>

        <div className="flex-1 overflow-auto amg-grid p-8 space-y-6">
          {/* Subscription hero */}
          <div className="amg-card card-clip p-6 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-[3px] h-16 bg-[#FF6B00]"></div>
            <div className="grid grid-cols-[1.6fr_1fr_1fr_1fr] gap-6 items-center">
              <div>
                <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Suscripción activa</span>
                <div className="f-display font-black text-3xl mt-1">PLAN GROWTH</div>
                <div className="flex items-center gap-2 mt-2">
                  <AMGBadge tone="success"><span className="w-1 h-1 rounded-full bg-[#39d353]"></span>AL DÍA</AMGBadge>
                  <span className="f-mono text-[11px] text-[#64748b] uppercase">Desde 12 Ene 2026</span>
                </div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Próximo cobro</div>
                <div className="f-display font-bold text-xl mt-1">12 May 2026</div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">en 18 días</div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Importe mensual</div>
                <div className="f-display font-bold text-xl mt-1 text-[#FF9A3C]">€155,00</div>
                <div className="f-mono text-[11px] text-[#94a3b8] mt-0.5">€128,10 + IVA</div>
              </div>
              <div>
                <div className="f-mono text-[10px] uppercase text-[#64748b]">Método de pago</div>
                <div className="flex items-center gap-2 mt-1">
                  <I.CreditCard size={14} stroke="#FF9A3C"/>
                  <span className="f-mono text-sm">•••• 4242</span>
                </div>
                <button className="f-mono text-[11px] uppercase text-[#FF9A3C] mt-1">CAMBIAR</button>
              </div>
            </div>
          </div>

          {/* Services with usage */}
          <div>
            <AMGSectionTitle eyebrow="Uso del mes" title="Servicios activos">
              <span className="f-mono text-[11px] text-[#64748b] uppercase">Ciclo 12 Abr → 12 May</span>
            </AMGSectionTitle>
            <div className="grid grid-cols-2 gap-3">
              {services.map((s,i) => {
                const Ic = I[s.icon];
                const pct = (s.used / s.total) * 100;
                const warn = pct > 80;
                return (
                  <div key={i} className="amg-card card-clip p-5">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-10 h-10 bg-[rgba(255,107,0,0.12)] border border-[rgba(255,107,0,0.35)] flex items-center justify-center">
                        <Ic size={16} stroke="#FF9A3C"/>
                      </div>
                      <div className="flex-1">
                        <div className="f-display font-bold text-sm">{s.name.toUpperCase()}</div>
                        <div className="f-mono text-[10px] text-[#64748b] uppercase flex items-center gap-1.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>OPERATIVO
                        </div>
                      </div>
                      <button className="f-mono text-[11px] uppercase text-[#FF9A3C] flex items-center gap-1">GESTIONAR<I.ArrowRight size={10}/></button>
                    </div>
                    <div className="flex items-baseline justify-between mb-1.5">
                      <span className="f-mono text-[11px] text-[#94a3b8] uppercase">{s.used.toLocaleString()} / {s.total.toLocaleString()} {s.unit}</span>
                      <span className={`f-mono text-[11px] ${warn ? 'text-[#f0b429]' : 'text-[#64748b]'}`}>{Math.round(pct)}%</span>
                    </div>
                    <div className="h-1.5 bg-[#212140] overflow-hidden">
                      <div className={`h-full ${warn ? 'bg-[#f0b429]' : 'bg-[#FF6B00]'}`} style={{width: `${pct}%`}}></div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Recent invoices + CTA */}
          <div className="grid grid-cols-3 gap-4">
            <div className="col-span-2 amg-card card-clip p-5">
              <AMGSectionTitle eyebrow="Historial" title="Últimas facturas">
                <a className="f-mono text-[10px] uppercase text-[#FF9A3C]">VER TODAS →</a>
              </AMGSectionTitle>
              <div className="space-y-0">
                {[
                  ['2026-0114','12 Abr 2026','€155,00','PAID','success'],
                  ['2026-0110','12 Mar 2026','€155,00','PAID','success'],
                  ['2026-0104','12 Feb 2026','€155,00','PAID','success'],
                  ['2026-0101','12 Ene 2026','€329,00','PAID','success',' (incl. setup)'],
                ].map((r,i) => (
                  <div key={i} className="grid grid-cols-[100px_1fr_100px_100px_30px] gap-3 px-2 h-11 items-center border-b border-[rgba(226,232,240,0.04)] text-sm last:border-b-0">
                    <span className="f-mono text-[#FF9A3C] text-xs">#{r[0]}</span>
                    <span className="f-mono text-[12px] text-[#94a3b8]">{r[1]}{r[5] || ''}</span>
                    <span className="f-mono text-[#e2e8f0]">{r[2]}</span>
                    <span><AMGBadge tone={r[4]}>{r[3]}</AMGBadge></span>
                    <button className="text-[#94a3b8] hover:text-[#FF9A3C]"><I.Download size={12}/></button>
                  </div>
                ))}
              </div>
            </div>

            <div className="amg-card card-clip p-5 flex flex-col">
              <I.Sparkles size={20} stroke="#FF9A3C"/>
              <div className="f-display font-bold text-base mt-3">¿NECESITAS AYUDA?</div>
              <p className="text-[13px] text-[#94a3b8] mt-1 flex-1">Responde tu asesor técnico asignado — Oriol M. · generalmente en menos de 2 horas.</p>
              <div className="space-y-2 mt-4">
                <AMGButton size="sm" icon={I.Mail} className="w-full justify-center">ESCRIBIR AL EQUIPO</AMGButton>
                <AMGButton variant="outline" size="sm" icon={I.Play} className="w-full justify-center">VER TUTORIALES</AMGButton>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function AMGLogin({ state = 'form' }) {
  return (
    <div className="amg relative w-full h-full bg-[#0d0d1a] overflow-hidden flex">
      {/* Animated grid bg */}
      <div className="absolute inset-0 amg-grid-sm"></div>
      <div className="absolute inset-0 pointer-events-none" style={{
        background:'radial-gradient(ellipse at 30% 50%, rgba(255,107,0,0.18), transparent 45%), radial-gradient(ellipse at 80% 80%, rgba(255,154,60,0.10), transparent 40%)'
      }}></div>

      {/* Left: brand panel */}
      <div className="relative hidden lg:flex w-[48%] p-16 flex-col justify-between z-10 border-r border-[rgba(255,107,0,0.15)]">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 bg-[#FF6B00] btn-clip flex items-center justify-center">
            <span className="f-display font-black text-black text-xl">A</span>
          </div>
          <div>
            <div className="f-display font-black text-lg tracking-[0.12em]">AMG</div>
            <div className="f-mono text-[10px] text-[#FF9A3C] tracking-[0.25em]">ENGINYERIA DIGITAL</div>
          </div>
        </div>

        <div>
          <div className="f-mono text-[11px] uppercase tracking-[0.2em] text-[#FF9A3C] mb-4">PORTAL v2.14.0</div>
          <h1 className="f-display font-black text-5xl leading-[1.05]">
            PYMES <span className="text-[#FF9A3C]">AUTOMATIZADAS</span><br/>
            EN MENOS DE 48 HORAS.
          </h1>
          <p className="text-lg text-[#94a3b8] mt-4 max-w-md">
            Bots, landings, workflows y facturación — un solo portal administrado.
          </p>
        </div>

        <div className="flex items-center gap-4 f-mono text-[10px] uppercase text-[#64748b]">
          <span className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-[#39d353] amg-blink"></span>SISTEMA OPERATIVO</span>
          <span>· 99.98% uptime</span>
          <span>· 48 pymes activas</span>
        </div>
      </div>

      {/* Right: login card */}
      <div className="relative flex-1 flex items-center justify-center p-10 z-10">
        <div className="w-full max-w-[420px]">
          <div className="amg-card card-clip p-8">
            {state === 'form' ? (
              <>
                <div className="flex items-center gap-2 mb-1">
                  <div className="w-1.5 h-1.5 bg-[#FF6B00]"></div>
                  <span className="f-mono text-[10px] uppercase tracking-[0.2em] text-[#FF9A3C]">Iniciar sesión</span>
                </div>
                <h2 className="f-display font-black text-2xl mb-2">ACCESO AL PORTAL</h2>
                <p className="text-[13px] text-[#94a3b8] mb-6">Te enviamos un enlace mágico por email. Sin contraseñas.</p>

                <div className="space-y-3">
                  <AMGInput label="Email corporativo" placeholder="tu@empresa.com" icon={I.Mail}/>
                  <AMGButton className="w-full justify-center" icon={I.ArrowRight}>ENVIAR MAGIC LINK</AMGButton>
                </div>

                <div className="flex items-center gap-3 my-5">
                  <div className="flex-1 h-[1px] bg-[rgba(226,232,240,0.08)]"></div>
                  <span className="f-mono text-[10px] uppercase text-[#64748b] tracking-wider">o con contraseña</span>
                  <div className="flex-1 h-[1px] bg-[rgba(226,232,240,0.08)]"></div>
                </div>

                <button className="w-full h-10 bg-[#1a1a2e] border border-[rgba(255,107,0,0.14)] flex items-center justify-between px-3 text-sm hover:border-[#FF6B00] transition">
                  <span className="flex items-center gap-2"><I.Lock size={14} stroke="#94a3b8"/>Usar contraseña</span>
                  <I.ChevDown size={12} className="text-[#64748b]"/>
                </button>
              </>
            ) : (
              <>
                <div className="w-16 h-16 bg-[rgba(255,107,0,0.12)] border border-[#FF6B00] flex items-center justify-center mb-5 mx-auto">
                  <I.Mail size={24} stroke="#FF9A3C"/>
                </div>
                <h2 className="f-display font-black text-2xl text-center">COMPRUEBA TU EMAIL</h2>
                <p className="text-[13px] text-[#94a3b8] mt-2 text-center">Enviamos un enlace de acceso a</p>
                <div className="f-mono text-sm text-[#FF9A3C] text-center mt-1">marta@nebula.cat</div>

                <div className="mt-6 p-3 border-l-2 border-l-[#58a6ff] bg-[rgba(88,166,255,0.05)] flex gap-3">
                  <I.Clock size={14} stroke="#58a6ff" className="shrink-0 mt-0.5"/>
                  <div className="text-[12px] text-[#94a3b8]">El enlace caduca en <span className="text-[#58a6ff] f-mono">15 min</span>. Revisa spam si no lo ves.</div>
                </div>

                <AMGButton variant="secondary" className="w-full justify-center mt-5">REENVIAR EN 0:42</AMGButton>
                <button className="w-full mt-3 f-mono text-[11px] uppercase text-[#64748b] hover:text-[#FF9A3C]">← USAR OTRO EMAIL</button>
              </>
            )}
          </div>

          <div className="mt-6 text-center f-mono text-[10px] uppercase text-[#64748b] tracking-wider">
            <a className="hover:text-[#FF9A3C]">TÉRMINOS</a> · <a className="hover:text-[#FF9A3C]">PRIVACIDAD</a> · <a className="hover:text-[#FF9A3C]">SOPORTE</a>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { AMGPortalDashboard, AMGLogin });
