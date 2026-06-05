'use client';

import type { FC } from 'react';
import Image from 'next/image';
import DOMPurify from 'dompurify';
import type { Block, PageStyles } from '@/services/factory';

interface Props {
  block: Block;
  styles: PageStyles;
  isSelected?: boolean;
  onSelect?: (id: string) => void;
  onRemove?: (id: string) => void;
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

export const BlockRenderer: FC<Props> = ({
  block, styles, isSelected, onSelect, onRemove, onUpdateProps, onClick, preview,
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

  // Inline editing: only active when block is selected in editor (not preview)
  const editable = !preview && !!isSelected;
  const upd = (key: string) => (v: string) => onUpdateProps?.({ [key]: v });

  const wrapperClass = `relative group ${!preview ? 'border-2 border-transparent hover:border-[#FF6B00]/30 cursor-pointer' : ''} ${isSelected ? '!border-[#FF6B00] ring-1 ring-[#FF6B00]/20' : ''}`;

  const removeBtn = !preview ? (
    <button
      onClick={(e) => { e.stopPropagation(); onRemove?.(block.id); }}
      className="absolute top-2 right-2 z-10 w-6 h-6 bg-red-500 text-white text-xs flex items-center justify-center rounded hover:bg-red-600 opacity-0 group-hover:opacity-100 transition"
    >
      ×
    </button>
  ) : null;

  const renderContent = () => {
    switch (block.type) {
      case 'hero': {
        const hasBg = !!s(p.bgImage);
        return (
          <section
            style={{
              background: hasBg
                ? `linear-gradient(rgba(0,0,0,0.45),rgba(0,0,0,0.45)), url(${s(p.bgImage)}) center/cover`
                : `linear-gradient(135deg, ${accent} 0%, ${primary} 100%)`,
              color: '#fff',
              fontFamily: fontB,
            }}
          >
            {removeBtn}
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '100px 24px', textAlign: 'center' }}>
              <ET
                tag="h1" value={s(p.title, 'Títol principal')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(2rem,5vw,3.5rem)', fontWeight: 800, lineHeight: 1.15, marginBottom: 20, color: '#fff' }}
              />
              <ET
                tag="p" value={s(p.subtitle)} editable={editable} onSave={upd('subtitle')}
                style={{ fontSize: 'clamp(1rem,2.5vw,1.3rem)', opacity: 0.9, marginBottom: 36, maxWidth: 600, margin: '0 auto 36px' }}
              />
              {s(p.ctaText) && (
                <a
                  href={s(p.ctaLink, '#')}
                  style={{ display: 'inline-block', padding: '14px 36px', background: '#fff', color: primary, fontWeight: 700, borderRadius: radius, fontSize: '1rem', textDecoration: 'none' }}
                >
                  {s(p.ctaText)}
                </a>
              )}
            </div>
          </section>
        );
      }

      case 'text':
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 760, margin: '0 auto', padding: '72px 24px' }}>
              <ET
                tag="h2" value={s(p.title)} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, marginBottom: 20 }}
              />
              {editable ? (
                <div
                  contentEditable
                  suppressContentEditableWarning
                  style={{ color: text, opacity: 0.8, lineHeight: 1.8, fontSize: '1.05rem', outline: '2px solid rgba(255,107,0,0.4)', outlineOffset: 2, borderRadius: 2, minHeight: 40 }}
                  onBlur={(e) => onUpdateProps?.({ body: e.currentTarget.innerHTML })}
                  onClick={(e) => e.stopPropagation()}
                  dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(s(p.body)) }}
                />
              ) : (
                <div
                  style={{ color: text, opacity: 0.8, lineHeight: 1.8, fontSize: '1.05rem' }}
                  dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(s(p.body)) }}
                />
              )}
            </div>
          </section>
        );

      case 'services': {
        const items = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'Serveis')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <div key={i} style={{ padding: '28px 24px', borderRadius: radius, boxShadow: '0 2px 12px rgba(0,0,0,0.08)', background: '#fff', borderTop: `4px solid ${primary}` }}>
                    <ET
                      tag="h3" value={s(item.title ?? item.name)} editable={editable}
                      onSave={(v) => { const ni = [...items]; ni[i] = { ...ni[i], title: v, name: v }; onUpdateProps?.({ items: ni }); }}
                      style={{ fontFamily: fontH, fontWeight: 700, fontSize: '1.1rem', color: text, marginBottom: 10 }}
                    />
                    <ET
                      tag="p" value={s(item.description ?? item.desc)} editable={editable}
                      onSave={(v) => { const ni = [...items]; ni[i] = { ...ni[i], description: v, desc: v }; onUpdateProps?.({ items: ni }); }}
                      style={{ color: text, opacity: 0.65, fontSize: '0.95rem', lineHeight: 1.6 }}
                    />
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      case 'gallery': {
        const images = Array.isArray(p.images) ? (p.images as string[]) : [];
        const placeholders = images.length > 0 ? images : Array.from({ length: 6 }, () => '');
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'Galeria')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 40 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(200px,1fr))', gap: 12 }}>
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

      case 'contact-form':
        return (
          <section id="contact" style={{ background: `${accent}10`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 560, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'Contacte')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 36 }}
              />
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {['Nom', 'Email', 'Telèfon'].map((ph) => (
                  <input key={ph} placeholder={ph} readOnly style={{ padding: '12px 16px', border: `1px solid ${accent}30`, borderRadius: radius, fontFamily: fontB, fontSize: '0.95rem', background: '#fff', color: text }} />
                ))}
                <textarea placeholder="Missatge" readOnly rows={4} style={{ padding: '12px 16px', border: `1px solid ${accent}30`, borderRadius: radius, fontFamily: fontB, fontSize: '0.95rem', resize: 'none', background: '#fff', color: text }} />
                <button style={{ padding: '14px', background: primary, color: '#fff', border: 'none', borderRadius: radius, fontFamily: fontH, fontWeight: 700, fontSize: '1rem', cursor: 'pointer' }}>
                  Enviar missatge
                </button>
              </div>
            </div>
          </section>
        );

      case 'faq': {
        const faqs = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 760, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'Preguntes freqüents')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
              />
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {faqs.map((item, i) => (
                  <div key={i} style={{ padding: '20px 24px', borderRadius: radius, background: '#fff', boxShadow: '0 1px 6px rgba(0,0,0,0.06)', borderLeft: `4px solid ${primary}` }}>
                    <ET
                      tag="h3" value={s(item.question ?? item.q)} editable={editable}
                      onSave={(v) => { const ni = [...faqs]; ni[i] = { ...ni[i], question: v, q: v }; onUpdateProps?.({ items: ni }); }}
                      style={{ fontFamily: fontH, fontWeight: 700, color: text, marginBottom: 8, fontSize: '1rem' }}
                    />
                    <ET
                      tag="p" value={s(item.answer ?? item.a)} editable={editable}
                      onSave={(v) => { const ni = [...faqs]; ni[i] = { ...ni[i], answer: v, a: v }; onUpdateProps?.({ items: ni }); }}
                      style={{ color: text, opacity: 0.65, fontSize: '0.95rem', lineHeight: 1.6 }}
                    />
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      case 'testimonials': {
        const testimonials = Array.isArray(p.items) ? (p.items as Array<Record<string, unknown>>) : [];
        return (
          <section style={{ background: `${accent}08`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'Testimonis')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 48 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 24 }}>
                {testimonials.map((item, i) => {
                  const rating = typeof item.rating === 'number' ? item.rating : 5;
                  return (
                    <div key={i} style={{ padding: '28px 24px', borderRadius: radius, background: '#fff', boxShadow: '0 2px 12px rgba(0,0,0,0.07)' }}>
                      <div style={{ color: '#f59e0b', fontSize: '1.1rem', marginBottom: 12 }}>{'★'.repeat(rating)}</div>
                      <ET
                        tag="p" value={s(item.text)} editable={editable}
                        onSave={(v) => { const ni = [...testimonials]; ni[i] = { ...ni[i], text: v }; onUpdateProps?.({ items: ni }); }}
                        style={{ color: text, opacity: 0.75, fontSize: '0.95rem', lineHeight: 1.7, marginBottom: 16, fontStyle: 'italic' }}
                      />
                      <ET
                        tag="span" value={s(item.name)} editable={editable}
                        onSave={(v) => { const ni = [...testimonials]; ni[i] = { ...ni[i], name: v }; onUpdateProps?.({ items: ni }); }}
                        style={{ fontFamily: fontH, fontWeight: 700, fontSize: '0.9rem', color: primary, display: 'block' }}
                      />
                    </div>
                  );
                })}
              </div>
            </div>
          </section>
        );
      }

      case 'cta':
        return (
          <section style={{ background: `linear-gradient(135deg, ${primary} 0%, ${accent} 100%)`, textAlign: 'center', fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 700, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, "Crida a l'acció")} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.5rem,4vw,2.5rem)', fontWeight: 800, color: '#fff', marginBottom: 28, lineHeight: 1.2 }}
              />
              <a
                href="#contact"
                style={{ display: 'inline-block', padding: '16px 40px', background: '#fff', color: primary, fontWeight: 700, borderRadius: radius, fontSize: '1.05rem', textDecoration: 'none' }}
              >
                {s(p.ctaText, 'Contacta')}
              </a>
            </div>
          </section>
        );

      case 'footer':
        return (
          <footer style={{ background: accent, color: '#fff', fontFamily: fontB, padding: '40px 24px', textAlign: 'center' }} onClick={handleClick}>
            {removeBtn}
            <ET
              tag="p" value={s(p.copyright, '© 2026')} editable={editable} onSave={upd('copyright')}
              style={{ opacity: 0.7, fontSize: '0.9rem' }}
            />
          </footer>
        );

      case 'opening-hours': {
        const todayIdx = new Date().getDay(); // 0=Sunday
        const dayOrder = [1,2,3,4,5,6,0]; // Dilluns…Diumenge
        const hours = Array.isArray(p.hours)
          ? (p.hours as Array<{ day: string; open: string; close: string; closed: boolean }>)
          : [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 560, margin: '0 auto', padding: '72px 24px' }}>
              <ET
                tag="h2" value={s(p.title, "Horaris d'atenció")} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 32 }}
              />
              <div style={{ borderRadius: radius, overflow: 'hidden', border: `1px solid ${accent}20` }}>
                {hours.map((row, i) => {
                  const isToday = dayOrder[i % 7] === todayIdx;
                  return (
                    <div key={i} style={{
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      padding: '12px 20px',
                      background: isToday ? `${primary}12` : i % 2 === 0 ? '#fff' : `${accent}05`,
                      borderLeft: isToday ? `4px solid ${primary}` : '4px solid transparent',
                    }}>
                      <span style={{ fontWeight: isToday ? 700 : 400, color: isToday ? primary : text, fontSize: '0.95rem' }}>
                        {row.day}
                      </span>
                      <span style={{ color: row.closed ? '#94a3b8' : text, fontSize: '0.9rem', fontWeight: isToday ? 600 : 400 }}>
                        {row.closed ? 'Tancat' : `${row.open} – ${row.close}`}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>
        );
      }

      case 'map':
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET
                tag="h2" value={s(p.title, 'On som')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, color: text, textAlign: 'center', marginBottom: 24 }}
              />
              <div style={{ borderRadius: radius, overflow: 'hidden', height: 280, background: '#e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748b', flexDirection: 'column', gap: 8 }}>
                <span style={{ fontSize: '2rem' }}>📍</span>
                <span style={{ fontFamily: fontB }}>{s(p.address, 'Adreça')}</span>
              </div>
            </div>
          </section>
        );

      case 'pricing': {
        const items = (p.items as Array<{ name: string; description: string; price: string; period: string; features: string[]; highlighted: boolean }>) ?? [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET tag="h2" value={s(p.title, 'Tarifes')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.5rem,3vw,2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <div key={i} style={{
                    background: item.highlighted ? primary : '#fff',
                    color: item.highlighted ? '#fff' : text,
                    borderRadius: radius, padding: '32px 28px',
                    boxShadow: item.highlighted ? `0 8px 32px ${primary}55` : '0 2px 12px rgba(0,0,0,.08)',
                    border: item.highlighted ? `2px solid ${primary}` : '2px solid transparent',
                    position: 'relative',
                  }}>
                    {item.highlighted && (
                      <div style={{ position: 'absolute', top: -12, left: '50%', transform: 'translateX(-50%)', background: accent, color: '#fff', fontSize: 11, fontWeight: 700, padding: '4px 14px', borderRadius: 99, whiteSpace: 'nowrap' }}>Recomanat</div>
                    )}
                    <h3 style={{ fontFamily: fontH, fontSize: '1.25rem', fontWeight: 700, marginBottom: 8 }}>{item.name}</h3>
                    <p style={{ opacity: .7, fontSize: '.9rem', marginBottom: 20 }}>{item.description}</p>
                    <div style={{ fontSize: '2.5rem', fontWeight: 800, marginBottom: 4 }}>
                      {item.price}<span style={{ fontSize: '1rem', fontWeight: 400, opacity: .7 }}>€/{item.period}</span>
                    </div>
                    <ul style={{ listStyle: 'none', padding: 0, marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
                      {(item.features ?? []).map((f, j) => (
                        <li key={j} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '.9rem' }}>
                          <span style={{ color: item.highlighted ? '#fff' : primary }}>✓</span> {f}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      case 'team': {
        const items = (p.items as Array<{ name: string; role: string; bio: string; photo: string }>) ?? [];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET tag="h2" value={s(p.title, 'El nostre equip')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.5rem,3vw,2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))', gap: 24 }}>
                {items.map((item, i) => (
                  <div key={i} style={{ textAlign: 'center', background: '#fff', borderRadius: radius, padding: '28px 20px', boxShadow: '0 2px 12px rgba(0,0,0,.08)' }}>
                    {item.photo
                      ? <div style={{ position: 'relative', width: 80, height: 80, margin: '0 auto 16px' }}><Image src={item.photo} alt={item.name} fill sizes="80px" style={{ borderRadius: '50%', objectFit: 'cover' }} /></div>
                      : <div style={{ width: 80, height: 80, borderRadius: '50%', background: `${primary}22`, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px', fontSize: 28, color: primary }}>👤</div>
                    }
                    <h3 style={{ fontFamily: fontH, fontWeight: 700, marginBottom: 4, color: text }}>{item.name}</h3>
                    <p style={{ color: primary, fontSize: '.85rem', fontWeight: 600, marginBottom: 10 }}>{item.role}</p>
                    <p style={{ opacity: .6, fontSize: '.85rem', lineHeight: 1.6 }}>{item.bio}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        );
      }

      case 'video': {
        const videoUrl = s(p.videoUrl, '');
        const embedUrl = videoUrl.replace('watch?v=', 'embed/').replace('youtu.be/', 'www.youtube.com/embed/').split('&')[0];
        return (
          <section style={{ background: bg, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 900, margin: '0 auto', padding: '80px 24px' }}>
              {s(p.title, '') && (
                <ET tag="h2" value={s(p.title, '')} editable={editable} onSave={upd('title')}
                  style={{ fontFamily: fontH, fontSize: '2rem', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 32 }}
                />
              )}
              {videoUrl ? (
                <div style={{ borderRadius: radius, overflow: 'hidden', aspectRatio: '16/9' }}>
                  <iframe src={embedUrl} style={{ width: '100%', height: '100%', border: 0 }} allowFullScreen title="video" />
                </div>
              ) : (
                <div style={{ borderRadius: radius, background: '#1a1a2e', aspectRatio: '16/9', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 12, color: '#94a3b8' }}>
                  <span style={{ fontSize: 48, opacity: .4 }}>▶</span>
                  <span style={{ fontSize: '.85rem' }}>Enganxa la URL del vídeo a les propietats</span>
                </div>
              )}
              {s(p.caption, '') && <p style={{ textAlign: 'center', opacity: .6, marginTop: 16, fontSize: '.9rem' }}>{s(p.caption, '')}</p>}
            </div>
          </section>
        );
      }

      case 'reviews': {
        const items = (p.items as Array<{ name: string; rating: number; text: string; date: string }>) ?? [];
        const gmUrl = s(p.googleMapsUrl, '');
        return (
          <section style={{ background: `${accent}10`, fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 1100, margin: '0 auto', padding: '80px 24px' }}>
              <ET tag="h2" value={s(p.title, 'Ressenyes')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.5rem,3vw,2rem)', fontWeight: 700, textAlign: 'center', color: text, marginBottom: 48 }}
              />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(260px,1fr))', gap: 20, marginBottom: gmUrl ? 32 : 0 }}>
                {items.map((item, i) => (
                  <div key={i} style={{ background: '#fff', borderRadius: radius, padding: '24px 20px', boxShadow: '0 2px 12px rgba(0,0,0,.06)' }}>
                    <div style={{ color: '#f59e0b', marginBottom: 10, fontSize: '1.1rem' }}>{'★'.repeat(item.rating)}</div>
                    <p style={{ opacity: .75, fontSize: '.9rem', lineHeight: 1.7, fontStyle: 'italic', marginBottom: 14 }}>&ldquo;{item.text}&rdquo;</p>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 700, fontSize: '.85rem', color: primary }}>{item.name}</span>
                      <span style={{ opacity: .4, fontSize: '.75rem' }}>{item.date}</span>
                    </div>
                  </div>
                ))}
              </div>
              {gmUrl && (
                <div style={{ textAlign: 'center', marginTop: 16 }}>
                  <a href={gmUrl} target="_blank" rel="noopener noreferrer"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: '#fff', border: `1px solid ${primary}`, color: primary, borderRadius: 99, padding: '10px 24px', fontWeight: 600, fontSize: '.9rem', textDecoration: 'none' }}>
                    <span>G</span> Veure totes les ressenyes a Google
                  </a>
                </div>
              )}
            </div>
          </section>
        );
      }

      case 'chat-cta':
        return (
          <section style={{ background: bg, textAlign: 'center', padding: '80px 24px', fontFamily: fontB }} onClick={handleClick}>
            {removeBtn}
            <div style={{ maxWidth: 700, margin: '0 auto' }}>
              <ET
                tag="h2" value={s(p.title, 'Reserva la teva cita')} editable={editable} onSave={upd('title')}
                style={{ fontFamily: fontH, fontSize: 'clamp(1.5rem,3vw,2rem)', fontWeight: 700, marginBottom: 16 }}
              />
              <ET
                tag="p" value={s(p.subtitle, 'Respon en menys d\'1 minut')} editable={editable} onSave={upd('subtitle')}
                style={{ opacity: 0.7, marginBottom: 32 }}
              />
              <button style={{ background: (p.accentColor as string) || primary, color: '#fff', border: 'none', cursor: 'pointer', padding: '16px 40px', borderRadius: radius, fontSize: '1.1rem', fontWeight: 700 }}>
                {s(p.buttonText, 'Xateja amb nosaltres')}
              </button>
            </div>
          </section>
        );

      default:
        return <div style={{ padding: 16, color: '#94a3b8' }}>Bloc desconegut: {block.type}</div>;
    }
  };

  return <div className={wrapperClass} onClick={handleClick}>{renderContent()}</div>;
};
