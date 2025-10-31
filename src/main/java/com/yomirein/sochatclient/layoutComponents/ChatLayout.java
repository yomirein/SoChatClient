package com.yomirein.sochatclient.layoutComponents;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChatLayout extends VerticalLayout {

    private final MessageList messageList = new MessageList();
    private final MessageInput messageInput = new MessageInput();

    public ChatLayout() {

        Div chatInput = new Div(messageInput);
        Div chatList = new Div(messageList);

        // CSS-классы
        addClassName("chatLayout");
        messageList.addClassName("chatList");
        messageInput.addClassName("chatMessageInput");
        chatInput.addClassName("chatInput");
        chatList.addClassName("chatMesasgeList");


        // Добавляем элементы в chatDiv
        add(chatList, chatInput);

        // Задаём размеры
        messageList.setSizeFull();
        chatList.setSizeFull();
        setSizeFull();

        configureMessageInput();
    }

    private void configureMessageInput() {
        messageInput.addSubmitListener(submitEvent -> {
            MessageListItem newMessage = new MessageListItem(
                    submitEvent.getValue(),
                    Instant.now(),
                    "username"
            );
            newMessage.setUserColorIndex(3);

            List<MessageListItem> items = new ArrayList<>(messageList.getItems());
            items.add(newMessage);
            messageList.setItems(items);

            // Автопрокрутка вниз
            messageList.getElement().executeJs("this.scrollTop = this.scrollHeight;");
        });
    }
}
