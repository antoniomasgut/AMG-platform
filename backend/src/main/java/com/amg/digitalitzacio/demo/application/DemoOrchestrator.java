package com.amg.digitalitzacio.demo.application;

import com.amg.digitalitzacio.demo.api.dto.DemoFlowResponse;
import com.amg.digitalitzacio.demo.api.dto.DemoListResponse;
import com.amg.digitalitzacio.demo.api.dto.DemoStepResponse;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DemoOrchestrator implements DemoService {

    private static final Map<String, DemoFlowResponse> DEMOS = Map.of(
        "new-lead", new DemoFlowResponse(
            "new-lead",
            "Captació automàtica de leads",
            "Quan un client envia un formulari de contacte, el sistema detecta automàticament el lead, l'emmagatzema al CRM i t'avisa al Telegram.",
            List.of(
                new DemoStepResponse(1, "📝", "Formulari de contacte", "El client omple el formulari a la web (nom, telèfon, email, missatge)", "instantani"),
                new DemoStepResponse(2, "⚡", "Webhook n8n", "El formulari envia les dades a n8n via webhook", "< 1s"),
                new DemoStepResponse(3, "💾", "Creació al CRM", "n8n crea un lead a la base de dades amb totes les dades", "< 1s"),
                new DemoStepResponse(4, "📱", "Alerta Telegram", " reps un missatge al grup de gestió: 'Nou lead de Joan — 600123456'", "< 2s"),
                new DemoStepResponse(5, "📋", "Tasca de seguiment", "Es crea una tasca automàtica per contactar el lead en 24h", "< 1s")
            )
        ),
        "budget-email", new DemoFlowResponse(
            "budget-email",
            "Pressupost i email automatitzat",
            "Quan un client sol·licita un pressupost, el sistema el genera en PDF, l'envia per email i obre un enllaç de pagament.",
            List.of(
                new DemoStepResponse(1, "💰", "Sol·licitud de pressupost", " el client demana pressupost per una landing pro", "instantani"),
                new DemoStepResponse(2, "📄", "Generació de PDF", "n8n genera un PDF professional amb el detall del pressupost", "< 3s"),
                new DemoStepResponse(3, "📧", "Enviament per email", "El PDF s'envia automàticament al correu del client", "< 2s"),
                new DemoStepResponse(4, "🔗", "Enllaç de pagament", "S'adjunta un enllaç segur per acceptar i pagar el pressupost", "< 1s"),
                new DemoStepResponse(5, "✅", "Confirmació", "Quan el client paga, reps una alerta i el pressupost es marca com a acceptat", "automàtic")
            )
        ),
        "whatsapp-auto", new DemoFlowResponse(
            "whatsapp-auto",
            "WhatsApp Business automatitzat",
            "Respon automàticament missatges de WhatsApp amb informació comercial i deriva consultes complexes a l'equip.",
            List.of(
                new DemoStepResponse(1, "💬", "Missatge de WhatsApp", "Un client escriu 'Quant val una landing?' al WhatsApp del negoci", "instantani"),
                new DemoStepResponse(2, "🤖", "Resposta automàtica", "n8n detecta la paraula clau i respon amb la llista de preus", "< 1s"),
                new DemoStepResponse(3, "🏷️", "Etiquetatge del contacte", "El contacte s'etiqueta com a 'interessat-landing' al CRM", "< 1s"),
                new DemoStepResponse(4, "📊", "Estadística", "Es registra la consulta per al dashboard d'atenció al client", "< 1s"),
                new DemoStepResponse(5, "👤", "Derivació si escau", "Si el client demana informació molt específica, es deriva a un humà", "automàtic")
            )
        ),
        "reviews-auto", new DemoFlowResponse(
            "reviews-auto",
            "Gestió de ressenyes Google",
            "Monitoritza les ressenyes de Google Maps i t'alerta si reps una valoració negativa per poder respondre ràpidament.",
            List.of(
                new DemoStepResponse(1, "⭐", "Ressenya nova a Google", "Un client deixa una ressenya de 3 estrelles al teu negoci", "instantani"),
                new DemoStepResponse(2, "🔔", "Alerta al Telegram", " reps: '⚠️ Ressenya 3⭐ detectada — Bar España — 'No m'ha agradat l'espera''", "< 5 min"),
                new DemoStepResponse(3, "📝", "Tasca de resposta", "Es crea una tasca per respondre la ressenya com a màxim en 2h", "automàtic"),
                new DemoStepResponse(4, "📈", "Registre al dashboard", "La ressenya es registra al panell de reputació online", "< 1s"),
                new DemoStepResponse(5, "📊", "Resum setmanal", "Cada dilluns reps un resum de totes les ressenyes de la setmana", "setmanal")
            )
        )
    );

    @Override
    public DemoListResponse listDemos() {
        var summaries = DEMOS.values().stream()
                .map(d -> new DemoListResponse.DemoFlowSummary(d.id(), d.title(), d.description()))
                .toList();
        return new DemoListResponse(summaries);
    }

    @Override
    public DemoFlowResponse getDemo(String id) {
        var demo = DEMOS.get(id);
        if (demo == null) {
            throw new ResourceNotFoundException("Demo not found: " + id);
        }
        return demo;
    }
}
