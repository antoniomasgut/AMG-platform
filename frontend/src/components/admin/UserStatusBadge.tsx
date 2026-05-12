'use client';

import React from 'react';
import { AMGBadge } from '@/components/ui/badge';

interface UserStatusBadgeProps {
  isActive: boolean;
  isBlocked: boolean;
}

export function UserStatusBadge({ isActive, isBlocked }: UserStatusBadgeProps) {
  if (isBlocked) {
    return <AMGBadge tone="danger">Blocat</AMGBadge>;
  }
  if (!isActive) {
    return <AMGBadge tone="neutral">Inactiu</AMGBadge>;
  }
  return <AMGBadge tone="success">Actiu</AMGBadge>;
}
