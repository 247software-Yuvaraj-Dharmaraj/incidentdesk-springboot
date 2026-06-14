package com.yuvaraj.incidentdesk.security;

import com.yuvaraj.incidentdesk.domain.Role;

/** Authenticated principal derived from the JWT (mirrors the Node req.user). */
public record AppUser(String id, Role role) {
}
