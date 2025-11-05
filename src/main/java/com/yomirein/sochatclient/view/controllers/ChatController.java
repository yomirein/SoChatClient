package com.yomirein.sochatclient.view.controllers;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.yomirein.sochatclient.config.WebSocketClient;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.ChatService;
import com.yomirein.sochatclient.view.ChatView;
import com.vaadin.flow.component.html.Div;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    private String token;

    public ChatController(String token) {
        this.token = token;
    }

    public List<Chat> getAllChats(ChatService chatService, Long id) {
        List<Chat> chatList1 = chatService.getChats(id, token);
        return chatList1;
    }

    public Chat createChat(ChatService chatService, Long userId) {
        Chat chat = chatService.createChat(userId, token);
        System.out.println(chat.toString() + " created");
        return chat;
    }


    public List<MessageListItem> getChatMessages(ChatService chatService, Long chatId, MessageList messageList) {
        List<Message> messages = chatService.getMessages(chatId, token);
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        for (Message message : messages){
            User user = chatService.getUser(message.getSenderId(), token);
            ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(message.getTimestamp());
            items.add(new MessageListItem(message.getContent(), message.getTimestamp().toInstant(zoneOffset), user.getUsername()));
        }
        return items;
    }

    public List<MessageListItem> sendMessage(WebSocketClient webSocketClient, Long chatId, MessageList messageList, MessageInput messageInput) {
        List<MessageListItem> items = new ArrayList<>(messageList.getItems());
        messageInput.addSubmitListener(submitEvent -> {
            System.out.println("масаге отправляется");
            MessageListItem newMessage = new MessageListItem(
                    submitEvent.getValue(), Instant.now(), "username");
            webSocketClient.sendMessage(chatId, submitEvent.getValue(), token);
            newMessage.setUserColorIndex(3);
            items.add(newMessage);
        });
        return items;
    }


}
