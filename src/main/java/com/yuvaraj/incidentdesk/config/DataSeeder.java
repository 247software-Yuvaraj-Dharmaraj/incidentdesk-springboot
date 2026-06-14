package com.yuvaraj.incidentdesk.config;

import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Role;
import com.yuvaraj.incidentdesk.domain.Status;
import com.yuvaraj.incidentdesk.domain.User;
import com.yuvaraj.incidentdesk.repository.IncidentRepository;
import com.yuvaraj.incidentdesk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Seeds demo accounts and sample incidents on first run (mirrors the original seed script). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final IncidentRepository incidents;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, IncidentRepository incidents, PasswordEncoder encoder) {
        this.users = users;
        this.incidents = incidents;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        log.info("Seeding demo data...");

        User admin = createUser("admin@incidentdesk.dev", "Admin123!", "Avery Admin", Role.ADMIN);
        User reporter = createUser("reporter@incidentdesk.dev", "Reporter123!", "Riley Reporter", Role.REPORTER);

        save("Main entrance turnstile jammed", IncidentType.MAINTENANCE, Priority.HIGH, Status.CLOSED, reporter, admin, true);
        save("Fire exit sign flickering", IncidentType.MAINTENANCE, Priority.CRITICAL, Status.CLOSED, reporter, admin, true);
        save("Broken AC unit 3", IncidentType.MAINTENANCE, Priority.HIGH, Status.CLOSED, reporter, admin, true);
        save("Spilled drink near section 104", IncidentType.INCIDENT, Priority.LOW, Status.CLOSED, reporter, admin, true);
        save("Lost child reported at gate B", IncidentType.INCIDENT, Priority.LOW, Status.CLOSED, reporter, admin, true);
        save("Request: extra security for VIP event", IncidentType.REQUEST, Priority.MEDIUM, Status.CLOSED, reporter, admin, true);
        save("Phone lost near food court", IncidentType.INCIDENT, Priority.MEDIUM, Status.IN_PROGRESS, reporter, admin, false);
        save("Sample incident", IncidentType.INCIDENT, Priority.MEDIUM, Status.OPEN, reporter, null, false);

        log.info("Seed complete: {} users, {} incidents", users.count(), incidents.count());
    }

    private User createUser(String email, String password, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        return users.save(user);
    }

    private void save(String title, IncidentType type, Priority priority, Status status,
                      User reporter, User assignee, boolean resolved) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setType(type);
        incident.setPriority(priority);
        incident.setStatus(status);
        incident.setReporter(reporter);
        incident.setAssignee(assignee);
        if (resolved) {
            incident.setResolvedAt(Instant.now());
        }
        incidents.save(incident);
    }
}
