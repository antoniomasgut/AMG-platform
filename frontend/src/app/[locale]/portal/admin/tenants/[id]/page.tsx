'use client';

import { useTranslations } from 'next-intl';
import { useState, useEffect } from 'react';
import { useRouter } from '@/i18n/navigation';
import { useParams } from 'next/navigation';
import { AMGButton } from '@/components/ui/button';
import { AMGInput } from '@/components/ui/input';
import { I } from '@/components/ui/icons';
import { getTenant, updateTenant } from '@/services/tenantService';

export default function EditTenantPage() {
  const t = useTranslations('admin');
  const router = useRouter();
  const params = useParams();
  const [form, setForm] = useState({ name: '', slug: '', email: '', phone: '', address: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const tenant = await getTenant(params.id as string);
        setForm({
          name: tenant.name,
          slug: tenant.slug,
          email: tenant.email || '',
          phone: tenant.phone || '',
          address: tenant.address || '',
        });
      } catch (err: any) {
        setErrors({ load: err.message });
      } finally {
        setFetching(false);
      }
    };
    load();
  }, [params.id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await updateTenant(params.id as string, form);
      router.push('/portal/admin/tenants');
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
        <h1 className="f-display font-black text-lg text-white">{t('tenants.edit')}</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <AMGInput label={t('tenants.fields.name' as any)} value={form.name}
          onChange={(e) => setForm({...form, name: e.target.value})} />
        <AMGInput label={t('tenants.fields.slug' as any)} value={form.slug} mono
          onChange={(e) => setForm({...form, slug: e.target.value})} />
        <AMGInput label={t('tenants.fields.email' as any)} type="email" value={form.email}
          onChange={(e) => setForm({...form, email: e.target.value})} />
        <AMGInput label={t('tenants.fields.phone' as any)} value={form.phone}
          onChange={(e) => setForm({...form, phone: e.target.value})} />
        <AMGInput label={t('tenants.fields.address' as any)} value={form.address}
          onChange={(e) => setForm({...form, address: e.target.value})} />

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
