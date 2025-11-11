package com.yomirein.sochatclient.view;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.AuthService;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.controllers.ChatController;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompSession;

import java.util.ArrayList;
import java.util.List;


@Route("chat")
public class ChatView extends VerticalLayout implements BeforeEnterObserver {

    private Long selectedChat = null;
    private MessageInput messageInput = new MessageInput();
    private MessageList messageList = new MessageList();

    public Div chatList = new Div();
    public Div friendList = new Div();

    public List<userInList> userList = new ArrayList<>();
    private StompSession.Subscription currentSubscription;

    ChatController chatController;
    SideListView sideListView = new SideListView();
    ChatMessagingView chatMessagingView = new ChatMessagingView(messageList, messageInput);
    ChatService chatService;
    WebSocketClient webSocketClient;
    private final AuthService authService;
    CookieStore cookieStore;

    @Autowired
    public ChatView(AuthService authServiceDang, ChatService chatService) {
        this.authService = authServiceDang;
        this.chatService = chatService;

        VaadinSession vaadinSession = VaadinSession.getCurrent();
        vaadinSession.lock();
        try {
            cookieStore = (CookieStore) vaadinSession.getAttribute("cookieStore");
            if (cookieStore != null) {
                for (Cookie c : cookieStore.getCookies()) {
                    System.out.println(c.getName() + " = " + c.getValue());
                }
            }

        } catch (Exception e)  {
            getUI().ifPresent(ui -> Notification.show(e.getMessage()));
            getUI().ifPresent(ui -> ui.access(() -> ui.navigate(LoginView.class)));
        }
        finally {
            vaadinSession.unlock();
        }

        try {
            webSocketClient = new WebSocketClient(cookieStore.getCookies());
        } catch (Exception e) {
            getUI().ifPresent(ui -> Notification.show("no cookies found"));
            getUI().ifPresent(ui -> ui.access(() -> ui.navigate(LoginView.class)));
        }
        UI ui = UI.getCurrent();

        User user = VaadinSession.getCurrent().getAttribute(User.class);
        if (user == null) {
            System.out.println("nope");
            UI.getCurrent().getPage().setLocation("/login");
        }

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("root-host");

        chatController = new ChatController();

        ChatHeaderView chatHeaderView = new ChatHeaderView();
        ChatMainView chatMainView = new ChatMainView(sideListView, chatMessagingView);

        chatHeaderView.dMSListButton.addClickListener(e -> {
            sideListView.toDMSList();
        });
        chatHeaderView.friendListButton.addClickListener(e -> {
            sideListView.toFriendsList();
        });


        Button button = new Button("getToken");
        button.addClickListener(e -> {
            add(new H4(cookieStore.getCookies().toString()));
        });

        try {
            chatController.initializeConnection(chatService, authService, webSocketClient, messageList, messageInput, ui, user,
                    chatHeaderView.logOutButton, chatList, friendList, sideListView.searchUserField, sideListView.addFriendButton);
        } catch (Exception e) {
            Notification.show(e.getMessage());
        }

        add(new H3(user.toString() + " " + cookieStore.getCookies()), button, chatHeaderView, chatMainView);
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        String token = null;
        if (cookieStore.getCookies() != null) {
            for (Cookie c : cookieStore.getCookies()) {
                if ("AUTH_TOKEN".equals(c.getName())) {
                    System.out.println("AUTH_TOKEN");
                    System.out.println(c.getValue());
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null || !authService.isTokenValid(token)) {
            event.forwardTo(LoginView.class);
        }
    }

    public class ChatHeaderView extends HorizontalLayout {
        public Button dMSListButton = new Button("DMS");
        public Button friendListButton = new Button("Friends");
        public Button logOutButton = new Button("Log out");
        public Div leftHeaderButtons = new Div();

        public ChatHeaderView() {
            addClassName("app-header");
            setWidthFull();
            setAlignItems(Alignment.CENTER);
            setPadding(true);
            setSpacing(true);

            logOutButton.addClassName("app-action");
            leftHeaderButtons.addClassName("left-header-items");
            dMSListButton.addClassName("left-header-item");
            friendListButton.addClassName("left-header-item");
            leftHeaderButtons.add(dMSListButton, friendListButton);


            add(leftHeaderButtons, logOutButton);
        }
    }

    public class ChatMainView extends HorizontalLayout {
        public ChatMainView(SideListView chatList, ChatMessagingView chatMessaging) {
            addClassName("main-layout");
            setSizeFull();
            setPadding(false);
            setSpacing(true);

            add(chatList, chatMessaging);

            setFlexGrow(1, chatList);
            setFlexGrow(2, chatMessaging);
        }
    }

    private class SideListView extends VerticalLayout {

        H2 soChatName = new H2("SoChat");
        H3 listTitle = new H3("DMS");

        Div searchBox = new Div();

        TextField searchUserField = new TextField();
        Button addFriendButton = new Button("Add");

        public SideListView() {
            addClassName("left-panel");
            setPadding(true);
            setSpacing(true);
            setSizeFull();

            chatList.setWidth("100%");
            friendList.setWidth("100%");
            searchBox.setWidth("100%");

            expand(searchUserField);
            searchBox.add(searchUserField);

            searchBox.addClassName("search-box");
            soChatName.addClassName("list-title");
            listTitle.addClassName("list-title");

            addFriendButton.addClassName("add-friend");
            searchUserField.addClassName("search-user");

            searchUserField.setPlaceholder("Search");

            add(soChatName, listTitle, searchBox, chatList);
        }

        public void toFriendsList(){
            listTitle.setText("Friends");
            remove(chatList);
            remove(searchBox);

            searchBox.add(addFriendButton);

            add(searchBox, friendList);
        }

        public void toDMSList(){
            listTitle.setText("DMS");
            remove(friendList);
            remove(searchBox);

            searchBox.remove(addFriendButton);

            add(searchBox, chatList);
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

            Avatar avatar = new Avatar(name);
            avatar.setWidth("32px");
            avatar.setHeight("32px");

            Span label = new Span(name);

            addClassName("item");
            setWidthFull();
            getElement().removeAllChildren();
            getElement().appendChild(avatar.getElement(), label.getElement());
        }
    }
}