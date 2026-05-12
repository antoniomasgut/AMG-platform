function AMGPublicLanding() {
  return (
    <div className="amg w-full h-full bg-[#0d0d1a] overflow-auto">
      {/* Nav */}
      <header className="sticky top-0 z-30 backdrop-blur bg-[#0d0d1a]/80 border-b border-[rgba(255,107,0,0.12)]">
        <div className="max-w-[1200px] mx-auto px-10 h-16 flex items-center gap-8">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-[#FF6B00] btn-clip flex items-center justify-center">
              <span className="f-display font-black text-black text-xs">A</span>
            </div>
            <div className="f-display font-black text-sm tracking-[0.15em]">AMG</div>
          </div>
          <nav className="flex gap-6 f-mono text-[11px] uppercase tracking-wider">
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Sistema</a>
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Resultats</a>
            <a className="text-[#e2e8f0] hover:text-[#FF9A3C]">Formación</a>
          </nav>
          <div className="flex-1"></div>
          <AMGButton size="sm" icon={I.ArrowRight}>SOLICITAR DEMO</AMGButton>
        </div>
      </header>

      {/* Hero */}
      <section className="relative amg-grid py-24">
        <div className="relative max-w-[800px] mx-auto px-10 text-center">
          <h1 className="f-display font-black text-[64px] leading-[0.98] tracking-tight">
            DEJA DE PERDER CLIENTES<br/>
            <span className="text-[#FF9A3C]">POR NO RESPONDER</span><br/>
            AL WHATSAPP.
          </h1>
          <p className="text-xl text-[#94a3b8] mt-6">
            Convierte tu WhatsApp en una máquina de generar trabajos organizados. 
            Sin tecnicismos, sin complicaciones.
          </p>
          <div className="flex items-center justify-center gap-3 mt-10">
            <AMGButton size="lg" icon={I.ArrowRight}>AGENDAR DEMO DE FUNCIONAMIENTO</AMGButton>
          </div>
        </div>
      </section>

      {/* Problema */}
      <section className="py-24 bg-[#13132a]">
        <div className="max-w-[800px] mx-auto px-10 text-center">
          <h2 className="f-display font-black text-4xl mb-12">¿TU NEGOCIO TE CONTROLA A TI?</h2>
          <div className="grid gap-6 text-left">
            {['Clientes que te escriben y esperan horas (o días).', 'Consultas que no son urgentes y te interrumpen el trabajo.', 'El caos de gestionar la agenda a base de llamadas.', 'Oportunidades de negocio que se pierden por falta de seguimiento.'].map((item, i) => (
              <div key={i} className="flex items-center gap-4 amg-card p-4">
                <I.AlertCircle className="text-[#FF6B00]" />
                <p className="text-lg">{item}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Solució */}
      <section className="py-24">
        <div className="max-w-[1000px] mx-auto px-10">
          <h2 className="f-display font-black text-4xl mb-12 text-center">TU NUEVO SISTEMA DE GESTIÓN</h2>
          <div className="grid grid-cols-2 gap-8">
            {[
              ['Respuesta inmediata', 'Tus clientes sienten que los atiendes al instante.'],
              ['Clasificación automática', 'Sabemos qué es urgencia, venta o duda.'],
              ['Agenda sin llamadas', 'El cliente elige cuándo, tú solo ejecutas.'],
              ['Seguimiento infalible', 'Nada se pierde, nada se olvida.']
            ].map(([t,d], i) => (
              <div key={i} className="amg-card p-8 border-l-2 border-l-[#FF6B00]">
                <h3 className="f-display font-bold text-xl mb-2">{t}</h3>
                <p className="text-[#94a3b8]">{d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Formación */}
      <section className="py-24 bg-[#1a1a2e]">
        <div className="max-w-[800px] mx-auto px-10 text-center">
          <h2 className="f-display font-black text-4xl mb-6">NO TE DEJAMOS SOLO</h2>
          <p className="text-lg text-[#94a3b8]">No solo instalamos el sistema. Te enseñamos a usarlo. Tú mantienes el control total de tu negocio, nosotros ponemos la infraestructura.</p>
        </div>
      </section>

      {/* CTA Final */}
      <section className="py-24 text-center">
        <AMGButton size="lg" icon={I.ArrowRight}>ACTIVAR MI SISTEMA DE GESTIÓN AUTOMÁTICA</AMGButton>
      </section>

      {/* Footer minimal */}
      <footer className="py-10 text-center text-[#64748b] text-sm">
        AMG Enginyeria Digital · El sistema que organiza tu negocio.
      </footer>
    </div>
  );
}
