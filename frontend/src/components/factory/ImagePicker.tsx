'use client';

import { useState, useRef, type FC } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listAssets, uploadAsset } from '@/services/assets';
import { getCurrentUser } from '@/services/auth';

interface Props {
  onSelect: (url: string) => void;
  onClose: () => void;
}

export const ImagePicker: FC<Props> = ({ onSelect, onClose }) => {
  const queryClient = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const user = getCurrentUser();

  const { data: assets = [] } = useQuery({
    queryKey: ['assets', user?.tenantId],
    queryFn: () => listAssets(user!.tenantId!),
    enabled: !!user?.tenantId,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadAsset(user!.tenantId!, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets', user?.tenantId] });
      setUploading(false);
    },
    onError: () => setUploading(false),
  });

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    uploadMutation.mutate(file);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
      <div className="bg-[#13132a] border border-border-medium rounded-lg w-full max-w-lg max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between p-4 border-b border-border-base">
          <span className="f-mono text-xs uppercase tracking-wider text-ink-0">Selector d&apos;imatges</span>
          <button onClick={onClose} className="text-ink-1 hover:text-white text-sm">×</button>
        </div>

        <div className="p-4 border-b border-border-base">
          <button
            onClick={() => fileRef.current?.click()}
            disabled={uploading}
            className="w-full py-2 bg-[#FF6B00] text-black text-xs font-semibold rounded hover:bg-[#FF9A3C] disabled:opacity-50 f-mono uppercase tracking-wider"
          >
            {uploading ? 'Pujant...' : '+ Pujar imatge'}
          </button>
          <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
        </div>

        <div className="flex-1 overflow-auto p-4 grid grid-cols-3 gap-2">
          {assets.length === 0 ? (
            <div className="col-span-3 text-center text-ink-3 text-xs py-8">
              Cap imatge pujada
            </div>
          ) : (
            assets.map((asset) => (
              <button
                key={asset.id}
                onClick={() => onSelect(asset.url)}
                className="aspect-square bg-[#0d0d1a] rounded overflow-hidden hover:ring-2 hover:ring-[#FF6B00] transition"
              >
                <img
                  src={asset.thumbnailUrl || asset.url}
                  alt={asset.originalName}
                  className="w-full h-full object-cover"
                />
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
