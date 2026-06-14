package com.yuvaraj.incidentdesk.config;

import com.yuvaraj.incidentdesk.domain.AuditLog;
import com.yuvaraj.incidentdesk.domain.Comment;
import com.yuvaraj.incidentdesk.domain.Incident;
import com.yuvaraj.incidentdesk.domain.IncidentType;
import com.yuvaraj.incidentdesk.domain.Priority;
import com.yuvaraj.incidentdesk.domain.Role;
import com.yuvaraj.incidentdesk.domain.Status;
import com.yuvaraj.incidentdesk.domain.User;
import com.yuvaraj.incidentdesk.repository.AuditLogRepository;
import com.yuvaraj.incidentdesk.repository.CommentRepository;
import com.yuvaraj.incidentdesk.repository.IncidentRepository;
import com.yuvaraj.incidentdesk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TEMP — rich demo dataset for recruiter-facing demos.
 *
 * Seeds the demo accounts plus several reporters and ~40 incidents across every type/priority/status,
 * backdated over the last month, with audit-log history and comment threads so the dashboard,
 * incident list, audit trail and comments all look populated. When {@code app.seed.reset} is true
 * (the default while demoing) it wipes and reseeds on every boot so the live demo self-heals.
 * Revert this commit to restore the minimal seed.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] INCIDENT_TITLES = {
            "Slip and fall near section 104", "Lost child reported at gate B", "Phone stolen at food court",
            "Altercation in parking lot C", "Medical assistance needed in the stands", "Unattended bag at entrance",
            "Crowd surge at gate A", "Vandalism in restroom 2",
    };
    private static final String[] REQUEST_TITLES = {
            "Request: extra security for VIP event", "Request: wheelchair assistance at gate D",
            "Request: additional wayfinding signage", "Request: lost & found pickup", "Request: vendor parking pass",
            "Request: press box access",
    };
    private static final String[] MAINTENANCE_TITLES = {
            "Main entrance turnstile jammed", "Fire exit sign flickering", "Broken AC unit 3",
            "Leaking pipe in the concourse", "Elevator 2 out of service", "Flickering lights in tunnel",
            "Escalator handrail loose", "Scoreboard pixel failure",
    };
    private static final String[] COMMENTS = {
            "On site, investigating now.", "Assigned to the facilities team.", "Awaiting parts, ETA tomorrow.",
            "Resolved — replaced the faulty unit.", "Coordinating with the event manager.",
            "Cordoned off the area as a precaution.", "Follow-up scheduled for next shift.",
    };

    private final UserRepository users;
    private final IncidentRepository incidents;
    private final AuditLogRepository auditLogs;
    private final CommentRepository comments;
    private final PasswordEncoder encoder;
    private final boolean reset;

    public DataSeeder(UserRepository users, IncidentRepository incidents, AuditLogRepository auditLogs,
                      CommentRepository comments, PasswordEncoder encoder,
                      @Value("${app.seed.reset:false}") boolean reset) {
        this.users = users;
        this.incidents = incidents;
        this.auditLogs = auditLogs;
        this.comments = comments;
        this.encoder = encoder;
        this.reset = reset;
    }

    @Override
    public void run(String... args) {
        // No @Transactional here: each save/deleteAll is its own tx so a seeding failure can never
        // leave a rollback-only transaction that fails the CommandLineRunner and crashes startup.
        try {
            seed();
        } catch (Exception e) {
            log.error("Demo seeding failed (app still starts): {}", e.getMessage(), e);
        }
    }

    private void seed() {
        if (reset) {
            log.info("SEED_RESET=true — clearing existing data before reseeding...");
            auditLogs.deleteAll();
            comments.deleteAll();
            incidents.deleteAll();
            users.deleteAll();
        } else if (users.count() > 0) {
            return;
        }
        log.info("Seeding demo data...");

        Random rnd = new Random(42);

        User admin = createUser("admin@incidentdesk.dev", "Admin123!", "Avery Admin", Role.ADMIN);
        User reporter = createUser("reporter@incidentdesk.dev", "Reporter123!", "Riley Reporter", Role.REPORTER);
        // Extra reporters so the admin's all-incidents view spans multiple people.
        List<User> reporters = new ArrayList<>();
        reporters.add(reporter);
        reporters.add(createUser("sam@incidentdesk.dev", "Reporter123!", "Sam Okafor", Role.REPORTER));
        reporters.add(createUser("jordan@incidentdesk.dev", "Reporter123!", "Jordan Diaz", Role.REPORTER));
        reporters.add(createUser("casey@incidentdesk.dev", "Reporter123!", "Casey Wong", Role.REPORTER));

        IncidentType[] typeOrder = IncidentType.values();
        Priority[] priorities = Priority.values();
        // Weighted status distribution: a healthy mix of active and closed work.
        Status[] statusPool = {
                Status.OPEN, Status.OPEN, Status.IN_PROGRESS, Status.IN_PROGRESS,
                Status.RESOLVED, Status.RESOLVED, Status.CLOSED, Status.CLOSED, Status.CLOSED,
        };

        int total = 40;
        for (int i = 0; i < total; i++) {
            IncidentType type = typeOrder[i % typeOrder.length];
            String title = titleFor(type, i);
            Priority priority = priorities[rnd.nextInt(priorities.length)];
            Status status = statusPool[rnd.nextInt(statusPool.length)];
            // The demo reporter (Riley) authors ~45% so their scoped list is full.
            User rep = rnd.nextInt(100) < 45 ? reporter : reporters.get(rnd.nextInt(reporters.size()));

            long createdDaysAgo = rnd.nextInt(30);
            Instant created = Instant.now().minus(createdDaysAgo, ChronoUnit.DAYS)
                    .minus(rnd.nextInt(8), ChronoUnit.HOURS);

            Incident inc = new Incident();
            inc.setTitle(title);
            inc.setType(type);
            inc.setPriority(priority);
            inc.setStatus(status);
            inc.setDescription("Reported via the operations console. " + title + ".");
            inc.setReporter(rep);
            inc.setCreatedAt(created);

            boolean worked = status != Status.OPEN;
            if (worked) {
                inc.setAssignee(admin);
            }
            if (status == Status.RESOLVED || status == Status.CLOSED) {
                inc.setResolvedAt(created.plus(2L + rnd.nextInt(40), ChronoUnit.HOURS));
            }
            incidents.save(inc);

            // Audit history for anything that moved beyond OPEN.
            if (worked) {
                auditLogs.save(new AuditLog(inc, admin, "assignee", null, admin.getFullName()));
                auditLogs.save(new AuditLog(inc, admin, "status", Status.OPEN.name(), Status.IN_PROGRESS.name()));
                if (status == Status.RESOLVED || status == Status.CLOSED) {
                    auditLogs.save(new AuditLog(inc, admin, "status", Status.IN_PROGRESS.name(), status.name()));
                }
            }
            // A comment thread on roughly half the incidents.
            if (rnd.nextBoolean()) {
                comments.save(new Comment(inc, admin, COMMENTS[rnd.nextInt(COMMENTS.length)], rnd.nextInt(100) < 30));
                if (rnd.nextInt(100) < 40) {
                    comments.save(new Comment(inc, rep, "Thanks — keeping an eye on this.", false));
                }
            }
        }

        log.info("Seed complete: {} users, {} incidents, {} audit logs, {} comments.",
                users.count(), incidents.count(), auditLogs.count(), comments.count());
        log.info("Demo accounts: admin@incidentdesk.dev / Admin123!  |  reporter@incidentdesk.dev / Reporter123!");
    }

    private String titleFor(IncidentType type, int i) {
        String[] pool = switch (type) {
            case INCIDENT -> INCIDENT_TITLES;
            case REQUEST -> REQUEST_TITLES;
            case MAINTENANCE -> MAINTENANCE_TITLES;
        };
        return pool[(i / IncidentType.values().length) % pool.length];
    }

    private User createUser(String email, String password, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        return users.save(user);
    }
}
