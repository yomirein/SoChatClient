package com.yomirein.sochatclient.view;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.controllers.ChatController;
import org.springframework.messaging.simp.stomp.StompSession;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Route("chat")
public class ChatView extends VerticalLayout {

    private Long selectedChat = null;
    private MessageInput messageInput = new MessageInput();
    private MessageList messageList = new MessageList();
    public Div chatList = new Div();
    public List<userInList> userList = new ArrayList<>();
    private StompSession.Subscription currentSubscription;

    ChatController chatController;
    ChatListView chatListView = new ChatListView(chatList);
    ChatMessagingView chatMessagingView = new ChatMessagingView(messageList, messageInput);
    WebSocketClient webSocketClient = new WebSocketClient();

    public ChatView(ChatService chatService) {
        UI ui = UI.getCurrent();

        User user = VaadinSession.getCurrent().getAttribute(User.class);
        String token = (String) VaadinSession.getCurrent().getAttribute("token");
        if (user == null || token == null) {
            System.out.println("nope");
            UI.getCurrent().getPage().setLocation("/login");
        }

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("root-host");

        chatController = new ChatController(token);

        ChatHeaderView chatHeaderView = new ChatHeaderView();
        ChatMainView chatMainView = new ChatMainView(chatListView, chatMessagingView);

        Button testCreateChatButton = new Button("Create Chat");
        testCreateChatButton.addClickListener(event -> {
            chatController.createChat(chatService, 2L);
        });

        add(new H3(user.toString()), testCreateChatButton, chatHeaderView, chatMainView);

        initChatUI(chatService, token, user);
    }



    private void subscribeToUserChat(ChatService chatService, Long chatId, String token, UI ui) {
        System.out.println("Subcribing to chat " + 1);
        webSocketClient.subscribeToChat(1L, token, msg -> {
            ui.access(() -> {
                List<MessageListItem> items = new ArrayList<>(messageList.getItems());

                User userSender = chatService.getUser(msg.getSenderId(), token);

                items.add(new MessageListItem(msg.getContent(), Instant.now(), userSender.getUsername()));
                messageList.setItems(items);
                System.out.println("Received message in chat " + 1 + ": " + msg.getContent());
            });
        });
    }

    private void initChatUI(ChatService chatService, String token, User user) {
        // 1️⃣ Подключаемся к WebSocket
        webSocketClient.connect(token).thenAccept(session -> {
            System.out.println("[LOG] Connected to WebSocket!");

            // 2️⃣ Загружаем все чаты асинхронно
            CompletableFuture.supplyAsync(() -> chatService.getChats(user.getId(), token))
                    .thenAccept(chats -> getUI().ifPresent(ui -> ui.access(() -> {
                        for (Chat chat : chats) {
                            Button btn = new userInList(chat.getId(), chat.getName());
                            btn.addClickListener(event -> {
                                chatController.openChat(chat.getId(), chatService, webSocketClient, messageList, ui, token);
                            });
                            chatList.add(btn);
                        }
                    })))
                    .exceptionally(ex -> { ex.printStackTrace(); return null; });

            // 3️⃣ Настраиваем отправку сообщений
            getUI().ifPresent(ui -> chatController.setupMessageSending(webSocketClient, messageList, messageInput, ui, token, user.getUsername()));

        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public class ChatHeaderView extends HorizontalLayout {
        public ChatHeaderView() {
            addClassName("app-header");
            setWidthFull();
            setAlignItems(FlexComponent.Alignment.CENTER);
            setPadding(true);
            setSpacing(true);

            H1 title = new H1("SoChat");
            title.addClassName("app-title");
            Button action = new Button("Log out");
            action.addClassName("app-action");

            add(title, action);
            expand(title);
        }
    }

    public class ChatMainView extends HorizontalLayout {
        public ChatMainView(ChatListView chatList, ChatMessagingView chatMessaging) {
            addClassName("main-layout");
            setSizeFull();
            setPadding(false);
            setSpacing(true);

            add(chatList, chatMessaging);

            setFlexGrow(1, chatList);
            setFlexGrow(2, chatMessaging);
        }
    }

    private class ChatListView extends VerticalLayout {
        public ChatListView(Div list){
            addClassName("left-panel");
            setPadding(true);
            setSpacing(true);
            setSizeFull();

            H2 listTitle = new H2("Chat     list");
            listTitle.addClassName("list-title");

            add(listTitle, list);
        }
    }

    private class ChatMessagingView extends VerticalLayout {

        public ChatMessagingView(MessageList messageList, MessageInput messageInput) {
            addClassName("right-panel");
            messageList.addClassName("chat");
            messageInput.addClassName("chatInput");
            messageList.setSizeFull();
            messageInput.setWidthFull();

            setSizeFull();
            setPadding(false);
            setSpacing(true);
            add(messageList, messageInput);
            expand(messageList);
        }
    }

    public static class userInList extends Button {
        Long id;
        String name;
        public userInList(Long id, String name) {
            this.id = id;
            this.name = name;

            addClassName("item");
            setText(name);
            setSizeFull();
        }
    }
}