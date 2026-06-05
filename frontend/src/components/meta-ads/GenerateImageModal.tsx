'use client';

import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { AMGButton } from '@/components/ui/button';
import { I } from '@/components/ui/icons';
import { useToast } from '@/lib/toast-context';
import { generateImage, type GenerateImageRequest, type ImageUpload } from '@/services/meta-ads';

const FORMAT_OPTIONS: { value: GenerateImageRequest['format']; label: string; dims: string; icon: string }[] = [
  { value: 'FEED',   label: 'Facebook / Instagram Feed', dims: '1200×628 · Horitzontal',  icon: '🖼' },
  { value: 'SQUARE', label: 'Instagram Feed Quadrat',    dims: '1080×1080 · 1:1',         icon: '⬛' },
  { value: 'STORY',  label: 'Instagram Story / Reels',  dims: '1080×1920 · Vertical',     icon: '📱' },
  { value: 'BANNER', label: 'Facebook Cover / Banner',  dims: '820×312 · Panoràmica',     icon: '📰' },
];

const STYLE_OPTIONS: { value: GenerateImageRequest['style']; label: string; desc: string }[] = [
  { value: 'REALISTIC',   label: 'Fotorealista',  desc: 'Foto professional d\'alta qualitat' },
  { value: 'CINEMATIC',   label: 'Cinematogràfic', desc: 'Il·luminació dramàtica, efecte cinema' },
  { value: 'ILLUSTRATED', label: 'Il·lustrat',     desc: 'Flat design, colors vius, vectorial' },
  { value: 'MINIMAL',     label: 'Minimalista',    desc: 'Net, fons blanc, formes geomètriques' },
];

interface Props {
  tenantId: string;
  initialPrompt?: string;
  onAccept: (result: ImageUpload) => void;
  onClose: () => void;
}

export function GenerateImageModal({ tenantId, initialPrompt = '', onAccept, onClose }: Props) {
  const { toast } = useToast();
  const [prompt, setPrompt] = useState(initialPrompt);
  const [format, setFormat] = useState<GenerateImageRequest['format']>('FEED');
  const [style, setStyle] = useState<GenerateImageRequest['style']>('REALISTIC');
  const [result, setResult] = useState<ImageUpload | null>(null);

  const genMut = useMutation({
    mutationFn: () => generateImage(tenantId, { prompt, format, style }),
    onSuccess: (r) => {
      setResult(r);
      toast('success', 'Imatge generada i pujada a Meta');
    },
    onError: (e: any) => toast('error', e.message ?? 'Error generant la imatge'),
  });

  const aspectClass = {
    FEED:   'aspect-[16/9]',
    SQUARE: 'aspect-square',
    STORY:  'aspect-[9/16]',
    BANNER: 'aspect-[20/6]',
  }[format];

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-surface-raised border border-border-base rounded-xl w-full max-w-lg my-4 flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-5 pt-5 pb-3 border-b border-border-base">
          <div>
            <h2 className="text-base font-bold text-ink-1">Genera imatge amb IA</h2>
            <p className="text-xs text-ink-3 mt-0.5">DALL·E 3 · Puja automàticament a Meta Ads</p>
          </div>
          <button type="button" onClick={onClose} className="text-ink-3 hover:text-ink-1">
            <I.X size={18} />
          </button>
        </div>

        <div className="p-5 space-y-5 overflow-y-auto">
          {/* Format */}
          <div>
            <label className="block text-xs text-ink-2 mb-2">Format de publicació</label>
            <div className="grid grid-cols-2 gap-2">
              {FORMAT_OPTIONS.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => { setFormat(opt.value); setResult(null); }}
                  className={`text-left p-2.5 rounded-lg border transition-all ${
                    format === opt.value
                      ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)]'
                      : 'border-border-base bg-surface-base hover:border-border-raised'
                  }`}
                >
                  <span className="text-base block mb-0.5">{opt.icon}</span>
                  <span className="text-xs font-medium text-ink-1 block leading-tight">{opt.label}</span>
                  <span className="text-[10px] text-ink-3">{opt.dims}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Style */}
          <div>
            <label className="block text-xs text-ink-2 mb-2">Estil visual</label>
            <div className="grid grid-cols-2 gap-2">
              {STYLE_OPTIONS.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => { setStyle(opt.value); setResult(null); }}
                  className={`text-left p-2.5 rounded-lg border transition-all ${
                    style === opt.value
                      ? 'border-[#FF6B00] bg-[rgba(255,107,0,0.08)]'
                      : 'border-border-base bg-surface-base hover:border-border-raised'
                  }`}
                >
                  <span className="text-xs font-medium text-ink-1 block">{opt.label}</span>
                  <span className="text-[10px] text-ink-3">{opt.desc}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Prompt */}
          <div>
            <label className="block text-xs text-ink-2 mb-1">Descripció de la imatge</label>
            <textarea
              className="w-full bg-surface-base border border-border-base rounded px-3 py-2 text-sm text-ink-1 focus:outline-none focus:border-accent-light resize-none"
              rows={3}
              placeholder="Ex: Reforma de bany modern, marbre blanc, ambient net i professional"
              value={prompt}
              onChange={e => { setPrompt(e.target.value); setResult(null); }}
            />
            <p className="text-[10px] text-ink-3 mt-0.5">Descriu el contingut visual, no el text. DALL·E no afegeix text a la imatge.</p>
          </div>

          {/* Preview */}
          {result && (
            <div className="space-y-2">
              <p className="text-xs text-ink-2">Previsualització</p>
              <div className={`${aspectClass} w-full rounded-lg overflow-hidden border border-border-base bg-surface-base`}>
                <img src={result.url} alt="Generated" className="w-full h-full object-cover" />
              </div>
              <div className="flex items-center gap-2">
                <I.Check size={12} className="text-[#39d353]" />
                <span className="text-xs text-[#39d353]">Pujada a Meta · hash: {result.hash.slice(0, 12)}...</span>
              </div>
            </div>
          )}

          {genMut.isPending && (
            <div className={`${aspectClass} w-full rounded-lg border-2 border-dashed border-border-base bg-surface-base flex flex-col items-center justify-center gap-2`}>
              <div className="w-6 h-6 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
              <p className="text-xs text-ink-3">Generant amb DALL·E 3...</p>
              <p className="text-[10px] text-ink-3">Pot trigar 15–30 segons</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 pb-5 pt-3 border-t border-border-base flex justify-between gap-3">
          <AMGButton
            variant="ghost"
            onClick={onClose}
          >
            Cancel·lar
          </AMGButton>

          <div className="flex gap-2">
            <AMGButton
              variant="secondary"
              disabled={!prompt.trim() || genMut.isPending}
              onClick={() => { setResult(null); genMut.mutate(); }}
            >
              {genMut.isPending ? 'Generant...' : result ? 'Regenerar' : 'Generar imatge'}
            </AMGButton>

            {result && (
              <AMGButton onClick={() => { onAccept(result); onClose(); }}>
                Usar aquesta imatge
              </AMGButton>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
