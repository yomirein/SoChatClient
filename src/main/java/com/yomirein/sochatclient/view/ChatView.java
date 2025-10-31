package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Route("chat")
public class ChatView extends VerticalLayout {
    private TextField getUserByIdInput = new TextField();

    private Div messages = new Div();
    private TextField input = new TextField();

    public ChatView(ChatService chatService, WebSocketClient webSocketClient) {
        User user = VaadinSession.getCurrent().getAttribute(User.class);
        String token = (String) VaadinSession.getCurrent().getAttribute("token");

        Text tokenText = new Text(user.getId() + ": " + token + " - " + user.getUsername());

        try {
            webSocketClient.connect(token);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        /*
        webSocketClient.subscribeToChat(null, null);

        webSocketClient.subscribeToChat(chatId, message -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                chatMessagesList.add(message.getSenderId() + ": " + message.getContent());
            }));
        });*/

        Div chatList = new Div();
        chatList.add(new Text("lelelele"));

        Button createChat = new Button("create chat", e -> {
            if (!getUserByIdInput.isEmpty()) {
                Chat chatTest = chatService.createChat(user.getId(), Long.parseLong(getUserByIdInput.getValue()), token);
            }
        });
        Button getChats = new Button("getchats", e -> {
            List<Chat> chatList1 = chatService.getChats(user.getId(), token);
            for (Chat chat : chatList1) {
                System.out.println(chat.toString());
                chatList.add(new Text(chat.getName() + "\n"));
            }
        });


        messages.setHeight("300px");
        messages.getStyle().set("overflow", "auto");

        Button send = new Button("Send", e -> {
            if (!input.isEmpty()) {
                messages.add(new Div(new com.vaadin.flow.component.Text(input.getValue())));
                webSocketClient.sendMessage(null, input.getValue());
                input.clear();
            }
        });

        add(getChats ,getUserByIdInput, createChat, messages, input, send, tokenText, chatList);
    }
}