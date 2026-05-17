import { z } from 'zod';

export const slugSchema = z
  .string()
  .min(1, 'El slug és obligatori')
  .regex(/^[a-z0-9]+(-[a-z0-9]+)*$/, 'Només minúscules, dígits i guions');

export const nameSchema = z
  .string()
  .min(2, 'Mínim 2 caràcters')
  .max(100, 'Màxim 100 caràcters');

export const descriptionSchema = z
  .string()
  .max(500, 'Màxim 500 caràcters')
  .optional()
  .or(z.literal(''));

export const priceSchema = z
  .string()
  .min(1, 'El preu és obligatori')
  .refine((v) => !isNaN(parseFloat(v)) && parseFloat(v) >= 0, 'Ha de ser un número positiu');

export const phaseNameSchema = z
  .string()
  .min(2, 'Mínim 2 caràcters')
  .max(100, 'Màxim 100 caràcters');

export const sortOrderSchema = z
  .string()
  .optional()
  .refine((v) => !v || (!isNaN(parseInt(v)) && parseInt(v) > 0), 'Ha de ser un número positiu')
  .or(z.literal(''));

// Composite schemas
export const profileFormSchema = z.object({
  name: nameSchema,
  slug: slugSchema,
  description: descriptionSchema,
});

export const serviceFormSchema = z.object({
  name: nameSchema,
  slug: slugSchema,
  description: descriptionSchema,
  cost: priceSchema,
  salePrice: priceSchema,
});

export type ProfileFormData = z.infer<typeof profileFormSchema>;
export type ServiceFormData = z.infer<typeof serviceFormSchema>;
