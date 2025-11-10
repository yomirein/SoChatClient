package com.yomirein.sochatclient.view;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.messaging.simp.stomp.StompSession;

import java.util.ArrayList;
import java.util.List;

@Route("chatDesignTest")
public class ChatViewDesignTest extends VerticalLayout{
    private MessageInput messageInput = new MessageInput();
    private MessageList messageList = new MessageList();
    public Div chatList = new Div();
    public Div friendList = new Div();

    public List<userInList> userList = new ArrayList<>();
    private StompSession.Subscription currentSubscription;

    SideListView sideListView = new SideListView();

    ChatMessagingView chatMessagingView = new ChatMessagingView(messageList, messageInput);

    public ChatViewDesignTest() {
        UI ui = UI.getCurrent();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("root-host");

        ChatHeaderView chatHeaderView = new ChatHeaderView();
        ChatMainView chatMainView = new ChatMainView(sideListView, chatMessagingView);

        chatHeaderView.dMSListButton.addClickListener(e -> {
            sideListView.toDMSList();
        });
        chatHeaderView.friendListButton.addClickListener(e -> {
            sideListView.toFriendsList();
        });
        Button btn = new userInList(1l, "UserNameTEST");
        chatList.add(btn);
        Button btn2 = new userInList(2l, "UserNameTEST2");
        chatList.add(btn2);
        Button btn3 = new userInList(3l, "user");
        chatList.add(btn3);


        Button btn4 = new userInList(1l, "HEYY");
        friendList.add(btn4);
        Button btn5 = new userInList(2l, "YOOO");
        friendList.add(btn5);
        Button btn6 = new userInList(3l, "ZAMN");
        friendList.add(btn6);

        add(chatHeaderView, chatMainView);
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