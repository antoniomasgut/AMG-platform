'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  getTemplate, previewTemplate,
} from '@/services/documents';

export default function PreviewTemplatePage() {
  const params = useParams();
  const id = params.id as string;

  const { data: template } = useQuery({
    queryKey: ['document-template', id],
    queryFn: () => getTemplate(id),
    enabled: !!id,
  });

  const { data: html } = useQuery({
    queryKey: ['template-preview', id],
    queryFn: () => previewTemplate(id),
    enabled: !!id,
  });

  return (
    <div className="min-h-dvh bg-[#0d0d1a] flex flex-col">
      <div className="h-12 border-b border-border-base flex items-center px-4 shrink-0">
        <span className="f-mono text-label uppercase text-accent-light tracking-widest text-xs">
          Previsualització · {template?.name || 'Carregant...'}
        </span>
      </div>
      <div className="flex-1 overflow-auto p-4 sm:p-8">
        {html ? (
          <div className="max-w-[800px] mx-auto bg-white text-black rounded shadow-lg overflow-hidden">
            <iframe
              srcDoc={html}
              className="w-full min-h-[800px]"
              title="Preview"
              sandbox="allow-same-origin"
            />
          </div>
        ) : (
          <div className="flex justify-center py-20">
            <span className="w-5 h-5 border-2 border-[#FF6B00] border-t-transparent rounded-full animate-spin" />
          </div>
        )}
      </div>
    </div>
  );
}
