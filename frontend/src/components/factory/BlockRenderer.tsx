'use client';

import type { FC } from 'react';
import type { Block, PageStyles } from '@/services/factory';

interface Props {
  block: Block;
  styles: PageStyles;
  isSelected?: boolean;
  onSelect?: (id: string) => void;
  onRemove?: (id: string) => void;
  onClick?: () => void;
  preview?: boolean;
}

export const BlockRenderer: FC<Props> = ({ block, styles: _, isSelected, onSelect, onRemove, onClick, preview }) => {
  const handleClick = () => {
    if (preview) return;
    if (onClick) onClick();
    if (onSelect) onSelect(block.id);
  };

  const p = block.props;
  const s = (v: unknown, def = '') => String(v ?? def);
  const wrapperClass = `relative group ${!preview ? 'border-2 border-transparent hover:border-[#FF6B00]/30 cursor-pointer' : ''} ${isSelected ? '!border-[#FF6B00] ring-1 ring-[#FF6B00]/20' : ''}`;

  const removeBtn = !preview ? (
    <button
      onClick={(e) => { e.stopPropagation(); onRemove?.(block.id); }}
      className="absolute top-1 right-1 z-10 w-6 h-6 bg-red-500 text-white text-xs flex items-center justify-center rounded hover:bg-red-600 opacity-0 group-hover:opacity-100 transition"
    >
      ×
    </button>
  ) : null;

  const renderContent = () => {
    switch (block.type) {
      case 'hero':
        return (
          <section className="py-20 px-4 text-center" style={{ background: s(p.bgImage) ? `url(${s(p.bgImage)}) center/cover` : '#f8fafc' }}>
            {removeBtn}
            <h1 className="text-4xl font-bold mb-4" style={{ color: '#1e293b' }}>{s(p.title, 'Hero Title')}</h1>
            <p className="text-lg mb-6" style={{ color: '#64748b' }}>{s(p.subtitle)}</p>
            {s(p.ctaText) ? (
              <span className="inline-block px-6 py-3 text-white font-semibold rounded" style={{ background: '#FF6B00' }}>
                {s(p.ctaText)}
              </span>
            ) : null}
          </section>
        );
      case 'text':
        return (
          <section className="py-12 px-4 max-w-3xl mx-auto" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold mb-4">{s(p.title)}</h2>
            <div className="prose" dangerouslySetInnerHTML={{ __html: s(p.body) }} />
          </section>
        );
      case 'services':
        return (
          <section className="py-12 px-4" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-8">{s(p.title, 'Serveis')}</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl mx-auto">
              {Array.isArray(p.items) ? (p.items as Array<{ name: string; desc: string }>).map((item, i) => (
                <div key={i} className="p-4 border rounded-lg text-center">
                  <h3 className="font-semibold">{item.name}</h3>
                  <p className="text-sm text-gray-500">{item.desc}</p>
                </div>
              )) : null}
            </div>
          </section>
        );
      case 'gallery':
        return (
          <section className="py-12 px-4" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-8">{s(p.title, 'Galeria')}</h2>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-2 max-w-4xl mx-auto">
              {Array.isArray(p.images) && (p.images as string[]).length > 0
                ? (p.images as string[]).map((img, i) => (
                    <div key={i} className="aspect-square bg-gray-200 rounded flex items-center justify-center text-gray-400 text-sm">
                      {img ? <img src={img} alt="" className="w-full h-full object-cover rounded" /> : 'Img'}
                    </div>
                  ))
                : Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} className="aspect-square bg-gray-200 rounded flex items-center justify-center text-gray-400 text-sm">Foto</div>
                  ))}
            </div>
          </section>
        );
      case 'contact-form':
        return (
          <section className="py-12 px-4 bg-gray-50" id="contact" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-8">{s(p.title, 'Contacte')}</h2>
            <div className="max-w-md mx-auto space-y-3">
              <input placeholder="Nom" className="w-full p-2 border rounded" readOnly />
              <input placeholder="Email" className="w-full p-2 border rounded" readOnly />
              <textarea placeholder="Missatge" className="w-full p-2 border rounded h-24" readOnly />
              <button className="px-6 py-2 text-white font-semibold rounded" style={{ background: '#FF6B00' }}>Enviar</button>
            </div>
          </section>
        );
      case 'faq':
        return (
          <section className="py-12 px-4 max-w-3xl mx-auto" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-8">{s(p.title, 'FAQ')}</h2>
            {Array.isArray(p.items) ? (p.items as Array<{ q: string; a: string }>).map((item, i) => (
              <div key={i} className="border-b py-4">
                <h3 className="font-semibold mb-1">{item.q}</h3>
                <p className="text-sm text-gray-500">{item.a}</p>
              </div>
            )) : null}
          </section>
        );
      case 'testimonials':
        return (
          <section className="py-12 px-4" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-8">{s(p.title, 'Testimonis')}</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl mx-auto">
              {Array.isArray(p.items) ? (p.items as Array<{ name: string; text: string; rating: number }>).map((item, i) => (
                <div key={i} className="p-4 border rounded-lg">
                  <div className="text-yellow-400 mb-2">{'★'.repeat(typeof item.rating === 'number' ? item.rating : 5)}</div>
                  <p className="text-sm text-gray-500 mb-2">{item.text}</p>
                  <p className="font-semibold text-sm">{item.name}</p>
                </div>
              )) : null}
            </div>
          </section>
        );
      case 'cta':
        return (
          <section className="py-16 px-4 text-center" style={{ background: '#FF6B00' }} onClick={handleClick}>
            {removeBtn}
            <h2 className="text-3xl font-bold text-white mb-4">{s(p.title, "Crida a l'acció")}</h2>
            <span className="inline-block px-6 py-3 bg-white font-semibold rounded">{s(p.ctaText, 'Contacta')}</span>
          </section>
        );
      case 'footer':
        return (
          <footer className="py-8 px-4 text-center text-sm text-gray-500 bg-gray-900 text-gray-400" onClick={handleClick}>
            {removeBtn}
            <p>{s(p.copyright, '© 2026')}</p>
          </footer>
        );
      case 'map':
        return (
          <section className="py-12 px-4" onClick={handleClick}>
            {removeBtn}
            <h2 className="text-2xl font-bold text-center mb-4">{s(p.title, 'On som')}</h2>
            <div className="max-w-4xl mx-auto h-64 bg-gray-200 rounded flex items-center justify-center text-gray-400">
              🗺️ {s(p.address, 'Mapa')}
            </div>
          </section>
        );
      default:
        return <div className="p-4 text-gray-400">Bloc desconegut: {block.type}</div>;
    }
  };

  return <div className={wrapperClass} onClick={handleClick}>{renderContent()}</div>;
};
