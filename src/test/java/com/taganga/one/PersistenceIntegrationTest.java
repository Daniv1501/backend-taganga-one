package com.taganga.one;

import com.taganga.one.domain.AssistantEntry;
import com.taganga.one.domain.AssistantEntryType;
import com.taganga.one.domain.GeoPoint;
import com.taganga.one.domain.VisitReason;
import com.taganga.one.domain.Visitor;
import com.taganga.one.domain.Zone;
import com.taganga.one.domain.ZoneGeometry;
import com.taganga.one.domain.ZoneType;
import com.taganga.one.dto.request.AskAssistantRequest;
import com.taganga.one.dto.request.CreateVisitorRequest;
import com.taganga.one.dto.request.VisitorLoginRequest;
import com.taganga.one.dto.response.VisitorResponse;
import com.taganga.one.dto.response.ZoneResponse;
import com.taganga.one.exception.BusinessRuleException;
import com.taganga.one.exception.InvalidCredentialsException;
import com.taganga.one.repository.AssistantEntryRepository;
import com.taganga.one.repository.DecalogueRuleRepository;
import com.taganga.one.repository.VisitorRepository;
import com.taganga.one.repository.ZoneRepository;
import com.taganga.one.assistant.TagangaKnowledge;
import com.taganga.one.dto.response.AssistantAnswerResponse;
import com.taganga.one.service.AssistantService;
import com.taganga.one.service.VisitorService;
import com.taganga.one.service.ZoneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de integración contra PostgreSQL real vía Testcontainers:
 * migraciones Flyway, restricciones del esquema y flujos completos de servicio.
 */
@Testcontainers
// Clave de Gemini vacía: las pruebas nunca llaman a Google aunque exista un .env local
@SpringBootTest(properties = "app.gemini.api-key=")
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("taganga_test")
                    .withUsername("taganga")
                    .withPassword("taganga");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private DecalogueRuleRepository decalogueRepository;

    @Autowired
    private AssistantEntryRepository assistantRepository;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private AssistantService assistantService;

    @Autowired
    private TagangaKnowledge knowledge;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PASSWORD = "clave-segura-1";

    // Hash de relleno con el largo de BCrypt: las pruebas de restricciones no lo verifican
    private static final String HASH = "$2a$04$" + "a".repeat(53);

    private CreateVisitorRequest request(String fullName, String email, String password) {
        return new CreateVisitorRequest(
                fullName, "Bogotá", 2, email, password,
                LocalDate.now().plusDays(2), VisitReason.TOURISM, true);
    }

    private Visitor visitor(String code, String email) {
        return new Visitor(code, "María Pérez", email, HASH,
                "Bogotá", 1, LocalDate.of(2026, 12, 1), VisitReason.TOURISM);
    }

    @Test
    void migrationsSeedAllPrototypeData() {
        assertThat(zoneRepository.count()).isEqualTo(24);
        assertThat(decalogueRepository.count()).isEqualTo(10);
        // V8: 6 preguntas rápidas sobre el mapa; las 5 del prototipo ya corregidas pasan a EXTRA
        assertThat(assistantRepository.findByTypeOrderByDisplayOrderAsc(AssistantEntryType.FAQ))
                .extracting(AssistantEntry::getCode)
                .containsExactly("zepat", "buceo", "snorkel", "recorridos", "embarcaciones", "historias-mapa");
        assertThat(assistantRepository.findByTypeOrderByDisplayOrderAsc(AssistantEntryType.EXTRA))
                .hasSize(12);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from zone_coordinates", Integer.class)).isEqualTo(233);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from assistant_keywords", Integer.class)).isEqualTo(115);
    }

    @Test
    void zonesKeepOrderAndCoordinateSequence() {
        List<Zone> zones = zoneRepository.findAllByOrderByDisplayOrderAsc();

        assertThat(zones).hasSize(24);
        assertThat(zones.get(0).getCode()).isEqualTo("ZEPAT_AGUJA");
        assertThat(zones.get(0).getType()).isEqualTo(ZoneType.ZEPAT);
        assertThat(zones.get(23).getCode()).isEqualTo("RUTA_MOTO");

        Zone route = zones.get(19);
        assertThat(route.getGeometry()).isEqualTo(ZoneGeometry.LINE);
        assertThat(route.getType()).isEqualTo(ZoneType.TOURIST_TRANSPORT);
        assertThat(route.getCoordinates()).hasSize(23);
        GeoPoint first = route.getCoordinates().get(0);
        assertThat(first.getLatitude()).isEqualTo(11.266020);
        assertThat(first.getLongitude()).isEqualTo(-74.191623);
    }

    @Test
    void zoneServiceMapsCoordinatesInsideTransaction() {
        ZoneResponse aguja = zoneService.findAll().get(0);

        assertThat(aguja.name()).isEqualTo("ZEPAT Isla Aguja");
        assertThat(aguja.geometry()).isEqualTo(ZoneGeometry.POLYGON);
        assertThat(aguja.coordinates()).hasSize(26);
    }

    @Test
    void registerPersistsVisitorAndConsentThenLoginFindsIt() {
        VisitorResponse registered = visitorService.register(
                request("María Pérez Gómez", "Maria@Example.com", PASSWORD));
        entityManager.flush();
        entityManager.clear();

        assertThat(registered.visitorCode()).matches("TG-\\d{4,}");

        Visitor stored = visitorRepository.findByVisitorCode(registered.visitorCode()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("maria@example.com");
        assertThat(stored.getPasswordHash()).startsWith("$2").hasSize(60);
        assertThat(stored.getConsents()).hasSize(1);
        assertThat(stored.getConsents().get(0).getLegalBasis())
                .isEqualTo("Ley 1581 de 2012 / Decreto 1377 de 2013");

        VisitorResponse loggedIn = visitorService.login(
                new VisitorLoginRequest("maria@example.com", PASSWORD));
        assertThat(loggedIn.visitorCode()).isEqualTo(registered.visitorCode());

        assertThrows(InvalidCredentialsException.class, () -> visitorService.login(
                new VisitorLoginRequest("maria@example.com", "incorrecta")));
    }

    @Test
    void registeringSameEmailAndPasswordReplacesRecordAndKeepsConsentHistory() {
        VisitorResponse first = visitorService.register(
                request("María Pérez Gómez", "ana@example.com", PASSWORD));
        entityManager.flush();

        VisitorResponse second = visitorService.register(
                request("María Pérez Actualizada", "ana@example.com", PASSWORD));
        entityManager.flush();
        entityManager.clear();

        assertThat(second.visitorCode()).isNotEqualTo(first.visitorCode());
        assertThat(visitorRepository.findByVisitorCode(first.visitorCode())).isEmpty();

        Visitor stored = visitorRepository.findByEmail("ana@example.com").orElseThrow();
        assertThat(stored.getFullName()).isEqualTo("María Pérez Actualizada");
        assertThat(stored.getConsents()).hasSize(2);
    }

    @Test
    void registeringSameEmailWithAnotherPasswordIsRejected() {
        visitorService.register(request("María Pérez Gómez", "luz@example.com", PASSWORD));
        entityManager.flush();

        assertThrows(BusinessRuleException.class, () -> visitorService.register(
                request("Otra Persona", "luz@example.com", "otra-clave-9")));
    }

    @Test
    void sequenceNeverRepeatsVisitorCodes() {
        long first = visitorRepository.nextVisitorCodeNumber();
        long second = visitorRepository.nextVisitorCodeNumber();

        assertThat(first).isGreaterThanOrEqualTo(1000);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        visitorRepository.saveAndFlush(visitor("TG-A", "a@example.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> visitorRepository.saveAndFlush(visitor("TG-B", "a@example.com")));
    }

    @Test
    void uppercaseEmailViolatesCheckConstraint() {
        assertThrows(DataIntegrityViolationException.class,
                () -> visitorRepository.saveAndFlush(visitor("TG-E", "Mayus@example.com")));
    }

    @Test
    void duplicateVisitorCodeViolatesUniqueConstraint() {
        visitorRepository.saveAndFlush(visitor("TG-C", "c1@example.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> visitorRepository.saveAndFlush(visitor("TG-C", "c2@example.com")));
    }

    @Test
    void groupSizeCheckConstraintIsEnforced() {
        assertThrows(DataIntegrityViolationException.class,
                () -> visitorRepository.saveAndFlush(new Visitor(
                        "TG-D", "María Pérez", "d@example.com", HASH,
                        "Bogotá", 51, LocalDate.of(2026, 12, 1), VisitReason.TOURISM)));
    }

    @Test
    void assistantAnswersUsingSeededKnowledge() {
        assertThat(assistantService.ask(new AskAssistantRequest("¿Puedo pasar en moto acuática por la bahía?", null)).answer())
                .contains("Moto acuática");
        assertThat(assistantService.ask(new AskAssistantRequest("¿Cómo llego a Playa Grande?", null)).answer())
                .contains("Hay dos formas");
        assertThat(assistantService.ask(new AskAssistantRequest("hola", "Ana")).answer())
                .contains("¡Hola, Ana!")
                .doesNotContain("{visitor}");
        assertThat(assistantService.ask(new AskAssistantRequest("¿Cuánto cuesta el peaje?", null)).answer())
                .contains("no cobra");
        assertThat(assistantService.ask(new AskAssistantRequest("xyz", null)).answer())
                .contains("Aún no tengo información precisa");
    }

    @Test
    void assistantFallsBackToKeywordsWithoutGeminiKey() {
        AssistantAnswerResponse response = assistantService.ask(new AskAssistantRequest("hola", null));

        assertThat(response.source()).isEqualTo(AssistantAnswerResponse.SOURCE_KEYWORDS);
    }

    @Test
    void faqsNoLongerDescribeThePrototypeMap() {
        assertThat(assistantService.findFaqs())
                .allSatisfy(faq -> assertThat(faq.answer())
                        .doesNotContain("dorado", "San Pedro", "rojo punteado", "libre tránsito"));
    }

    @Test
    void knowledgeIncludesStaticFileZonesAndDecalogue() {
        String instruction = knowledge.systemInstruction("Ana <script>");

        assertThat(instruction)
                .contains("Eres Tagui")
                .contains("Peibuni Tuxe")
                .contains("ZEPAT Isla Aguja")
                .contains("Recorrido de kayak")
                .contains("1. Regístrate")
                .contains("El visitante se llama Ana script.")
                .doesNotContain("<script>");
    }

    @Test
    void quickQuestionsAreAnsweredByTheirOwnKeywordsWithoutGemini() {
        assertThat(assistantService.findFaqs()).allSatisfy(faq ->
                assertThat(assistantService.ask(new AskAssistantRequest(faq.question(), null)).answer())
                        .as(faq.question())
                        .isEqualTo(faq.answer()));
    }
}
