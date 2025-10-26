package com.yomirein.sochatclient.controller;

import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.repo.ChatRepo;
import com.yomirein.sochatclient.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatRepo chatRepo;
    private final UserRepo userRepo;

    @GetMapping
    public List<Chat> all() {
        return chatRepo.findAll();
    }

    // 🔹 Новый метод — возвращает или создаёт чат между 2 пользователями
    @PostMapping("/between")
    public Chat getOrCreateChat(@RequestParam Long user1, @RequestParam Long user2) {
        List<Chat> all = chatRepo.findAll();

        for (Chat c : all) {
            if (c.getParticipants().size() == 2 &&
                    c.getParticipants().stream().anyMatch(u -> u.getId().equals(user1)) &&
                    c.getParticipants().stream().anyMatch(u -> u.getId().equals(user2))) {
                return c;
            }
        }

        User u1 = userRepo.findById(user1).orElseThrow();
        User u2 = userRepo.findById(user2).orElseThrow();

        Chat chat = new Chat();
        chat.setTitle(u1.getUsername() + " & " + u2.getUsername());
        chat.setParticipants(Set.of(u1, u2));

        return chatRepo.save(chat);
    }
}