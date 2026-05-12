import React from 'react';
import type { FC } from 'react';

interface InputProps {
  label?: string;
  placeholder?: string;
  value?: string;
  mono?: boolean;
  icon?: FC<{ size?: number; stroke?: string; className?: string }>;
  hint?: string;
  error?: string;
  type?: string;
  className?: string;
  autoFocus?: boolean;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export function AMGInput({
  label, placeholder, value, mono, icon: Ico, hint, error, type = 'text',
  className = '', autoFocus, onChange,
}: InputProps) {
  return (
    <label className={`block ${className}`}>
      {label && <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">{label}</span>}
      <div className="relative flex items-center h-10 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] focus-within:border-[#FF6B00] transition">
        {Ico && <div className="pl-3 text-[#64748b]"><Ico size={14} /></div>}
        <input
          type={type}
          defaultValue={value}
          placeholder={placeholder}
          autoFocus={autoFocus}
          onChange={onChange}
          className={`${mono ? 'f-mono' : ''} flex-1 bg-transparent outline-none px-3 text-sm text-[#e2e8f0] placeholder:text-[#64748b]`}
        />
      </div>
      {hint && <span className="block mt-1 text-[11px] text-[#64748b]">{hint}</span>}
      {error && <span className="block mt-1 text-[11px] text-[#ff6666]">{error}</span>}
    </label>
  );
}
