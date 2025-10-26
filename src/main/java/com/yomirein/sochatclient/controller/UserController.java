package com.yomirein.sochatclient.controller;

import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.repo.UserRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepo repo;

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return repo.save(user);
    }
}