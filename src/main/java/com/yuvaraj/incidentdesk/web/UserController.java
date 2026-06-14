package com.yuvaraj.incidentdesk.web;

import com.yuvaraj.incidentdesk.dto.AuthDtos.UserListItem;
import com.yuvaraj.incidentdesk.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<UserListItem> items = users.findAllByOrderByFullNameAsc().stream()
                .map(UserListItem::from)
                .toList();
        return Map.of("users", items);
    }
}
