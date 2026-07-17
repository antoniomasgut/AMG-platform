'use client';

import { useState, useEffect, useRef, type FC } from 'react';
import Image from 'next/image';
import DOMPurify from 'dompurify';
import type { Block, PageStyles } from '@/services/factory';

interface Props {
  block: Block;
  styles: PageStyles;
  isSelected?: boolean;
  onSelect?: (id: string) => void;
  onRemove?: (id: string) => void;
  onDuplicate?: (id: string) => void;
  onUpdateProps?: (props: Partial<Record<string, unknown>>) => void;
  onClick?: () => void;
  preview?: boolean;
}

/** Editable text element — only when block is selected in editor */
const ET: FC<{
  tag: 'h1' | 'h2' | 'h3' | 'p' | 'span';
  value: string;
  editable: boolean;
  onSave: (v: string) => void;
  style?: React.CSSProperties;
  className?: string;
}> = ({ tag: Tag, value, editable, onSave, style, className }) => {
  if (!editable) {
    return <Tag style={style} className={className}>{value}</Tag>;
  }
  return (
    <Tag
      contentEditable
      suppressContentEditableWarning
      style={{ ...style, outline: '2px solid rgba(255,107,0,0.4)', outlineOffset: 2, borderRadius: 2 }}
      className={className}
      onBlur={(e) => onSave(e.currentTarget.textContent ?? '')}
      onClick={(e) => e.stopPropagation()}
    >
      {value}
    </Tag>
  );
};

/** Hover-animated card wrapper */
const HoverCard: FC<{ children: React.ReactNode; style?: React.CSSProperties }> = ({ children, style }) => {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        ...style,
        transform: hovered ? 'translateY(-4px)' : 'translateY(0)',
        boxShadow: hovered ? '0 12px 32px rgba(0,0,0,0.15)' : (style?.boxShadow ?? '0 2px 12px rgba(0,0,0,0.08)'),
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
      }}
    >
      {children}
    </div>
  );
};

/** Scroll-fade-in wrapper — applies opacity+translateY animation when entering viewport */
const FadeIn: FC<{ children: React.ReactNode; delay?: number }> = ({ children, delay = 0 }) => {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) { setVisible(true); obs.disconnect(); } },
      { threshold: 0.1 }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);
  return (
    <div
      ref={ref}
      style={{
        opacity: visible ? 1 : 0,
        transform: visible ? 'translateY(0)' : 'translateY(24px)',
        transition: `opacity 0.5s ease ${delay}ms, transform 0.5s ease ${delay}ms`,
      }}
    >
      {children}
    </div>
  );
};

/** Initials avatar */
const Avatar: FC<{ name: string; size?: number; color: string }> = ({ name, size = 48, color }) => {
  const initials = name.split(' ').slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('');
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: `${color}22`, border: `2px solid ${color}44`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontWeight: 700, fontSize: size * 0.35, color, flexShrink: 0,
    }}>
      {initials || '?'}
    </div>
  );
};

const SECTION_ID: Partial<Record<string, string>> = {
  hero: 'hero', services: 'services', 'contact-form': 'contact',
  'opening-hours': 'hours', testimonials: 'testimonials', faq: 'faq',
  gallery: 'gallery', pricing: 'pricing', team: 'team', map: 'map',
  stats: 'stats', steps: 'steps', reviews: 'reviews', 'trust-bar': 'trust',
  'before-after': 'before-after',
};

export const BlockRenderer: FC<Props> = ({
  block, styles, isSelected, onSelect, onRemove, onDuplicate, onUpdateProps, onClick, preview,
}) => {
  const handleClick = () => {
    if (preview) return;
    if (onClick) onClick();
    if (onSelect) onSelect(block.id);
  };

  const p = block.props;
  const s = (v: unknown, def = '') => String(v ?? def);
  const primary = styles.primaryColor || '#FF6B00';
  const accent = styles.accentColor || '#1e293b';
  const bg = styles.bgColor || '#ffffff';
  const text = styles.textColor || '#1e293b';
  const fontH = styles.fontHeading || 'Montserrat, sans-serif';
  const fontB = styles.fontBody || 'Open Sans, sans-serif';
  const radius = styles.borderRadius || '8px';

  const editable = !preview && !!isSelected;
  const upd = (key: string) => (v: string) => onUpdateProps?.({ [key]: v });

  const wrapperClass = `relative group ${!preview ? 'border-2 border-transparent hover:border-[#FF6B00]/30 cursor-pointer' : ''} ${isSelected ? '!border-[#FF6B00] ring-1 ring-[#FF6B00]/20' : ''}`;

  const removeBtn = !preview ? (
    <div className="absolute top-2 right-2 z-10 flex gap-1 opacity-0 group-hover:opacity-100 transition">
      <button
        onClick={(e) => { e.stopPropagation(); onDuplicate?.(block.id); }}
        title="Duplicar bloc"
        className="w-6 h-6 bg-[#FF6B00] text-white text-xs flex items-center justify-center rounded hover:bg-[#FF9A3C]"
      >⧉</button>
      <button
        onClick={(e) => { e.stopPropagation(); onRemove?.(block.id); }}
        title="Eliminar bloc"
        className="w-6 h-6 bg-red-500 text-white text-xs flex items-center justify-center rounded hover:bg-red-600"
      >×</button>
    </div>
  ) : null;

  const renderContent = () => {
    switch (block.type) {

      // ─── HEADER / NAVBAR ──────────────────────────────────────────────────
      case 'header': {
        return <HeaderBlock p={p} s={s} styles={styles} bg={bg} accent={accent} primary={primary} text={text} fontH={fontH} fontB={fontB} radius={radius} preview={!!preview} removeBtn={removeBtn} handleClick={handleClick} />;
      }

      // ─── HERO ─────────────────────────────────────────────────────────────
      case 'hero': {
        const hasBg = !!s(p.bgImage);
        const badge = s(p.badgeText);
        return (
          <section
            style={{
              background: hasBg
                ? `linear-gradient(rgba(0,0,0,0.5),rgba(0,0,0,0.5)), url(${s(p.bgImage)}) center/cover`
                : `linear-gradient(135deg, ${accent} 0%, ${primary} 100%)`,
              color: '#fff',
              fontFamily: fontB,
              position: 'relative',
            }}
            onClick={handleClick}
          >
            {removeBtn}
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '100px 24px 120px', textAlign: 'center' }}>
              {badge && (
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.35)', borderRadius: 99, padding: '6px 16px', marginBottom: 24, fontSize: '0.82rem', fontWeight: 600, backdropFilter: 'blur(4px)' }}>
                  <span>✓</span> {badge}
                </div>
              )}
              <ET
                tag="h1" value={s(p.title, 'Títol principal')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(2.2rem,5.5vw,3.8rem)', fontWeight: 800, lineHeight: 1.12, marginBottom: 20, color: '#fff', letterSpacing: '-0.02em' }}
              />
              <ET
                tag="p" value={s(p.subtitle)} editable={editable} onSave={upd('subtitle')}
                style={{ fontSize: 'clamp(1rem,2.5vw,1.25rem)', opacity: 0.88, maxWidth: 620, margin: '0 auto 40px', lineHeight: 1.6 }}
              />
              <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
                {s(p.ctaText) && (
                  <a
                    href={s(p.ctaLink, '#contact')}
                    style={{ display: 'inline-block', padding: '15px 36px', background: '#fff', color: primary, fontWeight: 700, borderRadius: radius, fontSize: '1rem', textDecoration: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}
                  >
                    {s(p.ctaText)}
                  </a>
                )}
                {s(p.secondaryCtaText) && (
                  <a
                    href={s(p.secondaryCtaLink, '#services')}
                    style={{ display: 'inline-block', padding: '15px 36px', background: 'transparent', color: '#fff', fontWeight: 600, borderRadius: radius, fontSize: '1rem', textDecoration: 'none', border: '2px solid rgba(255,255,255,0.5)' }}
                  >
                    {s(p.secondaryCtaText)}
                  </a>
                )}
              </div>
            </div>
            {/* Scroll indicator */}
            <style>{`@keyframes bounce-dot{0%,100%{transform:translateY(0)}50%{transform:translateY(10px)}}`}</style>
            <div style={{ position: 'absolute', bottom: 24, left: '50%', transform: 'translateX(-50%)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, opacity: 0.55 }}>
              <div style={{ width: 24, height: 40, border: '2px solid rgba(255,255,255,0.6)', borderRadius: 12, display: 'flex', justifyContent: 'center', paddingTop: 6 }}>
                <div style={{ width: 4, height: 8, background: 'rgba(255,255,255,0.8)', borderRadius: 2, animation: 'bounce-dot 1.5s ease-in-out infinite' }} />
              </div>
            </div>
          </section>
        );
      }

      // ─── STATS ────────────────────────────────────────────────────────────
      case 'stats': {
        const items = Array.isArray(p.items) ? (p.items as Array<{ number: string; label: string; icon?: string }>) : [
          { number: '500+', label: 'Clients satisfets', icon: '👥' },
          { number: '15', label: 'Anys d\'experiència', icon: '🏆' },
          { number: '4.9★', label: 'Valoració Google', icon: '⭐' },
        ];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '72px 24px' }}>
              {s(p.title) && (
                <ET tag="h2" value={s(p.title)} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
                />
              )}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(130px,1fr))', gap: 32 }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 100}>
                    <div style={{ textAlign: 'center', padding: '24px 16px' }}>
                      {item.icon && <div style={{ fontSize: '2rem', marginBottom: 8 }}>{item.icon}</div>}
                      <div style={{ fontFamily: fontH, fontSize: 'clamp(2rem,4vw,3rem)', fontWeight: 800, color: primary, lineHeight: 1.1, marginBottom: 8 }}>
                        {item.number}
                      </div>
                      <div style={{ fontSize: '0.95rem', color: text, opacity: 0.65, fontWeight: 500 }}>
                        {item.label}
                      </div>
                    </div>
                  </FadeIn>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── TEXT ─────────────────────────────────────────────────────────────
      case 'text':
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 760, margin: '0 auto', padding: '80px 24px' }}>
              <FadeIn>
                <ET
                  tag="h2" value={s(p.title)} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, marginBottom: 24, lineHeight: 1.25 }}
                />
                {editable ? (
                  <div
                    contentEditable
                    suppressContentEditableWarning
                    style={{ color: text, opacity: 0.8, lineHeight: 1.85, fontSize: '1.05rem', outline: '2px solid rgba(255,107,0,0.4)', outlineOffset: 2, borderRadius: 2, minHeight: 40 }}
                    onBlur={(e) => onUpdateProps?.({ body: DOMPurify.sanitize(e.currentTarget.innerHTML) })}
                    onClick={(e) => e.stopPropagation()}
                    dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(s(p.body)) }}
                  />
                ) : (
                  <div
                    style={{ color: text, opacity: 0.8, lineHeight: 1.85, fontSize: '1.05rem' }}
                    dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(s(p.body)) }}
                  />
                )}
              </FadeIn>
            </div>
          </section>
        );

      // ─── SERVICES ─────────────────────────────────────────────────────────
      case 'services': {
        const items = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET
                  tag="h2" value={s(p.title, 'Serveis')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 56 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 80}>
                    <HoverCard style={{ padding: '32px 28px', borderRadius: radius, boxShadow: '0 2px 12px rgba(0,0,0,0.08)', background: '#fff', borderTop: `4px solid ${primary}` }}>
                      {s(item.icon) && (
                        <div style={{ fontSize: '2rem', marginBottom: 16 }}>{s(item.icon)}</div>
                      )}
                      <ET
                        tag="h3" value={s(item.title ?? item.name)} editable={editable}
                        onSave={(v) => { const ni = [...items]; ni[i] = { ...ni[i], title: v, name: v }; onUpdateProps?.({ items: ni }); }}
                        style={{ fontFamily: fontH, fontWeight: 700, fontSize: '1.15rem', color: text, marginBottom: 12 }}
                      />
                      <ET
                        tag="p" value={s(item.description ?? item.desc)} editable={editable}
                        onSave={(v) => { const ni = [...items]; ni[i] = { ...ni[i], description: v, desc: v }; onUpdateProps?.({ items: ni }); }}
                        style={{ color: text, opacity: 0.62, fontSize: '0.95rem', lineHeight: 1.65 }}
                      />
                    </HoverCard>
                  </FadeIn>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── STEPS ────────────────────────────────────────────────────────────
      case 'steps': {
        const items = Array.isArray(p.items) ? (p.items as Array<{ number?: string; title: string; description: string; icon?: string }>) : [];
        return (
          <section style={{ background: `${accent}06`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Com treballem')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 56 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(220px,1fr))', gap: 24, position: 'relative' }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 100}>
                    <div style={{ textAlign: 'center', padding: '28px 20px' }}>
                      <div style={{ width: 56, height: 56, borderRadius: '50%', background: primary, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px', fontFamily: fontH, fontWeight: 800, fontSize: '1.3rem' }}>
                        {item.icon ?? item.number ?? (i + 1)}
                      </div>
                      <h3 style={{ fontFamily: fontH, fontWeight: 700, fontSize: '1.1rem', color: text, marginBottom: 10 }}>{item.title}</h3>
                      <p style={{ fontSize: '0.9rem', color: text, opacity: 0.62, lineHeight: 1.65 }}>{item.description}</p>
                    </div>
                  </FadeIn>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── GALLERY ──────────────────────────────────────────────────────────
      case 'gallery': {
        const images = Array.isArray(p.images) ? (p.images as string[]) : [];
        const placeholders = images.length > 0 ? images : Array.from({ length: 6 }, () => '');
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET tag="h2" value={s(p.title, 'Galeria')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 40 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(220px,1fr))', gap: 12 }}>
                {placeholders.map((img, i) => (
                  <div key={i} style={{ position: 'relative', aspectRatio: '1', borderRadius: radius, overflow: 'hidden', background: '#f1f5f9' }}>
                    {img
                      ? <Image src={img} alt="" fill sizes="(max-width: 768px) 50vw, 33vw" style={{ objectFit: 'cover' }} />
                      : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94a3b8', fontSize: '0.8rem' }}>Foto</div>
                    }
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── CONTACT FORM ─────────────────────────────────────────────────────
      case 'contact-form': {
        const wa = s(p.whatsappNumber, styles.whatsappNumber ?? '');
        const ph = s(p.phone, styles.phone ?? '');
        return (
          <section style={{ background: `${accent}08`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 640, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Contacte')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 12 }}
                />
                {s(p.subtitle) && (
                  <ET tag="p" value={s(p.subtitle)} editable={editable} onSave={upd('subtitle')}
                    style={{ textAlign: 'center', color: text, opacity: 0.6, marginBottom: 40, fontSize: '1rem' }}
                  />
                )}
                {/* WhatsApp / Phone quick actions */}
                {(wa || ph) && (
                  <div style={{ display: 'flex', gap: 12, marginBottom: 28, flexWrap: 'wrap' }}>
                    {wa && (
                      <a href={`https://wa.me/${wa.replace(/\D/g, '')}`} target="_blank" rel="noopener noreferrer"
                        style={{ flex: 1, minWidth: 160, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '13px 20px', background: '#25D366', color: '#fff', borderRadius: radius, fontWeight: 700, fontSize: '0.95rem', textDecoration: 'none' }}>
                        <span>💬</span> WhatsApp
                      </a>
                    )}
                    {ph && (
                      <a href={`tel:${ph}`}
                        style={{ flex: 1, minWidth: 160, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '13px 20px', border: `2px solid ${primary}`, color: primary, borderRadius: radius, fontWeight: 700, fontSize: '0.95rem', textDecoration: 'none', background: 'transparent' }}>
                        <span>📞</span> {ph}
                      </a>
                    )}
                  </div>
                )}
                {(wa || ph) && (
                  <div style={{ textAlign: 'center', marginBottom: 28, color: text, opacity: 0.4, fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ flex: 1, height: 1, background: `${text}22` }} />
                    <span>o envia'ns un missatge</span>
                    <div style={{ flex: 1, height: 1, background: `${text}22` }} />
                  </div>
                )}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {['Nom complet', 'Correu electrònic', 'Telèfon (opcional)'].map((ph2) => (
                    <input key={ph2} placeholder={ph2} readOnly style={{ padding: '13px 16px', border: `1px solid ${accent}30`, borderRadius: radius, fontFamily: fontB, fontSize: '0.95rem', background: '#fff', color: text }} />
                  ))}
                  <textarea placeholder="Com et podem ajudar?" readOnly rows={4} style={{ padding: '13px 16px', border: `1px solid ${accent}30`, borderRadius: radius, fontFamily: fontB, fontSize: '0.95rem', resize: 'none', background: '#fff', color: text }} />
                  <button style={{ padding: '15px', background: primary, color: '#fff', border: 'none', borderRadius: radius, fontFamily: fontH, fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                    Enviar missatge →
                  </button>
                  <p style={{ textAlign: 'center', fontSize: '0.78rem', opacity: 0.45, margin: 0 }}>Respondrem en menys de 24h · Sense compromís</p>
                </div>
              </FadeIn>
            </div>
          </section>
        );
      }

      // ─── FAQ (accordion) ──────────────────────────────────────────────────
      case 'faq': {
        const faqs = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 760, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Preguntes freqüents')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
                />
              </FadeIn>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {faqs.map((item, i) => (
                  <FAQItem key={i} item={item} i={i} items={faqs} text={text} primary={primary} accent={accent} radius={radius} fontH={fontH} editable={editable} onUpdateProps={onUpdateProps} />
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── TESTIMONIALS ─────────────────────────────────────────────────────
      case 'testimonials': {
        const testimonials = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: `${accent}06`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Testimonis')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(280px,1fr))', gap: 24 }}>
                {testimonials.map((item, i) => {
                  const rating = typeof item.rating === 'number' ? item.rating : 5;
                  const name = s(item.name);
                  return (
                    <FadeIn key={i} delay={i * 80}>
                      <HoverCard style={{ padding: '28px 26px', borderRadius: radius, background: '#fff', boxShadow: '0 2px 12px rgba(0,0,0,0.07)' }}>
                        <div style={{ color: '#f59e0b', fontSize: '1rem', marginBottom: 14, letterSpacing: 2 }}>{'★'.repeat(rating)}</div>
                        <ET tag="p" value={s(item.text)} editable={editable}
                          onSave={(v) => { const ni = [...testimonials]; ni[i] = { ...ni[i], text: v }; onUpdateProps?.({ items: ni }); }}
                          style={{ color: text, opacity: 0.72, fontSize: '0.95rem', lineHeight: 1.75, marginBottom: 20, fontStyle: 'italic' }}
                        />
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                          <Avatar name={name} color={primary} size={42} />
                          <div>
                            <ET tag="span" value={name} editable={editable}
                              onSave={(v) => { const ni = [...testimonials]; ni[i] = { ...ni[i], name: v }; onUpdateProps?.({ items: ni }); }}
                              style={{ fontFamily: fontH, fontWeight: 700, fontSize: '0.92rem', color: text, display: 'block' }}
                            />
                            {s(item.role) && <span style={{ fontSize: '0.78rem', color: primary, fontWeight: 500 }}>{s(item.role)}</span>}
                          </div>
                        </div>
                      </HoverCard>
                    </FadeIn>
                  );
                })}
              </div>
            </div>
          </section>
        );
      }

      // ─── TRUST BAR ────────────────────────────────────────────────────────
      case 'trust-bar': {
        const items = Array.isArray(p.items) ? (p.items as Array<{ name: string; logo?: string; icon?: string }>) : [];
        return (
          <section style={{ background: `${accent}05`, borderTop: `1px solid ${accent}12`, borderBottom: `1px solid ${accent}12`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '32px 24px' }}>
              {s(p.title) && (
                <p style={{ textAlign: 'center', fontSize: '0.8rem', fontWeight: 600, color: text, opacity: 0.45, textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: 24 }}>
                  {s(p.title)}
                </p>
              )}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 40, flexWrap: 'wrap' }}>
                {items.map((item, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, opacity: 0.55 }}>
                    {item.logo
                      ? <img src={item.logo} alt={item.name} style={{ height: 28, objectFit: 'contain', filter: 'grayscale(100%)' }} />
                      : item.icon
                        ? <span style={{ fontSize: '1.4rem' }}>{item.icon}</span>
                        : null
                    }
                    <span style={{ fontSize: '0.9rem', fontWeight: 600, color: text }}>{item.name}</span>
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── CTA ──────────────────────────────────────────────────────────────
      case 'cta':
        return (
          <section style={{ background: `linear-gradient(135deg, ${primary} 0%, ${accent} 100%)`, textAlign: 'center', fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 700, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, "Crida a l'acció")} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.7rem,4vw,2.6rem)', fontWeight: 800, color: '#fff', marginBottom: 16, lineHeight: 1.2 }}
                />
                {s(p.subtitle) && (
                  <ET tag="p" value={s(p.subtitle)} editable={editable} onSave={upd('subtitle')}
                    style={{ color: 'rgba(255,255,255,0.8)', marginBottom: 36, fontSize: '1.05rem', lineHeight: 1.6 }}
                  />
                )}
                <a href={s(p.ctaLink, '#contact')}
                  style={{ display: 'inline-block', padding: '16px 44px', background: '#fff', color: primary, fontWeight: 700, borderRadius: radius, fontSize: '1.05rem', textDecoration: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
                  {s(p.ctaText, 'Contacta')}
                </a>
              </FadeIn>
            </div>
          </section>
        );

      // ─── FOOTER ───────────────────────────────────────────────────────────
      case 'footer': {
        const footerLinks = Array.isArray(p.links) ? (p.links as Array<{ label: string; href: string }>) : [];
        const socialLinks = Array.isArray(p.socialLinks) ? (p.socialLinks as Array<{ name: string; url: string; icon?: string }>) : [];
        const businessName = s(p.businessName, styles.chatBusinessName ?? 'El Negoci');
        const tagline = s(p.tagline);
        const addr = s(p.address, styles.address ?? '');
        const email = s(p.email);
        const phone = s(p.phone, styles.phone ?? '');
        const year = new Date().getFullYear();
        const socialIcons: Record<string, string> = { instagram: '📸', facebook: '👥', twitter: '🐦', linkedin: '💼', youtube: '▶', tiktok: '🎵' };
        return (
          <footer style={{ background: accent, color: '#fff', fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1200, margin: '0 auto', padding: '64px 24px 40px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 48, marginBottom: 48 }}>
                {/* Col 1: Brand */}
                <div>
                  <div style={{ fontFamily: fontH, fontWeight: 800, fontSize: '1.3rem', color: '#fff', marginBottom: 12, letterSpacing: '-0.02em' }}>
                    {businessName}
                  </div>
                  {tagline && <p style={{ opacity: 0.55, fontSize: '0.9rem', lineHeight: 1.6, marginBottom: 20 }}>{tagline}</p>}
                  {socialLinks.length > 0 && (
                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                      {socialLinks.map((sl, i) => (
                        <a key={i} href={sl.url} target="_blank" rel="noopener noreferrer"
                          style={{ width: 36, height: 36, borderRadius: '50%', background: 'rgba(255,255,255,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1rem', textDecoration: 'none' }}
                          title={sl.name}
                        >
                          {sl.icon ?? socialIcons[sl.name.toLowerCase()] ?? '🔗'}
                        </a>
                      ))}
                    </div>
                  )}
                </div>
                {/* Col 2: Quick links */}
                {footerLinks.length > 0 && (
                  <div>
                    <h4 style={{ fontFamily: fontH, fontWeight: 700, fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.1em', opacity: 0.55, marginBottom: 16 }}>Navegació</h4>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                      {footerLinks.map((l, i) => (
                        <a key={i} href={l.href} style={{ color: 'rgba(255,255,255,0.7)', textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500 }}
                          onMouseEnter={(e) => { (e.target as HTMLElement).style.color = '#fff'; }}
                          onMouseLeave={(e) => { (e.target as HTMLElement).style.color = 'rgba(255,255,255,0.7)'; }}
                        >{l.label}</a>
                      ))}
                    </div>
                  </div>
                )}
                {/* Col 3: Contact */}
                {(addr || email || phone) && (
                  <div>
                    <h4 style={{ fontFamily: fontH, fontWeight: 700, fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.1em', opacity: 0.55, marginBottom: 16 }}>Contacte</h4>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                      {addr && <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}><span style={{ opacity: 0.55 }}>📍</span><span style={{ opacity: 0.75, fontSize: '0.88rem', lineHeight: 1.5 }}>{addr}</span></div>}
                      {phone && <a href={`tel:${phone}`} style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'rgba(255,255,255,0.75)', textDecoration: 'none', fontSize: '0.88rem' }}><span style={{ opacity: 0.55 }}>📞</span>{phone}</a>}
                      {email && <a href={`mailto:${email}`} style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'rgba(255,255,255,0.75)', textDecoration: 'none', fontSize: '0.88rem' }}><span style={{ opacity: 0.55 }}>✉️</span>{email}</a>}
                    </div>
                  </div>
                )}
              </div>
              {/* Bottom bar */}
              <div style={{ borderTop: 'rgba(255,255,255,0.15) 1px solid', paddingTop: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                <ET tag="p" value={s(p.copyright, `© ${year} ${businessName} · Tots els drets reservats`)} editable={editable} onSave={upd('copyright')}
                  style={{ opacity: 0.4, fontSize: '0.8rem' }}
                />
                <div style={{ display: 'flex', gap: 16 }}>
                  {['Avís legal', 'Privacitat', 'Cookies'].map((l) => (
                    <a key={l} href="#" style={{ opacity: 0.35, fontSize: '0.75rem', color: '#fff', textDecoration: 'none' }}>{l}</a>
                  ))}
                </div>
              </div>
            </div>
          </footer>
        );
      }

      // ─── OPENING HOURS ────────────────────────────────────────────────────
      case 'opening-hours': {
        const todayIdx = new Date().getDay();
        const dayOrder = [1,2,3,4,5,6,0];
        const hours = Array.isArray(p.hours)
          ? (p.hours as Array<{ day: string; open: string; close: string; closed: boolean }>)
          : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 560, margin: '0 auto', padding: '72px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, "Horaris d'atenció")} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 32 }}
                />
                <div style={{ borderRadius: radius, overflow: 'hidden', border: `1px solid ${accent}20`, boxShadow: '0 2px 12px rgba(0,0,0,0.06)' }}>
                  {hours.map((row, i) => {
                    const isToday = dayOrder[i % 7] === todayIdx;
                    return (
                      <div key={i} style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        padding: '13px 22px',
                        background: isToday ? `${primary}12` : i % 2 === 0 ? '#fff' : `${accent}04`,
                        borderLeft: isToday ? `4px solid ${primary}` : '4px solid transparent',
                      }}>
                        <span style={{ fontWeight: isToday ? 700 : 400, color: isToday ? primary : text, fontSize: '0.92rem' }}>{row.day}</span>
                        <span style={{ color: row.closed ? '#94a3b8' : text, fontSize: '0.88rem', fontWeight: isToday ? 600 : 400 }}>
                          {row.closed ? 'Tancat' : `${row.open} – ${row.close}`}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </FadeIn>
            </div>
          </section>
        );
      }

      // ─── MAP ──────────────────────────────────────────────────────────────
      case 'map': {
        const lat = s(p.lat, '39.5696');
        const lng = s(p.lng, '2.6502');
        const addr2 = s(p.address, styles.address ?? '');
        const mapSrc = s(p.mapUrl) || `https://maps.google.com/maps?q=${lat},${lng}&z=15&output=embed`;
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'On som')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 24 }}
                />
                {addr2 && (
                  <p style={{ textAlign: 'center', opacity: 0.6, marginBottom: 24, fontSize: '0.95rem' }}>📍 {addr2}</p>
                )}
                <div style={{ borderRadius: radius, overflow: 'hidden', height: 360, boxShadow: '0 4px 24px rgba(0,0,0,0.1)' }}>
                  <iframe
                    src={mapSrc}
                    width="100%" height="100%"
                    style={{ border: 0 }}
                    allowFullScreen
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                    title="Ubicació"
                  />
                </div>
              </FadeIn>
            </div>
          </section>
        );
      }

      // ─── PRICING ──────────────────────────────────────────────────────────
      case 'pricing': {
        const items = (p.items as Array<{ name: string; description: string; price: string; period: string; features: string[]; highlighted: boolean }>) ?? [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Tarifes')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 100}>
                    <HoverCard style={{
                      background: item.highlighted ? primary : '#fff',
                      color: item.highlighted ? '#fff' : text,
                      borderRadius: radius, padding: '36px 28px',
                      boxShadow: item.highlighted ? `0 12px 40px ${primary}44` : '0 2px 12px rgba(0,0,0,.08)',
                      border: item.highlighted ? `2px solid ${primary}` : '2px solid transparent',
                      position: 'relative',
                    }}>
                      {item.highlighted && (
                        <div style={{ position: 'absolute', top: -13, left: '50%', transform: 'translateX(-50%)', background: accent, color: '#fff', fontSize: 11, fontWeight: 700, padding: '4px 16px', borderRadius: 99, whiteSpace: 'nowrap' }}>Més popular</div>
                      )}
                      <h3 style={{ fontFamily: fontH, fontSize: '1.3rem', fontWeight: 700, marginBottom: 8 }}>{item.name}</h3>
                      <p style={{ opacity: .65, fontSize: '.88rem', marginBottom: 24 }}>{item.description}</p>
                      <div style={{ fontSize: '2.8rem', fontWeight: 800, marginBottom: 4, lineHeight: 1 }}>
                        {item.price}<span style={{ fontSize: '1rem', fontWeight: 400, opacity: .65 }}>€/{item.period}</span>
                      </div>
                      <div style={{ height: 1, background: item.highlighted ? 'rgba(255,255,255,0.2)' : `${accent}15`, margin: '24px 0' }} />
                      <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {(item.features ?? []).map((f, j) => (
                          <li key={j} style={{ display: 'flex', alignItems: 'flex-start', gap: 10, fontSize: '.9rem', lineHeight: 1.4 }}>
                            <span style={{ color: item.highlighted ? '#fff' : primary, fontWeight: 700, flexShrink: 0 }}>✓</span> {f}
                          </li>
                        ))}
                      </ul>
                    </HoverCard>
                  </FadeIn>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── TEAM ─────────────────────────────────────────────────────────────
      case 'team': {
        const items = (p.items as Array<{ name: string; role: string; bio: string; photo: string }>) ?? [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'El nostre equip')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 80}>
                    <HoverCard style={{ textAlign: 'center', background: '#fff', borderRadius: radius, padding: '32px 20px', boxShadow: '0 2px 12px rgba(0,0,0,.08)' }}>
                      {item.photo
                        ? <div style={{ position: 'relative', width: 88, height: 88, margin: '0 auto 16px' }}><Image src={item.photo} alt={item.name} fill sizes="88px" style={{ borderRadius: '50%', objectFit: 'cover' }} /></div>
                        : <div style={{ margin: '0 auto 16px' }}><Avatar name={item.name} color={primary} size={88} /></div>
                      }
                      <h3 style={{ fontFamily: fontH, fontWeight: 700, marginBottom: 4, color: text, fontSize: '1.05rem' }}>{item.name}</h3>
                      <p style={{ color: primary, fontSize: '.85rem', fontWeight: 600, marginBottom: 12 }}>{item.role}</p>
                      <p style={{ opacity: .58, fontSize: '.85rem', lineHeight: 1.65 }}>{item.bio}</p>
                    </HoverCard>
                  </FadeIn>
                ))}
              </div>
            </div>
          </section>
        );
      }

      // ─── VIDEO ────────────────────────────────────────────────────────────
      case 'video': {
        const videoUrl = s(p.videoUrl, '');
        const embedUrl = videoUrl.replace('watch?v=', 'embed/').replace('youtu.be/', 'www.youtube.com/embed/').split('&')[0];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '80px 24px' }}>
              {s(p.title) && (
                <ET tag="h2" value={s(p.title)} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 32 }}
                />
              )}
              {videoUrl ? (
                <div style={{ borderRadius: radius, overflow: 'hidden', aspectRatio: '16/9', boxShadow: '0 8px 32px rgba(0,0,0,0.15)' }}>
                  <iframe src={embedUrl} style={{ width: '100%', height: '100%', border: 0 }} allowFullScreen title="video" />
                </div>
              ) : (
                <div style={{ borderRadius: radius, background: '#1a1a2e', aspectRatio: '16/9', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12, color: '#94a3b8' }}>
                  <span style={{ fontSize: 48, opacity: .3 }}>▶</span>
                  <span style={{ fontSize: '.85rem' }}>Enganxa la URL del vídeo a les propietats</span>
                </div>
              )}
              {s(p.caption) && <p style={{ textAlign: 'center', opacity: .55, marginTop: 16, fontSize: '.9rem' }}>{s(p.caption)}</p>}
            </div>
          </section>
        );
      }

      // ─── REVIEWS ──────────────────────────────────────────────────────────
      case 'reviews': {
        const items = (p.items as Array<{ name: string; rating: number; text: string; date: string }>) ?? [];
        const gmUrl = s(p.googleMapsUrl);
        return (
          <section style={{ background: `${accent}06`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '88px 24px' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Ressenyes')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
                />
              </FadeIn>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 20, marginBottom: gmUrl ? 32 : 0 }}>
                {items.map((item, i) => (
                  <FadeIn key={i} delay={i * 80}>
                    <HoverCard style={{ background: '#fff', borderRadius: radius, padding: '24px 22px', boxShadow: '0 2px 12px rgba(0,0,0,.06)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                        <div style={{ color: '#f59e0b', fontSize: '1rem', letterSpacing: 2 }}>{'★'.repeat(item.rating)}</div>
                        <span style={{ fontSize: '1rem', opacity: .5 }}>G</span>
                      </div>
                      <p style={{ opacity: .72, fontSize: '.9rem', lineHeight: 1.7, fontStyle: 'italic', marginBottom: 16 }}>&ldquo;{item.text}&rdquo;</p>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 700, fontSize: '.85rem', color: primary }}>{item.name}</span>
                        <span style={{ opacity: .35, fontSize: '.75rem' }}>{item.date}</span>
                      </div>
                    </HoverCard>
                  </FadeIn>
                ))}
              </div>
              {gmUrl && (
                <div style={{ textAlign: 'center' }}>
                  <a href={gmUrl} target="_blank" rel="noopener noreferrer"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: '#fff', border: `1px solid ${primary}`, color: primary, borderRadius: 99, padding: '11px 28px', fontWeight: 600, fontSize: '.9rem', textDecoration: 'none' }}>
                    <span>⭐</span> Veure totes les ressenyes a Google
                  </a>
                </div>
              )}
            </div>
          </section>
        );
      }

      // ─── CHAT CTA ─────────────────────────────────────────────────────────
      case 'chat-cta':
        return (
          <section style={{ background: bg, textAlign: 'center', padding: '88px 24px', fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 700, margin: '0 auto' }}>
              <FadeIn>
                <ET tag="h2" value={s(p.title, 'Reserva la teva cita')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: 'clamp(1.6rem,3vw,2.2rem)', fontWeight: 700, color: text, marginBottom: 12 }}
                />
                <ET tag="p" value={s(p.subtitle, 'Respon en menys d\'1 minut')} editable={editable} onSave={upd('subtitle')}
                  style={{ opacity: 0.65, marginBottom: 36, fontSize: '1.05rem', color: text }}
                />
                <button style={{ background: (p.accentColor as string) || primary, color: '#fff', border: 'none', cursor: 'pointer', padding: '16px 44px', borderRadius: radius, fontSize: '1.1rem', fontWeight: 700, fontFamily: fontH }}>
                  {s(p.buttonText, 'Xateja amb nosaltres')}
                </button>
              </FadeIn>
            </div>
          </section>
        );

      // ─── BEFORE / AFTER ───────────────────────────────────────────────────
      case 'before-after': {
        return <BeforeAfterBlock p={p} primary={primary} radius={radius} text={text} fontH={fontH} removeBtn={removeBtn} editable={editable} upd={upd} handleClick={handleClick} />;
      }

      default:
        return <div style={{ padding: 16, color: '#94a3b8' }}>Bloc desconegut: {block.type}</div>;
    }
  };

  const sectionId = SECTION_ID[block.type];
  return <div id={sectionId} className={wrapperClass} onClick={handleClick}>{renderContent()}</div>;
};

// ─── HEADER BLOCK (amb hamburger mòbil) ──────────────────────────────────────
const HeaderBlock: FC<{
  p: Record<string, unknown>;
  s: (v: unknown, def?: string) => string;
  styles: PageStyles;
  bg: string; accent: string; primary: string; text: string;
  fontH: string; fontB: string; radius: string;
  preview: boolean;
  removeBtn: React.ReactNode;
  handleClick: () => void;
}> = ({ p, s, bg, accent, primary, text, fontH, fontB, radius, preview, removeBtn, handleClick }) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const links = Array.isArray(p.links) ? (p.links as Array<{ label: string; href: string }>) : [];
  const logoText = s(p.logoText, 'El Negoci');
  const phone = s(p.phone, '');
  const ctaText = s(p.ctaText, 'Contacta');

  return (
    <header style={{ position: preview ? 'relative' : 'sticky', top: 0, zIndex: 100, background: bg, backdropFilter: 'blur(8px)', borderBottom: `1px solid ${accent}18`, fontFamily: fontB }} onClick={handleClick}>
      {removeBtn}
      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '0 24px', height: 68, display: 'flex', alignItems: 'center', gap: 32 }}>
        {/* Logo */}
        {s(p.logo) ? (
          <img src={s(p.logo)} alt={logoText} style={{ height: 40, objectFit: 'contain' }} />
        ) : (
          <span style={{ fontFamily: fontH, fontWeight: 800, fontSize: '1.2rem', color: primary, whiteSpace: 'nowrap', letterSpacing: '-0.02em' }}>{logoText}</span>
        )}
        {/* Desktop nav */}
        <nav style={{ display: 'flex', gap: 28, flex: 1 }} className="hidden-mobile">
          {links.map((l, i) => (
            <a key={i} href={l.href} style={{ fontFamily: fontB, fontSize: '0.9rem', color: text, opacity: 0.75, textDecoration: 'none', fontWeight: 500 }}
              onMouseEnter={(e) => { (e.target as HTMLElement).style.opacity = '1'; (e.target as HTMLElement).style.color = primary; }}
              onMouseLeave={(e) => { (e.target as HTMLElement).style.opacity = '0.75'; (e.target as HTMLElement).style.color = text; }}
            >{l.label}</a>
          ))}
        </nav>
        {/* Right side desktop */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexShrink: 0 }} className="hidden-mobile">
          {phone && (
            <a href={`tel:${phone}`} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.85rem', color: text, opacity: 0.7, textDecoration: 'none', fontWeight: 500 }}>
              <span>📞</span>{phone}
            </a>
          )}
          <a href={s(p.ctaLink, '#contact')} style={{ padding: '9px 20px', background: primary, color: '#fff', borderRadius: radius, fontFamily: fontH, fontWeight: 700, fontSize: '0.85rem', textDecoration: 'none', whiteSpace: 'nowrap' }}>
            {ctaText}
          </a>
        </div>
        {/* Hamburger button — mobile only */}
        <button
          onClick={(e) => { e.stopPropagation(); setMenuOpen(!menuOpen); }}
          style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', padding: 8, display: 'none' }}
          className="show-mobile"
          aria-label="Menú"
        >
          <div style={{ width: 22, height: 2, background: text, marginBottom: 5, transition: 'transform 0.2s', transform: menuOpen ? 'rotate(45deg) translate(5px,5px)' : 'none' }} />
          <div style={{ width: 22, height: 2, background: text, marginBottom: 5, opacity: menuOpen ? 0 : 1, transition: 'opacity 0.2s' }} />
          <div style={{ width: 22, height: 2, background: text, transition: 'transform 0.2s', transform: menuOpen ? 'rotate(-45deg) translate(5px,-5px)' : 'none' }} />
        </button>
      </div>
      {/* Mobile menu dropdown */}
      {menuOpen && (
        <div style={{ background: bg, borderTop: `1px solid ${accent}15`, padding: '16px 24px 24px' }} className="mobile-menu">
          {links.map((l, i) => (
            <a key={i} href={l.href} onClick={() => setMenuOpen(false)} style={{ display: 'block', padding: '10px 0', color: text, textDecoration: 'none', fontWeight: 500, borderBottom: `1px solid ${accent}10` }}>
              {l.label}
            </a>
          ))}
          <a href={s(p.ctaLink, '#contact')} onClick={() => setMenuOpen(false)} style={{ display: 'block', marginTop: 16, padding: '12px 20px', background: primary, color: '#fff', borderRadius: radius, fontWeight: 700, textDecoration: 'none', textAlign: 'center' }}>
            {ctaText}
          </a>
        </div>
      )}
      <style>{`
        @media(max-width:768px){.hidden-mobile{display:none!important}.show-mobile{display:flex!important}}
        @media(min-width:769px){.show-mobile{display:none!important}.mobile-menu{display:none!important}}
      `}</style>
    </header>
  );
};

// ─── FAQ ACCORDION ITEM ──────────────────────────────────────────────────────
const FAQItem: FC<{
  item: Record<string, unknown>;
  i: number;
  items: Array<Record<string, unknown>>;
  text: string; primary: string; accent: string; radius: string; fontH: string;
  editable: boolean;
  onUpdateProps?: (props: Partial<Record<string, unknown>>) => void;
}> = ({ item, i, items, text, primary, accent, radius, fontH, editable, onUpdateProps }) => {
  const [open, setOpen] = useState(i === 0);
  const s = (v: unknown, def = '') => String(v ?? def);
  return (
    <FadeIn delay={i * 60}>
      <div style={{ borderRadius: radius, background: '#fff', boxShadow: '0 1px 6px rgba(0,0,0,0.06)', overflow: 'hidden', border: open ? `1px solid ${primary}44` : `1px solid ${accent}15` }}>
        <button
          onClick={() => setOpen(!open)}
          style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '18px 22px', background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left', gap: 16 }}
        >
          <span style={{ fontFamily: fontH, fontWeight: 600, color: text, fontSize: '1rem', lineHeight: 1.4 }}>
            {s(item.question ?? item.q)}
          </span>
          <span style={{ color: primary, fontSize: '1.2rem', fontWeight: 700, flexShrink: 0, transform: open ? 'rotate(45deg)' : 'none', transition: 'transform 0.2s' }}>+</span>
        </button>
        {open && (
          <div style={{ padding: '0 22px 18px', color: text, opacity: 0.65, fontSize: '0.95rem', lineHeight: 1.7 }}>
            {editable ? (
              <div
                contentEditable
                suppressContentEditableWarning
                onBlur={(e) => { const ni = [...items]; ni[i] = { ...ni[i], answer: e.currentTarget.textContent, a: e.currentTarget.textContent }; onUpdateProps?.({ items: ni }); }}
              >
                {s(item.answer ?? item.a)}
              </div>
            ) : (
              s(item.answer ?? item.a)
            )}
          </div>
        )}
      </div>
    </FadeIn>
  );
};

// ─── BEFORE / AFTER INTERACTIVE SLIDER ───────────────────────────────────────
const BeforeAfterBlock: FC<{
  p: Record<string, unknown>;
  primary: string; radius: string; text: string; fontH: string;
  removeBtn: React.ReactNode;
  editable: boolean;
  upd: (key: string) => (v: string) => void;
  handleClick: () => void;
}> = ({ p, primary, radius, text, fontH, removeBtn, editable, upd, handleClick }) => {
  const s = (v: unknown, def = '') => String(v ?? def);
  const [pct, setPct] = useState(50);
  const containerRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);

  const setFromEvent = (clientX: number) => {
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const p2 = Math.max(5, Math.min(95, ((clientX - rect.left) / rect.width) * 100));
    setPct(p2);
  };

  const beforeImg = s(p.beforeImage);
  const afterImg  = s(p.afterImage);
  const beforeLbl = s(p.beforeLabel, 'Abans');
  const afterLbl  = s(p.afterLabel, 'Després');

  return (
    <section style={{ background: '#f8fafc', fontFamily: 'inherit' }} onClick={handleClick}>
      {removeBtn}
      <div style={{ maxWidth: 900, margin: '0 auto', padding: '80px 24px' }}>
        {s(p.title) && (
          <div style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 32 }}>
            {s(p.title)}
          </div>
        )}
        <div
          ref={containerRef}
          style={{ position: 'relative', borderRadius: radius, overflow: 'hidden', aspectRatio: '16/9', background: '#1a1a2e', cursor: 'ew-resize', userSelect: 'none' }}
          onMouseDown={(e) => { dragging.current = true; setFromEvent(e.clientX); }}
          onMouseMove={(e) => { if (dragging.current) setFromEvent(e.clientX); }}
          onMouseUp={() => { dragging.current = false; }}
          onMouseLeave={() => { dragging.current = false; }}
          onTouchStart={(e) => { dragging.current = true; setFromEvent(e.touches[0].clientX); }}
          onTouchMove={(e) => { if (dragging.current) setFromEvent(e.touches[0].clientX); }}
          onTouchEnd={() => { dragging.current = false; }}
        >
          {/* After (full) */}
          <div style={{ position: 'absolute', inset: 0, background: '#4b5563', overflow: 'hidden' }}>
            {afterImg ? <img src={afterImg} alt={afterLbl} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af', fontSize: '0.8rem' }}>Imatge &quot;{afterLbl}&quot;</div>}
          </div>
          {/* Before (clipped) */}
          <div style={{ position: 'absolute', inset: 0, clipPath: `inset(0 ${100 - pct}% 0 0)`, background: '#374151', overflow: 'hidden' }}>
            {beforeImg ? <img src={beforeImg} alt={beforeLbl} style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af', fontSize: '0.8rem' }}>Imatge &quot;{beforeLbl}&quot;</div>}
          </div>
          {/* Divider */}
          <div style={{ position: 'absolute', top: 0, bottom: 0, left: `${pct}%`, width: 3, background: '#fff', transform: 'translateX(-50%)', pointerEvents: 'none' }}>
            <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 40, height: 40, background: '#fff', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 10px rgba(0,0,0,0.3)', fontSize: 14, color: '#374151' }}>⇆</div>
          </div>
          {/* Labels */}
          <div style={{ position: 'absolute', bottom: 12, left: 12 }}>
            <span style={{ background: 'rgba(0,0,0,0.65)', color: '#fff', fontSize: '0.7rem', padding: '3px 10px', borderRadius: 4 }}>{beforeLbl}</span>
          </div>
          <div style={{ position: 'absolute', bottom: 12, right: 12 }}>
            <span style={{ background: primary, color: '#fff', fontSize: '0.7rem', padding: '3px 10px', borderRadius: 4 }}>{afterLbl}</span>
          </div>
        </div>
        <p style={{ textAlign: 'center', marginTop: 12, fontSize: '0.78rem', opacity: 0.45 }}>Arrossega per comparar</p>
      </div>
    </section>
  );
};
