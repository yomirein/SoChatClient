package com.yomirein.sochatclient.layout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChatLayout extends VerticalLayout {

    private final Div chatDiv = new Div();
    private final MessageList messageList = new MessageList();
    private final MessageInput messageInput = new MessageInput();

    public ChatLayout() {
        addClassName("chatLayout");

        VerticalLayout chatInput = new VerticalLayout(messageInput);
        VerticalLayout chatList = new VerticalLayout(messageList);

        // CSS-классы
        chatDiv.addClassName("chatDiv");
        chatList.addClassName("chatList");
        chatInput.addClassName("chatInput");

        // Добавляем элементы в chatDiv
        chatDiv.add(chatList, chatInput);
        add(chatDiv);

        // Задаём размеры
        setSizeFull();
        messageList.setWidthFull();

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
            items.add(0, newMessage);
            messageList.setItems(items);

            // Автопрокрутка вниз
            messageList.getElement().executeJs("this.scrollTop = this.scrollHeight;");
        });
    }
}