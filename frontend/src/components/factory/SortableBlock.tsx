'use client';

import type { FC } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { BlockRenderer } from './BlockRenderer';
import type { Block, PageStyles } from '@/services/factory';

interface Props {
  block: Block;
  styles: PageStyles;
  isSelected: boolean;
  onSelect: (id: string) => void;
  onRemove: (id: string) => void;
  onDuplicate: (id: string) => void;
  onUpdateProps: (blockId: string, props: Partial<Record<string, unknown>>) => void;
}

export const SortableBlock: FC<Props> = ({ block, styles, isSelected, onSelect, onRemove, onDuplicate, onUpdateProps }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: block.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
    position: 'relative' as const,
  };

  return (
    <div ref={setNodeRef} style={style} className="group">
      <div
        {...attributes}
        {...listeners}
        className="absolute top-2 left-2 z-20 w-5 h-5 flex flex-col items-center justify-center gap-[3px] cursor-grab active:cursor-grabbing opacity-0 group-hover:opacity-60 hover:!opacity-100 transition"
        title="Arrossega per moure"
      >
        {Array.from({ length: 3 }).map((_, i) => (
          <span key={i} className="w-3 h-[2px] bg-[#FF9A3C] rounded" />
        ))}
      </div>
      <BlockRenderer
        block={block}
        styles={styles}
        isSelected={isSelected}
        onSelect={onSelect}
        onRemove={onRemove}
        onDuplicate={onDuplicate}
        onUpdateProps={(props) => onUpdateProps(block.id, props)}
      />
    </div>
  );
};
