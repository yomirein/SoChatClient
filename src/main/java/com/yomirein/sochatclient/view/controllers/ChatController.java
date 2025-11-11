package com.yomirein.sochatclient.view.controllers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.CookieUtils;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.*;
import com.yomirein.sochatclient.service.AuthService;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.ChatView;
import com.vaadin.flow.component.html.Div;
import com.yomirein.sochatclient.view.LoginView;
import jakarta.servlet.http.Cookie;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.checkerframework.checker.units.qual.C;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.client.RestTemplate;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatController {

    @Getter
    @Setter
    private final Map<Long, StompSession.Subscription> subscriptions = new HashMap<>();

    @Getter
    @Setter
    private Long selectedChat = null;

    private final AtomicBoolean messageListenerRegistered = new AtomicBoolean(false);
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
    private CookieStore getOrCreateSessionCookieStore() {
        VaadinSession vs = VaadinSession.getCurrent();
        if (vs == null) return null;
        vs.lock();
        try {
            CookieStore cs = (CookieStore) vs.getAttribute("cookieStore");
            if (cs == null) {
                cs = new BasicCookieStore();
                vs.setAttribute("cookieStore", cs);
            }
            return cs;
        } finally {
            vs.unlock();
        }
    }

    private String derivePeerName(ChatWithUsers chat, User user, List<ChatWithUsers> chats) {
        for (User user1 : chat.getParticipants()) {
            if (!user1.getId().equals(user.getId())) {
                return user1.getUsername();
            }
        }
        return chat.getName();
    }

    public void initializeConnection(ChatService chatService, AuthService authService,
                                     WebSocketClient webSocketClient,
                                     MessageList messageList,
                                     MessageInput messageInput,
                                     UI ui,
                                     User user, Button logOutButton,
                                     Div chatList, Div friendList, TextField addFriendField, Button addFriend) {

        final CookieStore sessionCookieStore = getOrCreateSessionCookieStore();

        ui.access(() -> {
            addFriend.addClickListener(event -> {
                try {
                    chatService.createChat(Long.valueOf(addFriendField.getValue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ui.push();
            });

            // logout
            logOutButton.addClickListener(event -> {
                try {
                    authService.logout();
                    ui.navigate(LoginView.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ui.push();
            });

            // регистрация listener'а отправки сообщений — только один раз
            if (messageListenerRegistered.compareAndSet(false, true)) {
                setMessageSending(webSocketClient, messageList, messageInput, ui, sessionCookieStore, chatService);
            }
        });

        webSocketClient.connect().thenAccept(wsSession -> {
            System.out.println("[LOG] Connected to WebSocket!");

            final CookieStore snapshot = CookieUtils.snapshotCookieStore(sessionCookieStore);
            final RestTemplate restForBg = AuthService.createRestTemplateFromCookieStore(snapshot);

            CompletableFuture.supplyAsync(() -> {
                List<Chat> chatsList = chatService.getChatsUsingRest(restForBg, user.getId());
                List<ChatWithUsers> chatsListWithUsers = new ArrayList<>();

                for (Chat chat : chatsList) {
                    List<User> usersList = new ArrayList<>();
                    for (Long participantId : chat.getParticipants()) {
                        usersList.add(chatService.getUserUsingRest(restForBg, participantId));
                    }
                    ChatWithUsers chatWithUsers = new ChatWithUsers();
                    chatWithUsers.setId(chat.getId());
                    chatWithUsers.setName(chat.getName());
                    chatWithUsers.setGroup(chat.isGroup());
                    chatWithUsers.setParticipants(usersList);
                    chatsListWithUsers.add(chatWithUsers);
                }

                return chatsListWithUsers;
            })
                    .thenAccept(chatsList -> {
                        ui.access(() -> {
                            VaadinSession vs = VaadinSession.getCurrent();
                            if (vs != null) {
                                CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                                if (ss != null) CookieUtils.mergeCookieStore(ss, snapshot);
                            }

                            chatList.removeAll();
                            for (ChatWithUsers chat : chatsList) {
                                String chatName = derivePeerName(chat, user, chatsList);
                                ChatView.userInList btn = new ChatView.userInList(chat.getId(), chatName);
                                btn.addClickListener(e -> openChat(chatService, webSocketClient, messageList, ui, chat.getId()));
                                chatList.add(btn);
                            }
                            ui.push();
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });

        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public void openChat(ChatService chatService, WebSocketClient webSocketClient,
                         MessageList messageList, UI ui, Long chatId) {

        final CookieStore sessionStore = getOrCreateSessionCookieStore();
        selectedChat = chatId;
        final CookieStore snapshot = CookieUtils.snapshotCookieStore(sessionStore);
        final RestTemplate restForBg = AuthService.createRestTemplateFromCookieStore(snapshot);

        CompletableFuture.supplyAsync(() -> chatService.getMessagesUsingRest(restForBg, chatId))
                .thenAccept(messages -> {
                    ui.access(() -> {
                        VaadinSession vs = VaadinSession.getCurrent();
                        if (vs != null) {
                            CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                            if (ss != null) CookieUtils.mergeCookieStore(ss, snapshot);
                        }

                        List<MessageListItem> items = new ArrayList<>();
                        for (Message m : messages) {
                            User sender = chatService.getUser(m.getSenderId()); // UI-версия
                            MessageListItem item = new MessageListItem();
                            item.setText(m.getContent());
                            item.setTime(m.getTimestamp().toInstant(ZoneOffset.UTC));
                            item.setUserName(sender.getUsername());
                            items.add(item);
                        }
                        messageList.setItems(items);
                        ui.push();
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });

        if (subscriptions.containsKey(chatId)) {
            System.out.println("[LOG] Already subscribed to chat " + chatId);
            return;
        }

        final CookieStore subSnapshot = CookieUtils.snapshotCookieStore(sessionStore);
        final RestTemplate restForSubscription = AuthService.createRestTemplateFromCookieStore(subSnapshot);

        StompSession.Subscription subscription = webSocketClient.subscribeToChat(chatId, msg -> {
            CompletableFuture.runAsync(() -> {
                try {
                    User msgSender = chatService.getUserUsingRest(restForSubscription, msg.getSenderId());

                    ui.access(() -> {
                        VaadinSession vs = VaadinSession.getCurrent();
                        if (vs != null) {
                            CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                            if (ss != null) CookieUtils.mergeCookieStore(ss, subSnapshot);
                        }

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
                        ui.push();
                        System.out.println("[LOG] messageList updated, total: " + items.size());
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        subscriptions.put(chatId, subscription);
        ui.access(() -> {
            ui.addDetachListener(event -> {
                StompSession.Subscription sub = subscriptions.remove(chatId);
                if (sub != null) {
                    sub.unsubscribe();
                    System.out.println("[LOG] Unsubscribed from chat " + chatId + " due to UI detach");
                }
            });
        });
    }

    // --- setMessageSending ---
    public void setMessageSending(WebSocketClient webSocketClient,
                                  MessageList messageList,
                                  MessageInput messageInput,
                                  UI ui,
                                  CookieStore sessionCookieStore,
                                  ChatService chatService) {
        messageInput.addSubmitListener(submitEvent -> {
            String content = submitEvent.getValue();
            if (content == null || content.isBlank()) return;
            final Long chatIdSnapshot = selectedChat;
            if (chatIdSnapshot == null) {
                System.out.println("[LOG] No chat selected, skip send");
                return;
            }
            final CookieStore snapshot = CookieUtils.snapshotCookieStore(sessionCookieStore);
            CompletableFuture.runAsync(() -> {
                try {
                    webSocketClient.sendMessage(chatIdSnapshot, content);
                    ui.access(() -> {
                        VaadinSession vs = VaadinSession.getCurrent();
                        if (vs != null) {
                            CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                            if (ss != null) CookieUtils.mergeCookieStore(ss, snapshot);
                        }

                        ui.push();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}