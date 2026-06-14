package com.yuvaraj.incidentdesk.realtime;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.yuvaraj.incidentdesk.security.CookieUtil;
import com.yuvaraj.incidentdesk.security.JwtService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Socket.IO server (netty-socketio) that mirrors the Node backend's realtime layer:
 * authenticates the handshake via the JWT cookie and broadcasts "incidents:changed"
 * so connected clients refetch (RBAC-scoped) when incident data changes.
 */
@Component
public class RealtimeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeService.class);

    private final boolean enabled;
    private final int port;
    private final String clientUrl;
    private final JwtService jwtService;

    private SocketIOServer server;

    public RealtimeService(@Value("${app.socket.enabled}") boolean enabled,
                           @Value("${app.socket.port}") int port,
                           @Value("${app.client-url}") String clientUrl,
                           JwtService jwtService) {
        this.enabled = enabled;
        this.port = port;
        this.clientUrl = clientUrl;
        this.jwtService = jwtService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void start() {
        if (!enabled || server != null) {
            return;
        }
        Configuration config = new Configuration();
        config.setPort(port);
        config.setOrigin(clientUrl);
        config.setAuthorizationListener(data -> {
            try {
                String cookieHeader = data.getHttpHeaders().get("Cookie");
                String token = readCookie(cookieHeader);
                if (token == null) {
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }
                jwtService.parse(token);
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            } catch (Exception e) {
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        try {
            server = new SocketIOServer(config);
            server.start();
            log.info("Socket.IO server started on port {}", port);
        } catch (Exception e) {
            log.warn("Socket.IO server failed to start: {}", e.getMessage());
            server = null;
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /** Notifies all connected clients that incident data changed. */
    public void emitIncidentsChanged() {
        if (server != null) {
            server.getBroadcastOperations().sendEvent("incidents:changed");
        }
    }

    private static String readCookie(String cookieHeader, String name) {
        if (cookieHeader == null) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(name + "=")) {
                return trimmed.substring(name.length() + 1);
            }
        }
        return null;
    }

    private static String readCookie(String cookieHeader) {
        return readCookie(cookieHeader, CookieUtil.AUTH_COOKIE);
    }
}
