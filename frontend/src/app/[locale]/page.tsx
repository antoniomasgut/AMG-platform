import { LandingHeader } from '@/components/landing/LandingHeader';
import { HeroSection } from '@/components/landing/HeroSection';
import { ProblemSection } from '@/components/landing/ProblemSection';
import { PricingSection } from '@/components/landing/PricingSection';
import { ServicesSection } from '@/components/landing/ServicesSection';
import { HowItWorksSection } from '@/components/landing/HowItWorksSection';
import { PortalSection } from '@/components/landing/PortalSection';
import { CTASection } from '@/components/landing/CTASection';
import { LandingFooter } from '@/components/landing/LandingFooter';

export default function LandingPage() {
  return (
    <div className="bg-bg-0 text-ink-0">
      <LandingHeader />
      <HeroSection />
      <ProblemSection />
      <PricingSection />
      <ServicesSection />
      <HowItWorksSection />
      <PortalSection />
      <CTASection />
      <LandingFooter />
    </div>
  );
}
