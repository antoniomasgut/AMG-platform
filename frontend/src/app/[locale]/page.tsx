import { getTranslations } from 'next-intl/server';
import { Header } from '@/components/landing/Header';
import { Hero } from '@/components/landing/Hero';
import { ProblemSection } from '@/components/landing/ProblemSection';
import { ServicesSection } from '@/components/landing/ServicesSection';
import { HowItWorks } from '@/components/landing/HowItWorks';
import { PricingSection } from '@/components/landing/PricingSection';
import { CTASection } from '@/components/landing/CTASection';
import { Footer } from '@/components/landing/Footer';
import { CookieConsentBanner } from '@/components/landing/CookieConsentBanner';

export default function LandingPage() {
  return (
    <>
      <Header />
      <main>
        <Hero />
        <ProblemSection />
        <ServicesSection />
        <HowItWorks />
        <PricingSection />
        <CTASection />
      </main>
      <Footer />
      <CookieConsentBanner />
    </>
  );
}
