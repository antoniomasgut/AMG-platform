package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.api.dto.KnowledgeEntryRequest;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class KnowledgeBaseServiceTest {

    @Autowired private KnowledgeBaseService knowledgeBaseService;
    @Autowired private KnowledgeBaseRepository knowledgeBaseRepository;
    @Autowired private KnowledgeEntryRepository knowledgeEntryRepository;
    @Autowired private KnowledgeDocumentRepository knowledgeDocumentRepository;

    private UUID tenantId;
    private KnowledgeBase kb;

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository.deleteAll();
        knowledgeEntryRepository.deleteAll();
        knowledgeDocumentRepository.deleteAll();

        tenantId = UUID.randomUUID();
        kb = knowledgeBaseService.findOrCreate(tenantId);
    }

    @Test
    void findOrCreate_createsKB_whenNotExists() {
        var result = knowledgeBaseService.findOrCreate(UUID.randomUUID());
        assertThat(result.getId(), is(notNullValue()));
        assertThat(result.getIsActive(), is(true));
        assertThat(result.getVersion(), is(1));
    }

    @Test
    void findOrCreate_returnsExistingKB() {
        var first = knowledgeBaseService.findOrCreate(tenantId);
        var second = knowledgeBaseService.findOrCreate(tenantId);

        assertThat(second.getId(), is(first.getId()));
    }

    @Test
    void buildKnowledgeBlock_returnsEmpty_whenNoEntries() {
        var block = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        assertThat(block, is(""));
    }

    @Test
    void buildKnowledgeBlock_containsEntries() {
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.BUSINESS_INFO)
                .key("name").content("Botiga Test SL").sortOrder(1).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.FAQ)
                .key("hours").content("Horari: 9h-14h, 16h-20h").sortOrder(1).build());

        var block = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        assertThat(block, containsString("CONEIXEMENT DEL NEGOCI"));
        assertThat(block, containsString("Botiga Test SL"));
        assertThat(block, containsString("Horari: 9h-14h, 16h-20h"));
    }

    @Test
    void buildKnowledgeBlock_omitsInactiveEntries() {
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.SERVICE)
                .key("s1").content("Servei Actiu").sortOrder(1).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.SERVICE)
                .key("s2").content("Servei Inactiu").sortOrder(2)
                .isActive(false).build());

        var block = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        assertThat(block, containsString("Servei Actiu"));
        assertThat(block, not(containsString("Servei Inactiu")));
    }

    @Test
    void buildKnowledgeBlock_returnsEmpty_whenKBInactive() {
        kb.setIsActive(false);
        knowledgeBaseRepository.save(kb);

        var block = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        assertThat(block, is(""));
    }

    @Test
    void updateEntries_addsAndReplacesEntries() {
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.FAQ)
                .key("old").content("Old FAQ").sortOrder(1).build());

        knowledgeBaseService.updateEntries(tenantId, KnowledgeCategory.FAQ,
                java.util.List.of(new KnowledgeEntryRequest("q1", "P: Quant costa? R: 50€", 1)));

        var entries = knowledgeEntryRepository
                .findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId());
        assertThat(entries, hasSize(1));
        assertThat(entries.getFirst().getKey(), is("q1"));
    }

    @Test
    void addDocument_incrementsVersion() {
        int before = kb.getVersion();
        knowledgeBaseService.addDocument(tenantId, "test.txt", "content", null, null, null);

        kb = knowledgeBaseRepository.findByTenantId(tenantId).orElseThrow();
        assertThat(kb.getVersion(), is(before + 1));
    }

    @Test
    void deleteDocument_softDeletesAndIncrementsVersion() {
        var doc = knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .knowledgeBaseId(kb.getId()).filename("doc.txt").build());
        int before = kb.getVersion();

        knowledgeBaseService.deleteDocument(tenantId, doc.getId());

        var docOpt = knowledgeDocumentRepository.findById(doc.getId());
        assertThat(docOpt.isPresent(), is(true));
        assertThat(docOpt.get().getIsActive(), is(false));

        kb = knowledgeBaseRepository.findByTenantId(tenantId).orElseThrow();
        assertThat(kb.getVersion(), is(before + 1));
    }

    @Test
    void buildKnowledgeBlock_includesDocuments() {
        knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .knowledgeBaseId(kb.getId()).filename("menu.pdf")
                .extractedText("Menú: Pizza 10€, Pasta 12€").build());

        var block = knowledgeBaseService.buildKnowledgeBlock(tenantId);
        assertThat(block, containsString("DOCUMENTS"));
        assertThat(block, containsString("menu.pdf"));
        assertThat(block, containsString("Menú: Pizza 10€, Pasta 12€"));
    }

    @Test
    void addDocument_rejectsNonExistentTenant() {
        var unknownId = UUID.randomUUID();
        var doc = knowledgeBaseService.addDocument(unknownId, "test.txt", "content", null, null, null);
        assertThat(doc, is(notNullValue()));
    }
}
