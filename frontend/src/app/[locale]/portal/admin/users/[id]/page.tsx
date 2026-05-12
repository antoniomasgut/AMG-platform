'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useParams } from 'next/navigation';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { I } from '@/components/ui/icons';
import { getUser, updateUser } from '@/services/userService';
import { getCurrentUser } from '@/services/auth';

export default function EditUserPage() {
  const t = useTranslations('admin');
  const router = useRouter();
  const params = useParams();
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [form, setForm] = useState({ email: '', name: '', role: '', tenantId: '', isActive: true, password: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  useEffect(() => {
    const init = async () => {
      try {
        const cu = getCurrentUser();
        setCurrentUser(cu);
        const u = await getUser(params.id as string);
        setForm({
          email: u.email,
          name: u.name,
          role: u.role,
          tenantId: u.tenant?.id || '',
          isActive: u.isActive,
          password: '',
        });
      } catch (err: any) {
        setErrors({ load: err.message });
      } finally {
        setFetching(false);
      }
    };
    init();
  }, [params.id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload: any = { email: form.email, name: form.name };
      if (form.password) payload.password = form.password;
      await updateUser(params.id as string, payload);
      router.push('/portal/admin/users');
    } catch (err: any) {
      setErrors({ submit: err.message });
    } finally {
      setLoading(false);
    }
  };

  if (fetching) {
    return (
      <div className="p-4 sm:p-8 flex items-center justify-center">
        <span className="w-6 h-6 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-8 max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <AMGButton variant="ghost" size="sm" onClick={() => router.back()}>
          <I.Chevron size={16} className="rotate-180" />
        </AMGButton>
        <h1 className="f-display font-black text-lg text-white">{t('users.edit')}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <AMGInput label={t('users.fields.email' as any)} type="email" value={form.email}
          onChange={(e) => setForm({...form, email: e.target.value})} />
        <AMGInput label={t('users.fields.name' as any)} value={form.name}
          onChange={(e) => setForm({...form, name: e.target.value})} />

        {/* Role (read-only on edit) */}
        <div>
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">
            {t('users.fields.role' as any)}
          </span>
          <div className="h-10 flex items-center px-3 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] rounded text-sm text-ink-2/50">
            {t(`users.roles.${form.role}` as any)}
          </div>
        </div>

        {/* Tenant (read-only on edit) */}
        {form.role === 'CLIENT' && (
          <AMGInput label={t('users.fields.tenant' as any)} value={form.tenantId || '-'} onChange={() => {}} />
        )}

        {/* Active toggle */}
        <div className="flex items-center gap-3">
          <span className="f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8]">
            {t('users.fields.isActive' as any)}
          </span>
          <button type="button"
            className={`w-10 h-5 rounded-full transition-colors ${form.isActive ? 'bg-accent' : 'bg-[#1a1a2e]/80 border border-white/5'}`}
            onClick={() => setForm({...form, isActive: !form.isActive})}>
            <div className={`w-4 h-4 bg-white rounded-full transition-transform ${form.isActive ? 'translate-x-5' : 'translate-x-0.5'}`} />
          </button>
        </div>

        {/* Password (optional, SUPER_ADMIN only) */}
        {currentUser?.role === 'SUPER_ADMIN' && (
          <AMGInput label="Nova contrasenya (deixar buit per no canviar)" type="password" value={form.password}
            onChange={(e) => setForm({...form, password: e.target.value})} />
        )}

        {errors.submit && (
          <div className="bg-[rgba(255,68,68,0.12)] border border-[rgba(255,68,68,0.35)] rounded p-3 text-sm text-[#ff6666]">
            {errors.submit}
          </div>
        )}

        <div className="flex gap-3 pt-2">
          <AMGButton variant="primary" type="submit" loading={loading}>{t('save')}</AMGButton>
          <AMGButton variant="ghost" type="button" onClick={() => router.back()}>{t('cancel')}</AMGButton>
        </div>
      </form>
    </div>
  );
}
