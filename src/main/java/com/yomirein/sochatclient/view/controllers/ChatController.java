package com.yomirein.sochatclient.view.controllers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.Response;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.ChatView;
import com.vaadin.flow.component.html.Div;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.task.TaskExecutor;
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

    @Getter
    @Setter
    private final Map<Long, StompSession.Subscription> subscriptions = new HashMap<>();

    @Getter
    @Setter
    private Long selectedChat = null;

    public ChatController() {

    }

    public List<Chat> getAllChats(ChatService chatService, Long id) {
        List<Chat> chatList1 = chatService.getChats(id);
        return chatList1;
    }

    public Chat createChat(ChatService chatService, Long userId) {
        Chat chat = chatService.createChat(userId);
        System.out.println(chat.toString() + " created");
        return chat;
    }


    public List<MessageListItem> getChatMessages(ChatService chatService, Long chatId, MessageList messageList) {
        List<Message> messages = chatService.getMessages(chatId);
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        for (Message message : messages){
            User user = chatService.getUser(message.getSenderId());
            ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(message.getTimestamp());
            items.add(new MessageListItem(message.getContent(), message.getTimestamp().toInstant(zoneOffset), user.getUsername()));
        }
        return items;
    }

    public List<MessageListItem> sendMessage(WebSocketClient webSocketClient, Long chatId, MessageList messageList, MessageInput messageInput) {
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        messageInput.addSubmitListener(submitEvent -> {
            webSocketClient.sendMessage(chatId, submitEvent.getValue());
            System.out.println("масаге отправляется");
            MessageListItem newMessage = new MessageListItem(
                    submitEvent.getValue(), Instant.now(), "username");
            newMessage.setUserColorIndex(3);
            items.add(newMessage);
        });
        return items;
    }

    public void initializeConnection(ChatService chatService,
                                     WebSocketClient webSocketClient,
                                     MessageList messageList,
                                     MessageInput messageInput,
                                     UI ui,
                                     User user,
                                     Div chatList) {

        webSocketClient.connect().thenAccept(session -> {
            System.out.println("[LOG] Connected to WebSocket!");

            CompletableFuture.supplyAsync(() -> {
                        var chatLList = chatService.getChats(user.getId());
                        List<User> chatParticipants = new ArrayList<>();
                        for (Chat chat : chatLList){
                            for (Long userId :chat.getParticipants()){
                                if (!chatParticipants.contains(userId)) {
                                    chatParticipants.add(chatService.getUser(userId));
                                }
                            }
                        }
                        return new Response.ChatWithExtras(chatLList, chatParticipants);
                    })
                    .thenAccept(chats -> ui.access(() -> {
                        if (!ui.isAttached()) {
                            System.out.println("[LOG] UI detached, skipping chat list update");
                            return;
                        }
                        chatList.removeAll();

                        for (Chat chat : chats.getChats()) {
                            String chatName = "";
                            if (!chat.isGroup()) {
                                for (User user1 : chats.getUsers()) {
                                    if (chat.getParticipants().contains(user1.getId())) {
                                        System.out.println("[LOG] User " + user1.getUsername() + " is part of chat " + chat.getId());
                                        if (user1.getId() != user.getId()) {
                                            chatName = user1.getUsername();
                                            System.out.println("[LOG] User " + user.getUsername() + " using name " + user1.toString() + " in chat " + chat.toString());
                                        }
                                    }
                                }
                            } else{
                                chatName = chat.getName();
                            }

                            Button btn = new ChatView.userInList(chat.getId(), chatName);
                            btn.addClickListener(event ->
                                    openChat(chatService, webSocketClient, messageList, ui, chat.getId())
                            );
                            chatList.add(btn);
                        }

                        ui.push(); // 🔥 обновляем список чатов на клиенте сразу
                    }))
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });

            // Инициализация отправки сообщений
            setMessageSending(webSocketClient, messageList, messageInput, ui);

        }).exceptionally(ex -> {
            System.err.println("[ERROR] Failed to connect to WebSocket: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    public void openChat(ChatService chatService,
                         WebSocketClient webSocketClient,
                         MessageList messageList,
                         UI ui,
                         Long chatId) {

        selectedChat = chatId;
        System.out.println("[LOG] Opening chat " + chatId);

        // 1️⃣ Загрузка истории сообщений
        CompletableFuture.supplyAsync(() -> chatService.getMessages(chatId))
                .thenAccept(messages -> ui.access(() -> {
                    if (!ui.isAttached()) {
                        System.out.println("[LOG] UI detached, skipping loading old messages");
                        return;
                    }

                    List<MessageListItem> items = new ArrayList<>();
                    for (Message message : messages) {
                        User msgSender = chatService.getUser(message.getSenderId());

                        char firstChar = msgSender.getUsername().charAt(0);
                        int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;

                        MessageListItem item = new MessageListItem();
                        item.setText(message.getContent());
                        item.setTime(message.getTimestamp().toInstant(ZoneOffset.UTC));
                        item.setUserName(msgSender.getUsername());

                        item.setUserAbbreviation(msgSender.getUsername().substring(0, 1)); // первая буква имени
                        item.setUserColorIndex(colorIndex);

                        items.add(item);
                    }
                    messageList.setItems(items);
                    ui.push(); // 🔥 сразу обновляем браузер
                    System.out.println("[LOG] Loaded " + items.size() + " old messages.");
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });

        // 2️⃣ Проверка на уже существующую подписку
        if (subscriptions.containsKey(chatId)) {
            System.out.println("[LOG] Already subscribed to chat " + chatId);
            return;
        }

        // 3️⃣ Подписка на обновления через WebSocket
        Thread.startVirtualThread(() -> {
            StompSession.Subscription subscription = webSocketClient.subscribeToChat(chatId, msg -> {
                System.out.println("[LOG] WebSocket message received: " + msg.getContent());

                Thread.startVirtualThread(() -> {
                    try {
                        User msgSender = chatService.getUser(msg.getSenderId());

                        ui.access(() -> {
                            if (!ui.isAttached()) {
                                System.out.println("[LOG] UI detached, skipping message update");
                                return;
                            }

                            List<MessageListItem> items = new ArrayList<>(messageList.getItems());

                            char firstChar = msgSender.getUsername().charAt(0);
                            int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;

                            MessageListItem item = new MessageListItem();
                            item.setText(msg.getContent());
                            item.setTime(Instant.now());
                            item.setUserName(msgSender.getUsername());

                            item.setUserAbbreviation(msgSender.getUsername().substring(0, 1));
                            item.setUserColorIndex(colorIndex);

                            items.add(item);

                            messageList.setItems(items);
                            ui.push(); // 🔥 обновление на клиенте сразу
                            System.out.println("[LOG] messageList updated, total: " + items.size());
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            subscriptions.put(chatId, subscription);
            System.out.println("[LOG] Subscribed to chat " + chatId + " (virtual thread)");

            // 4️⃣ Отписка при отсоединении UI
            ui.addDetachListener(event -> {
                StompSession.Subscription sub = subscriptions.get(chatId);
                if (sub != null) {
                    sub.unsubscribe();
                    System.out.println("[LOG] Unsubscribed from chat " + chatId + " due to UI detach");
                }
            });
        });
    }


    public void setMessageSending(WebSocketClient webSocketClient,
                                  MessageList messageList,
                                  MessageInput messageInput,
                                  UI ui) {

        ui.access(() -> messageInput.addSubmitListener(submitEvent -> {
            String content = submitEvent.getValue();
            if (content == null || content.isBlank() || selectedChat == null) return;

            Thread.startVirtualThread(() -> {
                try {
                    // Отправляем сообщение через WebSocket
                    webSocketClient.sendMessage(selectedChat, content);
                    System.out.println("[LOG] Message sent to chat " + selectedChat + ": " + content);

                    // Добавляем сообщение сразу в UI
                    ui.access(() -> {
                        if (!ui.isAttached()) {
                            System.out.println("[LOG] UI detached, skipping local message update");
                            return;
                        }
                        ui.push(); // 🔥 мгновенное обновление UI
                        System.out.println("[LOG] UI updated immediately after sending message.");
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }));
    }
}