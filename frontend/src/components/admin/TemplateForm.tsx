'use client';

import { useState } from 'react';
import { AMGButton } from '@/components/ui/button';
import type { CreateTemplateRequest } from '@/services/templates';

interface Props {
  initial?: CreateTemplateRequest;
  onSave: (data: CreateTemplateRequest) => void | Promise<void>;
  onCancel: () => void;
  loading?: boolean;
}

export function TemplateForm({ initial, onSave, onCancel, loading }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [slug, setSlug] = useState(initial?.slug ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSave({ name, slug, description });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="f-mono text-label uppercase text-ink-2 block mb-1">Nom</label>
        <input
          type="text"
          required
          placeholder="Nom de la plantilla"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            if (!initial && !slug) {
              setSlug(
                e.target.value
                  .toLowerCase()
                  .normalize('NFD').replace(/[̀-ͯ]/g, '')
                  .replace(/[^a-z0-9]+/g, '-')
                  .replace(/^-|-$/g, '')
              );
            }
          }}
          className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
        />
      </div>
      <div>
        <label className="f-mono text-label uppercase text-ink-2 block mb-1">Slug</label>
        <input
          type="text"
          required
          placeholder="nom-plantilla"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
        />
        <p className="f-mono text-[9px] text-ink-3 mt-1">Identificador únic per a la plantilla</p>
      </div>
      <div>
        <label className="f-mono text-label uppercase text-ink-2 block mb-1">Descripció</label>
        <input
          type="text"
          placeholder="Descripció curta"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="w-full bg-[#0d0d1a] border border-border-base px-3 h-10 text-sm text-ink-0 placeholder:text-ink-3 focus:outline-none focus:border-[#FF6B00]"
        />
      </div>
      <div className="flex gap-3 pt-2">
        <AMGButton type="submit" disabled={loading || !name || !slug} className="flex-1 justify-center">
          {loading ? 'Guardant...' : 'Guardar'}
        </AMGButton>
        <AMGButton type="button" variant="outline" onClick={onCancel}>Cancel·lar</AMGButton>
      </div>
    </form>
  );
}
