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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Route("chat")
public class ChatView extends VerticalLayout {

    private Long selectedChat = null;
    private MessageInput messageInput = new MessageInput();
    private MessageList messageList = new MessageList();
    public Div chatList = new Div();
    public List<userInList> userList = new ArrayList<>();

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

        ChatHeaderView chatHeaderView = new ChatHeaderView();
        ChatMainView chatMainView = new ChatMainView(chatListView, chatMessagingView);

        add(new H3(user.toString()), chatHeaderView, chatMainView);
        expand(chatMainView);

        chatController = new ChatController(token);

        // Инициализируем UI сразу (кнопки, layout и т.д.)
        initUi(chatService, user, token, ui);

        // Подключаемся к WebSocket и подписываемся после подключения
        webSocketClient.connect(token).thenAccept(session -> {
            System.out.println("✅ Connected to WebSocket!");

            // Подписываемся на чаты только после успешного подключения
            subscribeToUserChats(chatService, user, ui);

        }).exceptionally(ex -> {
            System.err.println("❌ WebSocket connection error: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    public void initUi(ChatService chatService, User user, String token, UI ui) {
        ui.access(() -> {
            for (Chat chat : chatController.getAllChats(chatService, user.getId())){
                userList.add(new userInList(chat.getId(), chat.getName(), webSocketClient));
            }

            for (userInList button : userList){
                button.addClickListener(event -> {
                    List<Message> messages = chatService.getMessages(button.id, token);
                    for (Message message : messages){
                        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
                        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(message.getTimestamp());
                        MessageListItem newMessage = new MessageListItem(
                                message.getContent(),
                                message.getTimestamp().toInstant(zoneOffset),
                                message.getSenderId().toString()
                        );
                        items.add(newMessage);
                        messageList.setItems(items);
                    }
                });
                chatList.add(button);
            }

            if (selectedChat != null) {
                chatController.sendMessage(webSocketClient, selectedChat, messageList, messageInput);
                messageList.setItems(chatController.getChatMessages(chatService, selectedChat, messageList));
            }
        });
    }

    private void subscribeToUserChats(ChatService chatService, User user, UI ui) {
        List<Chat> userChats = chatController.getAllChats(chatService, user.getId());
        System.out.println("📡 Subscribing to " + userChats.size() + " chats");

        for (Chat chat : userChats) {
            webSocketClient.subscribeToChat(chat.getId(), msg -> {
                System.out.println("💬 Received message in chat " + chat.getId() + ": " + msg.getContent());
                ui.access(() -> {
                    List<MessageListItem> items = new ArrayList<>(messageList.getItems());
                    ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(msg.getTimestamp());
                    MessageListItem newMessage = new MessageListItem(
                            msg.getContent(),
                            msg.getTimestamp().toInstant(zoneOffset),
                            msg.getSenderId().toString()
                    );
                    items.add(newMessage);
                    messageList.setItems(items);
                });


            });
        }
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
        public userInList(Long id, String name, WebSocketClient webSocketClient) {
            this.id = id;
            this.name = name;

            addClassName("item");
            setText(name);
            setSizeFull();
        }
    }
}