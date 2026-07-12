package com.amg.digitalitzacio.hosting.application;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.hosting.api.dto.WebSiteResponse;
import com.amg.digitalitzacio.hosting.domain.WebSite;
import com.amg.digitalitzacio.hosting.domain.WebSiteRepository;
import com.amg.digitalitzacio.hosting.domain.WebsiteStatus;
import com.amg.digitalitzacio.hosting.domain.WebsiteType;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebHostingService {

    private static final long MAX_ZIP_BYTES = 50 * 1024 * 1024L; // 50 MB
    private static final List<String> FORBIDDEN_EXTENSIONS = List.of(".php", ".htaccess", ".env", ".sh", ".py", ".rb", ".pl");

    private final WebSiteRepository webSiteRepository;
    private final TenantRepository tenantRepository;
    private final HostingManifestService manifestService;
    private final EmailService emailService;

    @Value("${app.hosting.data-path:/data/websites}")
    private String dataPath;

    @Value("${app.api.base-url:https://api.amgdl.com}")
    private String apiBaseUrl;

    public WebSiteResponse requestContainerSite(UUID tenantId, MultipartFile compose,
                                                  MultipartFile envExample, String domain,
                                                  String description, String upstreamContainer,
                                                  Integer upstreamPort, UserPrincipal principal) {
        verifyTenantIdAccess(tenantId, principal);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        Path composeDir = Path.of(dataPath, tenantId.toString(), "compose");
        try {
            Files.createDirectories(composeDir);
            Files.copy(compose.getInputStream(), composeDir.resolve("docker-compose.yml"), StandardCopyOption.REPLACE_EXISTING);
            if (envExample != null && !envExample.isEmpty()) {
                Files.copy(envExample.getInputStream(), composeDir.resolve(".env.example"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error desant el compose: " + e.getMessage(), e);
        }

        WebSite site = new WebSite();
        site.setTenantId(tenantId);
        site.setType(WebsiteType.CONTAINER);
        site.setStatus(WebsiteStatus.PENDING_REVIEW);
        site.setDomain(domain);
        site.setContainerName("site-" + tenant.getSlug() + "-pro");
        site.setUpstreamContainer(upstreamContainer);
        site.setUpstreamPort(upstreamPort != null ? upstreamPort : 80);
        site.setClientNotes(description);
        site.setStorageBytes(compose.getSize());

        return toResponse(webSiteRepository.save(site));
    }

    public Resource exportCompose(UUID siteId, UserPrincipal principal) {
        WebSite site = findSite(siteId);
        if (!"SUPER_ADMIN".equals(principal.role())) {
            verifyTenantAccess(site, principal);
        }
        Path composeFile = Path.of(dataPath, site.getTenantId().toString(), "compose", "docker-compose.yml");
        if (!Files.exists(composeFile)) {
            throw new ResourceNotFoundException("Compose file no trobat per site: " + siteId);
        }
        return new FileSystemResource(composeFile);
    }

    public WebSiteResponse requestStaticSite(UUID tenantId, MultipartFile zip, String domain, UserPrincipal principal) {
        verifyTenantIdAccess(tenantId, principal);
        if (zip.getSize() > MAX_ZIP_BYTES) {
            throw new IllegalArgumentException("El ZIP supera el límit de 50 MB");
        }
        validateZip(zip);

        Path tenantDir = Path.of(dataPath, tenantId.toString());
        Path htmlDir = tenantDir.resolve("html");
        Path backupZip = tenantDir.resolve("backup.zip");

        try {
            Files.createDirectories(htmlDir);
            Files.copy(zip.getInputStream(), backupZip, StandardCopyOption.REPLACE_EXISTING);
            extractZip(zip.getInputStream(), htmlDir);
        } catch (IOException e) {
            throw new RuntimeException("Error desant el ZIP: " + e.getMessage(), e);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        WebSite site = new WebSite();
        site.setTenantId(tenantId);
        site.setType(WebsiteType.STATIC);
        site.setStatus(WebsiteStatus.PENDING_REVIEW);
        site.setDomain(domain);
        site.setContainerName("site-" + tenant.getSlug() + "-static");
        site.setStorageBytes(zip.getSize());

        return toResponse(webSiteRepository.save(site));
    }

    public WebSiteResponse approveSite(UUID siteId, String notes, String upstreamContainer,
                                        Integer upstreamPort, UserPrincipal principal) {
        WebSite site = findSite(siteId);
        site.setStatus(WebsiteStatus.APPROVED);
        site.setReviewNotes(notes);
        site.setReviewedBy(principal.id());
        site.setReviewedAt(Instant.now());
        if (upstreamContainer != null && !upstreamContainer.isBlank()) {
            site.setUpstreamContainer(upstreamContainer);
        }
        if (upstreamPort != null) {
            site.setUpstreamPort(upstreamPort);
        }
        webSiteRepository.save(site);

        deploySite(site);
        return toResponse(site);
    }

    public WebSiteResponse rejectSite(UUID siteId, String notes, UserPrincipal principal) {
        WebSite site = findSite(siteId);
        site.setStatus(WebsiteStatus.REJECTED);
        site.setReviewNotes(notes);
        site.setReviewedBy(principal.id());
        site.setReviewedAt(Instant.now());
        return toResponse(webSiteRepository.save(site));
    }

    public WebSiteResponse updateStaticSite(UUID siteId, MultipartFile zip, UserPrincipal principal) {
        if (zip.getSize() > MAX_ZIP_BYTES) {
            throw new IllegalArgumentException("El ZIP supera el límit de 50 MB");
        }
        validateZip(zip);

        WebSite site = findSite(siteId);
        verifyTenantAccess(site, principal);

        Path htmlDir = Path.of(dataPath, site.getTenantId().toString(), "html");
        Path backupZip = Path.of(dataPath, site.getTenantId().toString(), "backup.zip");

        try {
            // Neteja el directori anterior i extreu el nou ZIP
            deleteDirectoryContents(htmlDir);
            Files.copy(zip.getInputStream(), backupZip, StandardCopyOption.REPLACE_EXISTING);
            extractZip(zip.getInputStream(), htmlDir);
        } catch (IOException e) {
            throw new RuntimeException("Error actualitzant el ZIP: " + e.getMessage(), e);
        }

        site.setStorageBytes(zip.getSize());
        // Nginx serveix la nova versió immediatament (volum muntat)
        log.info("[Hosting] Web actualitzada per tenant {}, site {}", site.getTenantId(), siteId);
        return toResponse(webSiteRepository.save(site));
    }

    public Resource exportZip(UUID siteId, UserPrincipal principal) {
        WebSite site = findSite(siteId);
        verifyTenantAccess(site, principal);
        Path backupZip = Path.of(dataPath, site.getTenantId().toString(), "backup.zip");
        if (!Files.exists(backupZip)) {
            throw new ResourceNotFoundException("Backup ZIP no trobat per site: " + siteId);
        }
        return new FileSystemResource(backupZip);
    }

    public void deleteSite(UUID siteId, UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role())) {
            throw new IllegalArgumentException("Only SUPER_ADMIN can delete sites");
        }
        WebSite site = findSite(siteId);

        // Els sites CONTAINER tenen un proxy gestionat per l'agent del host: en demanem
        // l'eliminació via manifest. Els STATIC no tenen contenidor (els serveix el backend);
        // en esborrar la fila, el backend deixa de servir-los.
        if (site.getType() == WebsiteType.CONTAINER) {
            manifestService.requestRemoval(site.getContainerName() + "-proxy");
        } else if (site.getType() == WebsiteType.STATIC) {
            manifestService.removeSiteRoute(site.getDomain());
        }

        webSiteRepository.delete(site);
        log.info("[Hosting] Site {} eliminat per {}", siteId, principal.email());
    }

    public List<WebSiteResponse> listSitesByTenant(UUID tenantId) {
        return webSiteRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    public WebSiteResponse getSite(UUID siteId, UserPrincipal principal) {
        WebSite site = findSite(siteId);
        verifyTenantAccess(site, principal);
        return toResponse(site);
    }

    public List<WebSiteResponse> listPendingSites() {
        return webSiteRepository.findByStatus(WebsiteStatus.PENDING_REVIEW)
                .stream().map(this::toResponse).toList();
    }

    public List<WebSiteResponse> listAllSites() {
        return webSiteRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    // --- Privats ---

    private void deploySite(WebSite site) {
        site.setStatus(WebsiteStatus.DEPLOYING);
        webSiteRepository.save(site);

        try {
            if (site.getType() == WebsiteType.CONTAINER) {
                deployContainerSite(site);
            } else {
                deployStaticSite(site);
            }
            site.setStatus(WebsiteStatus.ACTIVE);
            site.setDeployedAt(Instant.now());
        } catch (Exception e) {
            site.setStatus(WebsiteStatus.APPROVED);
            log.error("[Hosting] Error desplegant site {}: {}", site.getId(), e.getMessage());
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        } finally {
            webSiteRepository.save(site);
        }
    }

    private void deployStaticSite(WebSite site) {
        // Els sites estàtics NO necessiten contenidor: els serveix el backend
        // (PublicSiteController) directament des del volum, resolent pel Host.
        Path htmlDir = Path.of(dataPath, site.getTenantId().toString(), "html");
        injectWidgetScript(htmlDir, site.getId());
        // Ruta a coolify-proxy amb prefix /api/v1/sites/serve (la genera l'agent del host)
        manifestService.requestSiteRoute(site.getDomain(), "static");
    }

    private void deployContainerSite(WebSite site) {
        String upstream = site.getUpstreamContainer();
        int port = site.getUpstreamPort() != null ? site.getUpstreamPort() : 80;
        if (upstream == null || upstream.isBlank()) {
            throw new IllegalArgumentException(
                "upstreamContainer no definit per site " + site.getId() + " — no es pot crear el proxy");
        }
        String proxyName = site.getContainerName() + "-proxy";
        String tenant = site.getTenantId().toString();
        String confSubPath = tenant + "/proxy/nginx-proxy.conf";
        String confAbsolute = Path.of(dataPath, confSubPath).toString();
        String widgetUrl = apiBaseUrl + "/api/v1/widget/" + site.getId() + "/loader";
        // El backend només escriu el manifest; l'agent del host crea el contenidor.
        manifestService.requestContainerProxy(proxyName, site.getDomain(), upstream, port,
                widgetUrl, confSubPath, confAbsolute);
        log.info("[Hosting] Manifest de proxy demanat per site {} → {}:{}", site.getId(), upstream, port);
    }

    private void injectWidgetScript(Path htmlDir, java.util.UUID siteId) {
        String scriptTag = "\n<script src=\"" + apiBaseUrl + "/api/v1/widget/" + siteId + "/loader\" defer></script>";
        try (var stream = Files.walk(htmlDir)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".html")).forEach(htmlFile -> {
                try {
                    String content = Files.readString(htmlFile, StandardCharsets.UTF_8);
                    if (content.contains(scriptTag)) return;
                    String injected = content.replaceAll("(?i)</body>", scriptTag + "\n</body>");
                    if (!injected.equals(content)) {
                        Files.writeString(htmlFile, injected, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    log.warn("[Hosting] Could not inject widget script into {}: {}", htmlFile, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("[Hosting] Could not walk html dir for widget injection: {}", e.getMessage());
        }
    }

    private void validateZip(MultipartFile zip) {
        boolean hasIndexHtml = false;
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if ("index.html".equals(name)) hasIndexHtml = true;
                for (String forbidden : FORBIDDEN_EXTENSIONS) {
                    if (name.endsWith(forbidden)) {
                        throw new IllegalArgumentException("Fitxer no permès al ZIP: " + entry.getName());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error llegint el ZIP: " + e.getMessage());
        }
        if (!hasIndexHtml) {
            throw new IllegalArgumentException("El ZIP ha de contenir index.html a l'arrel");
        }
    }

    private void extractZip(InputStream inputStream, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                // Protecció zip-slip
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("Entrada ZIP fora de l'arrel: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectoryContents(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(dir))
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        }
    }

    private WebSite findSite(UUID id) {
        return webSiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
    }

    private void verifyTenantAccess(WebSite site, UserPrincipal principal) {
        if ("SUPER_ADMIN".equals(principal.role())) return;
        if (!site.getTenantId().equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Site not found: " + site.getId());
        }
    }

    private void verifyTenantIdAccess(UUID tenantId, UserPrincipal principal) {
        if ("SUPER_ADMIN".equals(principal.role())) return;
        if (!tenantId.equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Tenant not found: " + tenantId);
        }
    }

    public WebSiteResponse requestExternalSite(UUID tenantId, String domain,
                                                String contactEmail, String contactRedirectUrl,
                                                UserPrincipal principal) {
        verifyTenantIdAccess(tenantId, principal);

        if (contactEmail == null || contactEmail.isBlank()) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
            contactEmail = tenant.getEmail();
        }

        WebSite site = new WebSite();
        site.setTenantId(tenantId);
        site.setType(WebsiteType.EXTERNAL);
        site.setStatus(WebsiteStatus.ACTIVE);
        site.setDomain(domain);
        site.setContactEmail(contactEmail);
        site.setContactRedirectUrl(contactRedirectUrl);
        site.setDeployedAt(Instant.now());

        return toResponse(webSiteRepository.save(site));
    }

    public void sendSnippetsEmail(UUID siteId) {
        WebSite site = webSiteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + siteId));
        Tenant tenant = tenantRepository.findById(site.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String widgetSnippet = "<script src=\"" + apiBaseUrl + "/api/v1/widget/" + siteId + "/loader\" defer></script>";
        String formSnippet = """
                <form action="%s/api/v1/widget/%s/contact" method="POST">
                  <input type="text"  name="name"    placeholder="Nom"      required />
                  <input type="email" name="email"   placeholder="Email"    required />
                  <input type="tel"   name="phone"   placeholder="Telèfon"           />
                  <textarea           name="message" placeholder="Missatge" required></textarea>
                  <button type="submit">Enviar</button>
                </form>""".formatted(apiBaseUrl, siteId);

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#1a1a1a">
                  <h2 style="color:#FF6B00">Integra el xat i els formularis a la teva web</h2>
                  <p>Hola, et fem arribar els codis d'integració per afegir el xat IA, el botó de WhatsApp i el formulari de contacte a la teva web.</p>

                  <h3>1. Widget de xat IA + botó WhatsApp</h3>
                  <p>Afegeix aquest codi just abans de <code>&lt;/body&gt;</code> a totes les pàgines on vulguis mostrar el widget:</p>
                  <pre style="background:#f5f5f5;padding:16px;border-radius:4px;font-size:13px;overflow-x:auto">%s</pre>

                  <h3>2. Formulari de contacte</h3>
                  <p>Afegeix aquest formulari on vulguis. Quan un visitant l'envia, el missatge arriba directament al bot conversacional. Pots personalitzar l'HTML i el CSS lliurement, però no modifiquis l'<code>action</code> ni els atributs <code>name</code>:</p>
                  <pre style="background:#f5f5f5;padding:16px;border-radius:4px;font-size:12px;overflow-x:auto">%s</pre>

                  <h3>Notes importants</h3>
                  <ul>
                    <li>El widget de xat apareixerà automàticament quan l'agent IA estigui activat.</li>
                    <li>El botó de WhatsApp apareixerà automàticament si tens el número configurat.</li>
                    <li>No cal modificar el teu codi de servidor — tot és JavaScript i HTML estàtic.</li>
                  </ul>

                  <p style="color:#888;font-size:12px;margin-top:32px">AMG Digitalització · <a href="https://amgdl.com" style="color:#FF6B00">amgdl.com</a></p>
                </div>
                """.formatted(widgetSnippet, formSnippet);

        String subject = "Integra el xat i els formularis a la teva web — " + (tenant.getName() != null ? tenant.getName() : tenant.getEmail());
        emailService.sendEmail(tenant.getEmail(), subject, html);
        log.info("Snippets email sent to {} for site {}", tenant.getEmail(), siteId);
    }

    private WebSiteResponse toResponse(WebSite site) {
        return new WebSiteResponse(
                site.getId(), site.getTenantId(), site.getType(), site.getStatus(),
                site.getDomain(), site.getContainerName(),
                site.getUpstreamContainer(), site.getUpstreamPort(),
                site.getStorageBytes(), site.getContactEmail(), site.getContactRedirectUrl(),
                site.getClientNotes(), site.getReviewNotes(),
                site.getReviewedAt(), site.getDeployedAt(), site.getCreatedAt()
        );
    }
}
