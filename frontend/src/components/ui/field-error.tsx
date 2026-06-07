import { IconSet } from './icons';

export function FieldError({ error }: { error?: string | null }) {
  if (!error) return null;
  return (
    <div className="flex items-center gap-1 mt-1 text-red-400 f-mono text-xs" role="alert">
      <IconSet.AlertCircle size={12} stroke="#f87171" />
      <span>{error}</span>
    </div>
  );
}
