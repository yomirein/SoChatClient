package com.yomirein.sochatclient.controller;

import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.repo.ChatRepo;
import com.yomirein.sochatclient.repo.MessageRepo;
import com.yomirein.sochatclient.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final ChatRepo chatRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{chatId}")
    public List<Message> getMessages(@PathVariable Long chatId) {
        return messageRepo.findByChatIdOrderByTimestampAsc(chatId);
    }

    @PostMapping
    public Message sendMessage(
            @RequestParam Long chatId,
            @RequestParam Long userId,
            @RequestParam String text
    ) {
        User user = userRepo.findById(userId).orElseThrow();
        Chat chat = chatRepo.findById(chatId).orElseThrow();

        Message msg = new Message();
        msg.setSender(user);
        msg.setChat(chat);
        msg.setContent(text);
        msg.setTimestamp(LocalDateTime.now());

        Message saved = messageRepo.save(msg);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, saved);

        return saved;
    }
}
