package com.yomirein.sochatclient.view;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.messaging.simp.stomp.StompSession;

import java.util.ArrayList;
import java.util.List;

@Route("chatDesignTest")
public class ChatViewDesignTest extends VerticalLayout{
    private MessageInput messageInput = new MessageInput();
    private MessageList messageList = new MessageList();
    public Div chatList = new Div();
    public List<userInList> userList = new ArrayList<>();
    private StompSession.Subscription currentSubscription;

    ChatListView chatListView = new ChatListView(chatList);
    ChatMessagingView chatMessagingView = new ChatMessagingView(messageList, messageInput);

    public ChatViewDesignTest() {
        UI ui = UI.getCurrent();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("root-host");

        ChatHeaderView chatHeaderView = new ChatHeaderView();
        ChatMainView chatMainView = new ChatMainView(chatListView, chatMessagingView);

        add(chatHeaderView, chatMainView);
    }

    public class ChatHeaderView extends HorizontalLayout {
        public ChatHeaderView() {
            addClassName("app-header");
            setWidthFull();
            setAlignItems(Alignment.CENTER);
            setPadding(true);
            setSpacing(true);

            Button logOutButton = new Button("Log out");
            logOutButton.addClassName("app-action");

            Button friendListButton = new Button("Friends");
            friendListButton.addClassName("left-header-buttons");

            add(friendListButton, logOutButton);
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

            H2 listTitle = new H2("Friends");
            listTitle.addClassName("list-title");

            list.setWidth("100%");

            Button btn = new userInList(1l, "UserNameTEST");
            list.add(btn);
            Button btn2 = new userInList(2l, "UserNameTEST2");
            list.add(btn2);
            Button btn3 = new userInList(3l, "user");
            list.add(btn3);
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

    public class userInList extends Button {
        Long id;
        String name;
        public userInList(Long id, String name) {
            this.id = id;
            this.name = name;

            Avatar avatar = new Avatar(name);
            avatar.setWidth("32px");
            avatar.setHeight("32px");

            Span label = new Span(name);

            setSpacing("true");
            addClassName("item");
            setWidthFull();
            getElement().removeAllChildren();
            getElement().appendChild(avatar.getElement(), label.getElement());
        }
    }
}