'use client';

import { useState, useEffect, type FC } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getChatContext, saveChatContext, deleteChatContext } from '@/services/factory';

const EXAMPLE_PROMPT = `Ets l'assistent virtual de [NOM DEL NEGOCI], [descripció breu] a [Ciutat].

SERVEIS I PREUS:
- [Servei 1]: [preu]
- [Servei 2]: [preu]
- [Servei 3]: [preu]

HORARI: [dies i hores]
UBICACIÓ: [adreça]
CONTACTE: Tel. [telèfon] · [email]

NORMES:
- Respon SEMPRE en català (o en la llengua del visitant)
- Respostes curtes, màx. 2-3 frases, estil WhatsApp
- Si demanen cita, pregunta quin servei i quin dia/hora
- Si no saps alguna cosa, convida'ls a trucar
- No inventis preus ni serveis`;

interface Props {
  landingId: string;
}

export const ChatContextPanel: FC<Props> = ({ landingId }) => {
  const qc = useQueryClient();
  const [businessName, setBusinessName] = useState('');
  const [systemPrompt, setSystemPrompt] = useState('');
  const [saved, setSaved] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [hasContext, setHasContext] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['chat-context', landingId],
    queryFn: () => getChatContext(landingId),
    retry: false,
  });

  useEffect(() => {
    if (data) {
      setBusinessName(data.businessName ?? '');
      setSystemPrompt(data.systemPrompt ?? '');
      setHasContext(true);
    }
  }, [data]);

  const saveMutation = useMutation({
    mutationFn: () => saveChatContext(landingId, { businessName, systemPrompt }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['chat-context', landingId] });
      setHasContext(true);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteChatContext(landingId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['chat-context', landingId] });
      setBusinessName('');
      setSystemPrompt('');
      setHasContext(false);
      setDeleted(true);
      setTimeout(() => setDeleted(false), 2000);
    },
  });

  const isPending = saveMutation.isPending || deleteMutation.isPending;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-20">
        <span className="w-4 h-4 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3 p-3">
      {/* Header */}
      <div className="flex items-center gap-2">
        <span className="text-[#FF6B00]">💬</span>
        <span className="f-mono text-[9px] uppercase tracking-wider text-ink-1">Bot de xat</span>
        {hasContext && (
          <span className="ml-auto f-mono text-[9px] text-green-400">Actiu</span>
        )}
      </div>

      <p className="text-xs text-ink-3 leading-relaxed">
        El bot respon als visitants de la landing usant aquest context.
        Inclou serveis, preus, horari i normes de comportament.
      </p>

      {/* Nom del negoci */}
      <div className="flex flex-col gap-1">
        <label className="f-mono text-[9px] uppercase tracking-wider text-ink-2">
          Nom del negoci
        </label>
        <input
          value={businessName}
          onChange={(e) => setBusinessName(e.target.value)}
          placeholder="Ex: Estètica Mireia"
          className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1.5 text-xs text-ink-0 placeholder:text-ink-4 focus:outline-none focus:border-[#FF6B00]"
        />
      </div>

      {/* System prompt */}
      <div className="flex flex-col gap-1">
        <div className="flex items-center justify-between">
          <label className="f-mono text-[9px] uppercase tracking-wider text-ink-2">
            Context del bot (system prompt)
          </label>
          <button
            onClick={() => setSystemPrompt(EXAMPLE_PROMPT)}
            className="f-mono text-[9px] text-accent-light hover:text-[#FF9A3C] transition"
          >
            Exemple
          </button>
        </div>
        <textarea
          value={systemPrompt}
          onChange={(e) => setSystemPrompt(e.target.value)}
          placeholder="Escriu les instruccions del bot: qui és, quins serveis ofereix, preus, horari i com ha de respondre..."
          rows={14}
          className="w-full bg-[#0d0d1a] border border-border-base rounded px-2 py-1.5 text-xs text-ink-0 placeholder:text-ink-4 focus:outline-none focus:border-[#FF6B00] resize-none leading-relaxed font-mono"
        />
        <p className="f-mono text-[9px] text-ink-4">
          {systemPrompt.length} caràcters
        </p>
      </div>

      {/* Profanitat */}
      <div className="bg-[#0d0d1a] border border-border-base rounded p-2 text-xs text-ink-3">
        <span className="text-yellow-400">⚠</span>{' '}
        Si un visitant envia paraules malsonants, la conversa es tanca automàticament.
      </div>

      {/* Actions */}
      <div className="flex gap-2 pt-1">
        <button
          onClick={() => saveMutation.mutate()}
          disabled={isPending || !businessName.trim() || !systemPrompt.trim()}
          className="flex-1 h-8 f-mono text-[10px] uppercase tracking-wider rounded bg-[#FF6B00] text-black font-semibold hover:bg-[#FF9A3C] disabled:opacity-40 transition"
        >
          {saved ? '✓ Desat' : isPending ? 'Guardant...' : 'Guardar'}
        </button>
        {hasContext && (
          <button
            onClick={() => { if (confirm('Eliminar el context del bot?')) deleteMutation.mutate(); }}
            disabled={isPending}
            className="h-8 px-3 f-mono text-[10px] uppercase tracking-wider rounded border border-red-500/40 text-red-400 hover:bg-red-500/10 disabled:opacity-40 transition"
          >
            {deleted ? '✓' : 'Eliminar'}
          </button>
        )}
      </div>

      {/* Error */}
      {(saveMutation.isError || deleteMutation.isError) && (
        <p className="text-xs text-red-400">Error en desar. Torna-ho a provar.</p>
      )}
    </div>
  );
};
