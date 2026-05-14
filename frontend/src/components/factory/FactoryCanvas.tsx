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

  return (
    <div className="flex-1 overflow-auto bg-[#1a1a2e] flex justify-center p-4">
      <div className={`${widthClass} w-full min-h-full bg-white shadow-2xl`} style={{ fontFamily: styles.fontFamily }}>
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
                />
              ))}
            </SortableContext>
          </DndContext>
        )}
      </div>
    </div>
  );
};
