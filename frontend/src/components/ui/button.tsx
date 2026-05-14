'use client';

import React from 'react';
import type { FC, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: FC<{ size?: number; stroke?: string; className?: string }>;
  children?: ReactNode;
  className?: string;
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
  type?: 'button' | 'submit';
}

export function AMGButton({
  variant = 'primary',
  size = 'md',
  icon: Ico,
  children,
  className = '',
  disabled,
  loading,
  onClick,
  type = 'button',
}: ButtonProps) {
  const base = 'f-mono uppercase inline-flex items-center gap-2 font-semibold transition-all select-none';
  const sizes = { sm: 'px-3 h-8 text-caption', md: 'px-5 h-10 text-xs', lg: 'px-7 h-12 text-sm' };
  const variants = {
    primary: 'btn-clip bg-[#FF6B00] hover:bg-[#FF9A3C] text-black',
    secondary: 'btn-clip bg-[#1a1a2e] hover:bg-[#212140] text-ink-0 border border-border-strong',
    ghost: 'text-ink-0 hover:text-accent-light hover:bg-accent-subtle',
    danger: 'btn-clip bg-[#ff4444] hover:bg-[#ff6666] text-black',
    outline: 'btn-clip bg-transparent text-accent-light border border-[#FF6B00]/60 hover:bg-[#FF6B00]/10',
  };
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={`${base} ${sizes[size]} ${variants[variant]} ${className} ${disabled || loading ? 'opacity-40 cursor-not-allowed' : ''}`}
    >
      {loading ? (
        <span className="w-3 h-3 border-2 border-black/60 border-t-transparent rounded-full animate-spin" />
      ) : Ico ? (
        <Ico size={size === 'sm' ? 12 : 14} />
      ) : null}
      {children}
    </button>
  );
}
