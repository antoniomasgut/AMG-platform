import { getTranslations } from 'next-intl/server';

export async function ServicesSection() {
  const t = await getTranslations('services');
  const profiles = t.raw('profiles') as {
    name: string;
    tagline: string;
    price: string;
    features: string[];
  }[];

  return (
    <section id="serveis" className="relative py-24">
      <div className="amg-grid-sm" />

      <div className="max-w-6xl mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="f-display text-3xl sm:text-4xl font-black text-white mb-4">
            {t('title')}
          </h2>
          <p className="text-ink-2 text-lg max-w-2xl mx-auto">
            {t('subtitle')}
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {profiles.map((profile, i) => (
            <div
              key={i}
              className={`amg-card p-6 border transition-all duration-300 group ${
                i === profiles.length - 1
                  ? 'border-accent/30 bg-accent/5 md:col-span-2 lg:col-span-1'
                  : 'border-white/5 hover:border-accent/20'
              }`}
            >
              <div className="flex items-center justify-between mb-4">
                <h3 className="f-display text-lg font-bold text-white">{profile.name}</h3>
                <span className="text-accent font-bold text-sm">{profile.price}</span>
              </div>
              <p className="text-ink-2 text-sm mb-4">{profile.tagline}</p>
              <ul className="space-y-2 mb-6">
                {profile.features.map((feat, j) => (
                  <li key={j} className="flex items-start gap-2 text-sm text-ink-2">
                    <svg className="w-4 h-4 mt-0.5 text-accent shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    {feat}
                  </li>
                ))}
              </ul>
              <a
                href="#cta"
                className="block w-full text-center py-2.5 text-xs font-bold tracking-wider border border-accent/30 text-accent btn-clip hover:bg-accent hover:text-white transition-all"
              >
                SOL·LICITAR
              </a>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
