import * as Sentry from '@sentry/nextjs';

export function initSentry() {
  if (process.env.NEXT_PUBLIC_SENTRY_DSN) {
    Sentry.init({
      dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
      environment: process.env.NODE_ENV,
      tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 0,
      integrations: [
        Sentry.browserTracingIntegration(),
        Sentry.replayIntegration(),
      ],
      replaysSessionSampleRate: 0.01,
      replaysOnErrorSampleRate: 0.1,
    });
  }
}

export function captureError(error: unknown, context?: Record<string, unknown>) {
  if (process.env.NEXT_PUBLIC_SENTRY_DSN) {
    Sentry.captureException(error, { extra: context });
  }
}
