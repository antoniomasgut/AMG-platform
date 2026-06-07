'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useParams } from 'next/navigation';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { IconSet } from '@/components/ui/icons';
import { PortalShell } from '@/components/portal/PortalShell';
import { createUser } from '@/services/userService';
import { listTenants } from '@/services/tenantService';
import { getCurrentUser } from '@/services/auth';

export default function NewUserPage() {
  const t = useTranslations('admin');
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [form, setForm] = useState({ email: '', password: '', name: '', role: 'CLIENT', tenantId: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [tenants, setTenants] = useState<any[]>([]);

  useEffect(() => {
    const user = getCurrentUser();
    setCurrentUser(user);
    listTenants({ page: 0, size: 100 }).then(r => setTenants(r.content)).catch(() => {});
  }, []);

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.email) e.email = 'Email requerit';
    if (!form.password || form.password.length < 4) e.password = 'Mínim 4 caràcters';
    if (!form.name) e.name = 'Nom requerit';
    if (form.role === 'CLIENT' && !form.tenantId) e.tenantId = 'Selecciona un tenant';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      await createUser(form as any);
      router.push('/portal/admin/users');
    } catch (err: any) {
      setErrors({ submit: err.message });
    } finally {
      setLoading(false);
    }
  };

  const roles = currentUser?.role === 'SUPER_ADMIN'
    ? ['SUPER_ADMIN', 'ADMIN', 'CLIENT']
    : ['CLIENT'];

  return (
    <PortalShell breadcrumb="admin · usuaris · nou" backHref={`/${locale}/portal/admin/users`}>
    <div className="p-4 sm:p-8 max-w-2xl space-y-6">
      <h1 className="f-display font-black text-lg text-white">{t('users.new')}</h1>

      <form onSubmit={handleSubmit} className="space-y-5">
        <AMGInput label={t('users.fields.email' as any)} type="email" value={form.email}
          onChange={(e) => setForm({...form, email: e.target.value})} error={errors.email} />
        <AMGInput label={t('users.fields.password' as any)} type="password" value={form.password}
          onChange={(e) => setForm({...form, password: e.target.value})} error={errors.password} />
        <AMGInput label={t('users.fields.name' as any)} value={form.name}
          onChange={(e) => setForm({...form, name: e.target.value})} error={errors.name} />

        <div>
          <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">
            {t('users.fields.role' as any)}
          </span>
          {currentUser?.role === 'ADMIN' ? (
            <div className="h-10 flex items-center px-3 bg-[#1a1a2e]/80 border border-[rgba(255,107,0,0.14)] rounded text-sm text-ink-2/50">
              {t('users.roles.CLIENT' as any)}
            </div>
          ) : (
            <div className="flex flex-wrap gap-2">
              {roles.map(role => (
                <AMGButton key={role} variant={form.role === role ? 'primary' : 'ghost'} size="sm"
                  type="button" onClick={() => setForm({...form, role, tenantId: ''})}>
                  {t(`users.roles.${role}` as any)}
                </AMGButton>
              ))}
            </div>
          )}
        </div>

        {form.role === 'CLIENT' && (
          <div>
            <span className="block f-mono uppercase text-[10px] tracking-[0.14em] text-[#94a3b8] mb-1.5">
              {t('users.fields.tenant' as any)}
            </span>
            <div className="flex flex-wrap gap-2">
              {tenants.map(tenant => (
                <AMGButton key={tenant.id} variant={form.tenantId === tenant.id ? 'primary' : 'ghost'} size="sm"
                  type="button" onClick={() => setForm({...form, tenantId: tenant.id})}>
                  {tenant.name}
                </AMGButton>
              ))}
            </div>
            {errors.tenantId && <span className="block mt-1 text-[11px] text-[#ff6666]">{errors.tenantId}</span>}
          </div>
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
    </PortalShell>
  );
}
