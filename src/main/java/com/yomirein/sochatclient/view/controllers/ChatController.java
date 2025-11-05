package com.yomirein.sochatclient.view.controllers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.ChatView;
import com.vaadin.flow.component.html.Div;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompSession;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatController {

    private String token;

    @Getter
    @Setter
    private final Map<Long, StompSession.Subscription> subscriptions = new HashMap<>();

    @Getter
    @Setter
    private Long selectedChat = null;

    private final ExecutorService sendExecutor = Executors.newFixedThreadPool(2);


    public ChatController(String token) {
        this.token = token;
    }

    public List<Chat> getAllChats(ChatService chatService, Long id) {
        List<Chat> chatList1 = chatService.getChats(id, token);
        return chatList1;
    }

    public Chat createChat(ChatService chatService, Long userId) {
        Chat chat = chatService.createChat(userId, token);
        System.out.println(chat.toString() + " created");
        return chat;
    }


    public List<MessageListItem> getChatMessages(ChatService chatService, Long chatId, MessageList messageList) {
        List<Message> messages = chatService.getMessages(chatId, token);
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        for (Message message : messages){
            User user = chatService.getUser(message.getSenderId(), token);
            ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(message.getTimestamp());
            items.add(new MessageListItem(message.getContent(), message.getTimestamp().toInstant(zoneOffset), user.getUsername()));
        }
        return items;
    }

    public List<MessageListItem> sendMessage(WebSocketClient webSocketClient, Long chatId, MessageList messageList, MessageInput messageInput) {
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        messageInput.addSubmitListener(submitEvent -> {
            webSocketClient.sendMessage(chatId, submitEvent.getValue(), token);
            System.out.println("масаге отправляется");
            MessageListItem newMessage = new MessageListItem(
                    submitEvent.getValue(), Instant.now(), "username");
            newMessage.setUserColorIndex(3);
            items.add(newMessage);
        });
        return items;
    }

    public void openChat(Long chatId, ChatService chatService,
                         WebSocketClient webSocketClient,
                         MessageList messageList,
                         UI ui,
                         String token) {

        selectedChat = chatId;

        // Подписка на чат
        StompSession.Subscription subscription = webSocketClient.subscribeToChat(chatId, token, msg -> {
            System.out.println("[LOG] WebSocket message received: " + msg.getContent());

            CompletableFuture.supplyAsync(() -> chatService.getUser(msg.getSenderId(), token))
                    .thenAccept(userSender -> {
                        ui.access(() -> {
                            List<MessageListItem> items = new ArrayList<>(messageList.getItems());
                            items.add(new MessageListItem(
                                    msg.getContent(),
                                    Instant.now(),
                                    userSender.getUsername()
                            ));
                            messageList.setItems(items);
                            System.out.println("[LOG] messageList updated, total messages: " + items.size());
                        });
                    })
                    .exceptionally(ex -> { ex.printStackTrace(); return null; });
        });

        subscriptions.put(chatId, subscription);
        System.out.println("[LOG] Subscribed to chat " + chatId);
    }

    // Настройка отправки сообщений
    public void setupMessageSending(WebSocketClient webSocketClient,
                                    MessageList messageList,
                                    MessageInput messageInput,
                                    UI ui,
                                    String token,
                                    String username) {

        ui.access(() -> messageInput.addSubmitListener(submitEvent -> {
            String content = submitEvent.getValue();
            if (content == null || content.isBlank() || selectedChat == null) return;

            // Асинхронная отправка через Executor
            sendExecutor.submit(() -> {
                try {
                    System.out.println("[LOG] Sending message asynchronously: " + content);
                    webSocketClient.sendMessage(selectedChat, content, token);
                    System.out.println("[LOG] Message sent: " + content);
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { System.out.println("[LOG] sendExecutor task finished for message: " + content); }
            });

            // ❌ Не добавляем локальное сообщение в UI — оно придёт через WebSocket
        }));
    }

    // Отписка от всех чатов
    public void unsubscribeAll() {
        subscriptions.values().forEach(StompSession.Subscription::unsubscribe);
        subscriptions.clear();
        sendExecutor.shutdown();
    }
}