package com.jairomatias.eventix.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventStatus;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

/**
 * Visible, entirely fictional catalog for the public free Render demo.
 *
 * Only active with the render Spring profile and ONLY an in-memory H2 URL.
 * Never seeds the SQL Server/Flyway deployment, admin accounts, transactions,
 * actual customer details or reusable passwords.
 */
@Component
@Profile("render")
public class RenderDemoCatalogInitializer implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RenderDemoCatalogInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String dataSourceUrl;

    public RenderDemoCatalogInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            EventCategoryRepository categoryRepository,
            EventRepository eventRepository,
            TicketTypeRepository ticketTypeRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${spring.datasource.url:}") String dataSourceUrl) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.dataSourceUrl = dataSourceUrl;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!dataSourceUrl.startsWith("jdbc:h2:mem:")) {
            throw new IllegalStateException(
                    "El catálogo de demostración solo puede usarse con H2 en memoria.");
        }
        if (eventRepository.count() > 0) {
            LOGGER.info("La demo ya tiene eventos; no se crearán registros adicionales.");
            return;
        }

        Role organizerRole = null;
        for (RoleName roleName : RoleName.values()) {
            Optional<Role> found = roleRepository.findByName(roleName);
            Role role = found.orElseGet(() -> roleRepository.save(
                    new Role(roleName, "Rol de prueba (perfil Render).")));
            if (roleName == RoleName.ORGANIZER) {
                organizerRole = role;
            }
        }

        // No known credentials. This account is disabled and only backs the
        // required organizer relation on public sample events.
        User organizer = new User(
                "Equipo", "Demostración Eventix",
                "organizador-demo@eventix.local",
                "eventix_demo",
                passwordEncoder.encode(UUID.randomUUID().toString()),
                null,
                organizerRole);
        organizer.setStatus(UserStatus.INACTIVE);
        organizer.setMustChangePassword(true);
        organizer = userRepository.save(organizer);

        LocalDateTime firstStart = LocalDateTime.now(clock)
                .plusDays(18).withHour(18).withMinute(30)
                .withSecond(0).withNano(0);

        seedEvent("Tecnología", "Experiencias ficticias de tecnología.",
                "Eventix Tech Experience — DEMO",
                "Conferencia ficticia para enseñar el catálogo, las entradas y el recorrido de Eventix. "
                        + "No es un evento real; las compras son simuladas.",
                firstStart, "Auditorio Eventix (ficticio)",
                organizer, new BigDecimal("599.00"), 120,
                TicketTypeCategory.GENERAL, "Entrada general de demostración");

        seedEvent("Música", "Conciertos y experiencias musicales ficticias.",
                "Noche de Música — DEMO",
                "Concierto ficticio creado exclusivamente para la demostración de Eventix. "
                        + "No existe una presentación ni un recinto contratado.",
                firstStart.plusDays(8).withHour(20), "Escenario Eventix (ficticio)",
                organizer, new BigDecimal("750.00"), 250,
                TicketTypeCategory.GENERAL, "Acceso general de demostración");

        seedEvent("Cultura", "Actividades culturales ficticias.",
                "Encuentro Creativo — DEMO",
                "Actividad ficticia y gratuita para recorrer la experiencia de eventos de Eventix. "
                        + "La información y las boletas no son válidas fuera de la demo.",
                firstStart.plusDays(15).withHour(17), "Sala Creativa Eventix (ficticia)",
                organizer, BigDecimal.ZERO, 80,
                TicketTypeCategory.COMPLIMENTARY, "Entrada gratuita de demostración");

        LOGGER.info("Catálogo público Render listo: tres eventos ficticios y sin cobros reales.");
    }

    private void seedEvent(
            String categoryName,
            String categoryDescription,
            String title,
            String description,
            LocalDateTime startAt,
            String venue,
            User organizer,
            BigDecimal price,
            int capacity,
            TicketTypeCategory ticketCategory,
            String ticketName) {
        EventCategory category = categoryRepository.save(
                new EventCategory(categoryName, categoryDescription));

        Event event = eventRepository.save(new Event(
                title, description, category, EventStatus.PUBLISHED,
                startAt, startAt.plusHours(3),
                venue, "Recinto ficticio de demostración, República Dominicana",
                capacity, organizer, null,
                price.signum() == 0, price));

        ticketTypeRepository.save(new TicketType(
                event, ticketCategory, ticketName, price, capacity));
    }
}
