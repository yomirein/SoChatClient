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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.controllers.ChatController;
import org.springframework.core.task.TaskExecutor;
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
    ChatService chatService = new ChatService();

    public ChatView() {
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

        chatController.initializeConnection(chatService, webSocketClient, messageList, messageInput, ui, user, chatList, token);

        add(/*new H3(user.toString()),*/ chatHeaderView, chatMainView);
    }

    public class ChatHeaderView extends HorizontalLayout {
        public ChatHeaderView() {
            addClassName("app-header");
            setWidthFull();
            setAlignItems(FlexComponent.Alignment.CENTER);
            setPadding(true);
            setSpacing(true);

            H1 title = new H1("");
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