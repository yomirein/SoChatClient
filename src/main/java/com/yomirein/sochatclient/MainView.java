package com.yomirein.sochatclient;

import com.vaadin.flow.router.PageTitle;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.simp.stomp.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Route("sochat")
@PageTitle("Messenger")
public class MainView extends HorizontalLayout {

    private final RestTemplate rest = new RestTemplate();
    private final String api = "http://localhost:8080/api";
    private final Grid<User> users = new Grid<>(User.class, false);
    private final Grid<Message> messages = new Grid<>(Message.class, false);
    private final TextField msgField = new TextField();
    private final Button sendBtn = new Button("Отправить");

    private User currentUser;
    private User targetUser;
    private Chat currentChat;
    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    public MainView() {
        setSizeFull();
        setPadding(true);

        users.addColumn(User::getUsername).setHeader("Пользователи");
        users.setItems(loadUsers());
        users.addItemClickListener(e -> {
            if (currentUser == null) {
                currentUser = e.getItem();
                Notification.show("Вы вошли как " + currentUser.getUsername());
            } else {
                targetUser = e.getItem();
                openChatWith(targetUser);
            }
        });

        messages.addColumn(m -> m.getSender().getUsername() + ": " + m.getContent())
                .setHeader("Сообщения");

        sendBtn.addClickListener(e -> {
            if (currentChat != null && currentUser != null) {
                sendMessage(msgField.getValue());
                msgField.clear();
            }
        });

        VerticalLayout chatLayout = new VerticalLayout(messages, new HorizontalLayout(msgField, sendBtn));
        chatLayout.setSizeFull();

        add(users, chatLayout);
        setFlexGrow(1, users);
        setFlexGrow(3, chatLayout);
    }

    private List<User> loadUsers() {
        return Arrays.asList(rest.getForObject(api + "/users", User[].class));
    }

    private List<Message> loadMessages(Long chatId) {
        return Arrays.asList(rest.getForObject(api + "/messages/" + chatId, Message[].class));
    }

    private void sendMessage(String text) {
        if (text.isBlank()) return;
        rest.postForObject(api + "/messages?chatId=" + currentChat.getId()
                + "&userId=" + currentUser.getId()
                + "&text=" + text, null, Message.class);
    }

    private void openChatWith(User other) {
        if (currentUser == null || other == null || currentUser.equals(other)) return;

        currentChat = rest.postForObject(api + "/chats/between?user1=" + currentUser.getId() +
                "&user2=" + other.getId(), null, Chat.class);

        messages.setItems(loadMessages(currentChat.getId()));
        connectWebSocket(currentChat.getId());
        Notification.show("Чат с " + other.getUsername());
    }

    private void connectWebSocket(Long chatId) {
        if (stompClient == null) {
            stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        }

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();

        stompClient.connectAsync("ws://localhost:8080/ws", headers, connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;

                session.subscribe("/topic/chat/" + chatId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return Message.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        Message newMsg = (Message) payload;
                        UI.getCurrent().access(() -> {
                            messages.setItems(loadMessages(chatId));
                        });
                    }
                });
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                exception.printStackTrace();
            }
        });
    }
}
