import dynamic from 'next/dynamic';
import { LandingHeader } from '@/components/landing/LandingHeader';
import { HeroSection } from '@/components/landing/HeroSection';
import { FelanitxGrantSection } from '@/components/landing/FelanitxGrantSection';
import { ProblemSection } from '@/components/landing/ProblemSection';
import { PricingSection } from '@/components/landing/PricingSection';
import { ServicesSection } from '@/components/landing/ServicesSection';
import { HowItWorksSection } from '@/components/landing/HowItWorksSection';
import { SectorsSection } from '@/components/landing/SectorsSection';
import { AltresNegocisSection } from '@/components/landing/AltresNegocisSection';
import { PortalSection } from '@/components/landing/PortalSection';
import { TestimonialsSection } from '@/components/landing/TestimonialsSection';
import { CTASection } from '@/components/landing/CTASection';
import { LandingFooter } from '@/components/landing/LandingFooter';

const AgencyChatWidget = dynamic(
  () => import('@/components/landing/AgencyChatWidget').then(m => m.AgencyChatWidget),
  { ssr: false }
);

export default function LandingPage() {
  return (
    <div className="bg-bg-0 text-ink-0">
      <LandingHeader />
      <HeroSection />
      <FelanitxGrantSection />
      <ProblemSection />
      <PricingSection />
      <ServicesSection />
      <HowItWorksSection />
      <SectorsSection />
      <AltresNegocisSection />
      <PortalSection />
      <TestimonialsSection />
      <CTASection />
      <LandingFooter />
      <AgencyChatWidget />
    </div>
  );
}
