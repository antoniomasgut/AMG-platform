'use client';

import React from 'react';

export interface Column<T> {
  label: string;
  render: (item: T) => React.ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  loading?: boolean;
  emptyMessage?: string;
  keyExtractor: (item: T) => string;
}

export function DataTable<T>({
  columns,
  data,
  page,
  totalPages,
  onPageChange,
  loading = false,
  emptyMessage = 'No s\'han trobat resultats',
  keyExtractor,
}: DataTableProps<T>) {
  if (loading) {
    return (
      <div className="amg-card border border-white/5 overflow-hidden">
        <div className="flex items-center justify-center py-16">
          <span className="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="amg-card border border-white/5 overflow-hidden">
        <div className="flex items-center justify-center py-16">
          <span className="text-sm text-ink-2/50">{emptyMessage}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="amg-card border border-white/5 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/5 text-ink-2/50 f-mono text-[10px] uppercase tracking-widest">
              {columns.map((col, i) => (
                <th key={i} className={`text-left p-3 font-normal ${col.className || ''}`}>
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr
                key={keyExtractor(item)}
                className="border-b border-white/5 last:border-none text-ink-2 hover:bg-white/5 transition-colors"
              >
                {columns.map((col, i) => (
                  <td key={i} className={`p-3 ${col.className || ''}`}>
                    {col.render(item)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between px-3 py-3 border-t border-white/5">
          <span className="text-xs text-ink-2/50 f-mono">
            Pàgina {page + 1} de {totalPages}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page <= 0}
              className="px-3 h-7 text-xs f-mono uppercase tracking-wider text-ink-2 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              Anterior
            </button>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 h-7 text-xs f-mono uppercase tracking-wider text-ink-2 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              Següent
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
