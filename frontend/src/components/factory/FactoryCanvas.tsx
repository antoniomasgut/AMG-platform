'use client';

import { useMemo, useCallback, type FC } from 'react';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { useEditorStore } from '@/store/editor';
import { SortableBlock } from './SortableBlock';

export const FactoryCanvas: FC = () => {
  const content = useEditorStore((s) => s.content);
  const styles = useEditorStore((s) => s.styles);
  const previewMode = useEditorStore((s) => s.previewMode);
  const selectedBlockId = useEditorStore((s) => s.selectedBlockId);
  const selectBlock = useEditorStore((s) => s.selectBlock);
  const removeBlock = useEditorStore((s) => s.removeBlock);
  const moveBlock = useEditorStore((s) => s.moveBlock);
  const updateBlockProps = useEditorStore((s) => s.updateBlockProps);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const blockIds = useMemo(() => content.blocks.map((b) => b.id), [content.blocks]);

  const widthClass =
    previewMode === 'mobile'
      ? 'max-w-[375px]'
      : previewMode === 'tablet'
      ? 'max-w-[768px]'
      : 'max-w-full';

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const newIndex = content.blocks.findIndex((b) => b.id === over.id);
    moveBlock(String(active.id), newIndex);
  }, [content.blocks, moveBlock]);

  const waNumber = styles.whatsappNumber?.replace(/\D/g, '');

  return (
    <div className="flex-1 overflow-auto bg-[#1a1a2e] flex justify-center p-4">
      <div
        className={`${widthClass} w-full min-h-full bg-white shadow-2xl relative`}
        style={{ fontFamily: styles.fontBody || styles.fontHeading }}
      >
        {content.blocks.length === 0 ? (
          <div className="flex items-center justify-center h-64 text-gray-400 text-sm">
            Selecciona un bloc del catàleg per començar
          </div>
        ) : (
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
            <SortableContext items={blockIds} strategy={verticalListSortingStrategy}>
              {content.blocks.map((block) => (
                <SortableBlock
                  key={block.id}
                  block={block}
                  styles={styles}
                  isSelected={block.id === selectedBlockId}
                  onSelect={selectBlock}
                  onRemove={removeBlock}
                  onUpdateProps={updateBlockProps}
                />
              ))}
            </SortableContext>
          </DndContext>
        )}

        {/* Botó WhatsApp flotant — previsualització */}
        {waNumber && (
          <a
            href={`https://wa.me/${waNumber}`}
            target="_blank"
            rel="noopener noreferrer"
            title="Contacta per WhatsApp"
            style={{ background: '#25d366' }}
            className="absolute bottom-6 right-6 w-14 h-14 rounded-full flex items-center justify-center shadow-lg hover:scale-110 transition-transform z-50"
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="#fff">
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z"/>
              <path d="M12 0C5.373 0 0 5.373 0 12c0 2.123.554 4.117 1.528 5.845L0 24l6.336-1.508A11.934 11.934 0 0012 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 21.818a9.804 9.804 0 01-5.003-1.368l-.358-.213-3.722.885.916-3.618-.234-.372A9.807 9.807 0 012.182 12C2.182 6.562 6.562 2.182 12 2.182S21.818 6.562 21.818 12 17.438 21.818 12 21.818z"/>
            </svg>
          </a>
        )}
      </div>
    </div>
  );
};
