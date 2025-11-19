package com.yomirein.sochatclient.view.controllers;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.CookieUtils;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.converter.PayloadConverter;
import com.yomirein.sochatclient.events.EventMessage;
import com.yomirein.sochatclient.model.*;
import com.yomirein.sochatclient.service.AuthService;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.CallView;
import com.yomirein.sochatclient.view.ChatView;
import com.vaadin.flow.component.html.Div;
import com.yomirein.sochatclient.view.LoginView;
import com.yomirein.sochatclient.view.components.Notifications;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.checkerframework.checker.units.qual.C;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    private String derivePeerName(ChatWithUsers chat, User user) {
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
                                     UI ui, Button testEventButton,
                                     User user, Button logOutButton,
                                     Div chatList, Div friendList, TextField addFriendField, Button addFriend, ChatView.ChatMainView chatMainView) {

        final CookieStore sessionCookieStore = getOrCreateSessionCookieStore();

        ui.access(() -> {
            addFriend.addClickListener(event -> {
                try {
                    webSocketClient.createChat(addFriendField.getValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ui.push();
            });

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
                setMessageSending(webSocketClient, messageList, messageInput, ui, testEventButton, sessionCookieStore, chatService);
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
                                String chatName = derivePeerName(chat, user);
                                ChatView.userInList btn = new ChatView.userInList(chat.getId(), chatName);
                                btn.addClickListener(e -> openChat(chatService, webSocketClient, messageList,
                                        messageInput, ui, chat.getId(), testEventButton, chatMainView, user));
                                chatList.add(btn);
                            }
                            ui.push();
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });

            final CookieStore subSnapshot = CookieUtils.snapshotCookieStore(sessionCookieStore);
            final RestTemplate restForSubscription = AuthService.createRestTemplateFromCookieStore(subSnapshot);
            StompSession.Subscription subcribeToChatEvents = webSocketClient.subscribeChatEvents(user.getId(), event -> {
                CompletableFuture.runAsync(() -> {
                    switch (event.getEventType()){
                        case "sendMessage":
                            event.getPayload().getClass().getName();
                            Message message = PayloadConverter.convertPayload(event.getPayload(), Message.class);

                            if (selectedChat == message.getChatId()) {
                                User msgSender = chatService.getUserUsingRest(restForSubscription, message.getSenderId());
                                List<MessageListItem> items = new ArrayList<>(messageList.getItems());
                                char firstChar = msgSender.getUsername().charAt(0);
                                int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;

                                MessageListItem item = new MessageListItem();
                                item.setText(message.getContent());
                                item.setTime(Instant.now());
                                item.setUserName(msgSender.getUsername());
                                item.setUserAbbreviation(msgSender.getUsername().substring(0, 1));
                                item.setUserColorIndex(colorIndex);
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
                                    items.add(item);
                                    messageList.setItems(items);
                                    ui.push();
                                    System.out.println("[LOG] messageList updated, total: " + items.size());
                                });
                            }
                            else {
                                User sender = chatService.getUserUsingRest(restForSubscription, message.getSenderId());
                                ui.access(() -> {
                                    Notification notification = Notifications.show(sender.getUsername(), message.getContent());
                                    notification.open();
                                });
                            }

                            break;
                        case "chatCreate":
                            System.out.println("chatCreated");
                            Chat chat = PayloadConverter.convertPayload(event.getPayload(), Chat.class);
                            List<User> usersList = new ArrayList<>();
                            for (Long participantId : chat.getParticipants()) {
                                usersList.add(chatService.getUserUsingRest(restForSubscription, participantId));
                            }
                            for (var v : usersList){
                                System.out.println("[LOG] user: " + v.getUsername());
                            }
                            ChatWithUsers chatWithUsers = new ChatWithUsers();
                            chatWithUsers.setId(chat.getId());
                            chatWithUsers.setName(chat.getName());
                            chatWithUsers.setGroup(chat.isGroup());
                            chatWithUsers.setParticipants(usersList);
                            String chatName = derivePeerName(chatWithUsers, user);
                            ChatView.userInList btn = new ChatView.userInList(chat.getId(), chatName);

                            ui.access(() -> {
                                VaadinSession vs = VaadinSession.getCurrent();
                                if (vs != null) {
                                    CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                                    if (ss != null) CookieUtils.mergeCookieStore(ss, subSnapshot);
                                }

                                if (!ui.isAttached()) {
                                    System.out.println("[LOG] UI detached, skipping chat update");
                                    return;
                                }
                                btn.addClickListener(e -> openChat(chatService, webSocketClient, messageList, messageInput, ui, chat.getId(), testEventButton, chatMainView, user));
                                chatList.add(btn);

                                ui.push();
                            });
                            break;
                    }
                });
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });


    }

    public void openChat(ChatService chatService, WebSocketClient webSocketClient,
                         MessageList messageList, MessageInput messageInput, UI ui, Long chatId,
                         Button testEventButton, ChatView.ChatMainView chatMainView, User user) {

        final CookieStore sessionStore = getOrCreateSessionCookieStore();
        selectedChat = chatId;
        int lastMessageCount = 0;
        final CookieStore snapshot = CookieUtils.snapshotCookieStore(sessionStore);
        final RestTemplate restForBg = AuthService.createRestTemplateFromCookieStore(snapshot);

        CompletableFuture.supplyAsync(() -> chatService.getMessagesUsingRest(restForBg, chatId, 0))
                .thenAccept(messages -> {

                    List<MessageListItem> items = new ArrayList<>();
                    List<User> userList = new ArrayList<>();
                    User reciever = null;
                    for (Message m : messages) {
                        User sender = chatService.getUserUsingRest(restForBg, m.getSenderId()); // UI-версия

                        if (sender != user){
                            reciever = sender;
                        }

                        char firstChar = sender.getUsername().charAt(0);
                        int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;
                        MessageListItem item = new MessageListItem();
                        item.setText(m.getContent());
                        item.setTime(m.getTimestamp().toInstant(ZoneOffset.UTC));
                        item.setUserName(sender.getUsername());
                        item.setUserColorIndex(colorIndex);
                        items.add(0, item);
                    }
                    final User finalUser = reciever;

                    ui.access(() -> {
                        messageList.setClassName("chat");
                        messageInput.setClassName("chatInput");
                        messageList.setSizeFull();
                        messageInput.setWidthFull();

                        if (finalUser != null) {
                            CallView callView = new CallView(user, finalUser);
                            chatMainView.add(callView);
                        }

                        VaadinSession vs = VaadinSession.getCurrent();
                        if (vs != null) {
                            CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                            if (ss != null) CookieUtils.mergeCookieStore(ss, snapshot);
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
    }

    public void setMessageSending(WebSocketClient webSocketClient,
                                  MessageList messageList,
                                  MessageInput messageInput,
                                  UI ui, Button button,
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
        messageList.getElement().addEventListener("scroll-top", e -> {
            System.out.println("[LOG] Scroll top");
            final Long chatIdSnapshot = selectedChat;
            if (chatIdSnapshot == null) {
                System.out.println("[LOG] No chat selected, skip load");
                return;
            }

            final CookieStore snapshot = CookieUtils.snapshotCookieStore(sessionCookieStore);
            final RestTemplate restForBg = AuthService.createRestTemplateFromCookieStore(snapshot);

            CompletableFuture.supplyAsync(() -> chatService.getMessagesUsingRest(restForBg, chatIdSnapshot, (int)(messageList.getItems().size())/30))
                    .thenAccept(messages -> {
                        if (messages.size() == 0) {
                            return;
                        }

                        System.out.println("starting load messages from " + (messageList.getItems().size())/30);
                        ui.access(() -> {
                            VaadinSession vs = VaadinSession.getCurrent();
                            if (vs != null) {
                                CookieStore ss = (CookieStore) vs.getAttribute("cookieStore");
                                if (ss != null) CookieUtils.mergeCookieStore(ss, snapshot);
                            }

                            List<MessageListItem> items = new ArrayList<>(messageList.getItems());
                            for (Message m : messages) {
                                User sender = chatService.getUserUsingRest(restForBg, m.getSenderId()); // UI-версия
                                MessageListItem item = new MessageListItem();
                                System.out.println(m.getContent());
                                item.setText(m.getContent());
                                item.setTime(m.getTimestamp().toInstant(ZoneOffset.UTC));
                                item.setUserName(sender.getUsername());
                                items.add(0, item);
                            }

                            System.out.println("loaded 30 messages");

                            messageList.setItems(items);
                            ui.push();
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        });
    }
}