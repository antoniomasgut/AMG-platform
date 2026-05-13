'use client';

import type { FC } from 'react';
import { useEditorStore } from '@/store/editor';
import { BlockRenderer } from './BlockRenderer';

export const FactoryCanvas: FC = () => {
  const content = useEditorStore((s) => s.content);
  const styles = useEditorStore((s) => s.styles);
  const previewMode = useEditorStore((s) => s.previewMode);
  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);
  const selectBlock = useEditorStore((s) => s.selectBlock);
  const removeBlock = useEditorStore((s) => s.removeBlock);

  const widthClass = previewMode === 'mobile' ? 'max-w-[375px]' : previewMode === 'tablet' ? 'max-w-[768px]' : 'max-w-full';

  return (
    <div className="flex-1 overflow-auto bg-[#1a1a2e] flex justify-center p-4">
      <div className={`${widthClass} w-full min-h-full bg-white shadow-2xl`} style={{ fontFamily: styles.fontFamily }}>
        {content.blocks.length === 0 ? (
          <div className="flex items-center justify-center h-64 text-gray-400 text-sm">
            Selecciona un bloc del catàleg per començar
          </div>
        ) : (
          content.blocks.map((block) => (
            <BlockRenderer
              key={block.id}
              block={block}
              styles={styles}
              isSelected={block.id === selectedBlockId}
              onSelect={selectBlock}
              onRemove={removeBlock}
            />
          ))
        )}
      </div>
    </div>
  );
};
