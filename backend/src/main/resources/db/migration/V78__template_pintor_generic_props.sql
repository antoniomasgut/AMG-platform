-- Correcció V77: default_props de la plantilla 'pintor-professional' eren
-- específics d'un negoci concret (Toni Planas). Substituïm per contingut
-- genèric i reutilitzable per a qualsevol pintor/lacador.

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'pintor-professional')

UPDATE template_sections ts
SET default_props = CASE ts.sort_order

  WHEN 1 THEN $p$
{
  "title": "El pintor professional que necessites",
  "subtitle": "Serveis de pintura interior, lacat i façanes amb garantia de satisfacció. Pressupost gratuït en 24h.",
  "ctaText": "Demana pressupost gratuït",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}
$p$

  WHEN 2 THEN $p$
{
  "items": [
    {"value": "15+",  "label": "anys d'experiència",       "icon": "⭐"},
    {"value": "500+", "label": "projectes completats",     "icon": "🏠"},
    {"value": "4.9★", "label": "valoració Google",         "icon": "✅"},
    {"value": "24h",  "label": "resposta garantida",       "icon": "⚡"}
  ]
}
$p$

  WHEN 3 THEN $p$
{
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "home",
      "title": "Pintura interior",
      "description": "Pintem habitatges, locals i oficines amb acabats impecables. Protegim mobles i superfícies i deixem tot net."
    },
    {
      "icon": "award",
      "title": "Lacats i esmalts",
      "description": "Lacats professionals en fusteria, portes i mobiliari. Acabats llisos, satinats o brillants segons les teves preferències."
    },
    {
      "icon": "shield",
      "title": "Façanes i exteriors",
      "description": "Pintura de façanes amb tractaments impermeabilitzants i antihumitat. Treballem amb plataformes elevadores amb totes les garanties de seguretat."
    },
    {
      "icon": "leaf",
      "title": "Protecció de fusta",
      "description": "Tractaments per a portes, finestres, pergoles i tarimes. Productes de qualitat que protegeixen i embelleixen la fusta."
    },
    {
      "icon": "star",
      "title": "Sòls i revestiments",
      "description": "Microciment, resines i paviments continus. Solucions modernes i duradores per a qualsevol tipus de superfície."
    },
    {
      "icon": "zap",
      "title": "Pintura decorativa",
      "description": "Efectes decoratius, empapelats i acabats especials. Transformem els teus espais amb tècniques artesanals i modernes."
    }
  ]
}
$p$

  WHEN 4 THEN $p$
{
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Maria G.",
      "text": "Molt contents amb el resultat. Van ser puntuals, nets i van deixar la casa millor del que esperàvem. Totalment recomanables.",
      "avatarUrl": ""
    },
    {
      "name": "Joan F.",
      "text": "Excel·lent servei. Van fer el pressupost de seguida i van complir els terminis. La qualitat de l'acabat és molt bona.",
      "avatarUrl": ""
    },
    {
      "name": "Anna R.",
      "text": "Professionals i de confiança. Ens van assessorar en la tria de colors i el resultat final va superar les expectatives.",
      "avatarUrl": ""
    }
  ]
}
$p$

  WHEN 5 THEN $p$
{
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "En quant temps reberé el pressupost?",
      "answer": "En menys de 24 hores laborables. El pressupost és completament gratuït i sense cap compromís per a tu."
    },
    {
      "question": "Podeu treballar mentre vivim a la casa?",
      "answer": "Sí. Treballem per fases i protegim els mobles i el terra. Intentem minimitzar les molèsties perquè puguis seguir amb la teva rutina."
    },
    {
      "question": "Quina garantia teniu en els vostres treballs?",
      "answer": "Tots els treballs inclouen garantia de satisfacció. Si alguna cosa no queda com esperaves, tornem i ho solucionem sense cost addicional."
    },
    {
      "question": "Treballeu amb particulars i empreses?",
      "answer": "Sí, atenem tant particulars (pisos, xalets) com empreses (locals, oficines, hotels i edificis comercials)."
    },
    {
      "question": "Quin és el vostre horari de treball?",
      "answer": "Treballem de dilluns a divendres de 8h a 18h. Per a projectes d'empresa o urgències podem acordar horaris especials."
    }
  ]
}
$p$

  WHEN 6 THEN $p$
{
  "title": "Demana el teu pressupost gratuït",
  "subtitle": "Ens posem en contacte en menys de 24h"
}
$p$

  ELSE default_props
END
WHERE ts.template_id = (SELECT id FROM tpl);
