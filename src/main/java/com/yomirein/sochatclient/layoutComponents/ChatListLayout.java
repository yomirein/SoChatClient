package com.yomirein.sochatclient.layoutComponents;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

public class ChatListLayout extends VerticalLayout {

    private final ListBox<String> chatListBox = new ListBox<>();

    public ChatListLayout() {
        configureChatListBox();

        add(chatListBox);
        addClassName("chatListLayout");
        chatListBox.addClassName("chatListBox");

        setSizeFull();
    }

    private void configureChatListBox() {
        chatListBox.setItems("Username1", "Username2", "Username3");
        chatListBox.setValue("Username1");

        chatListBox.setRenderer(new ComponentRenderer<>(item -> {
            Button btn = new Button(item);
            btn.addClassName("listButton");
            return btn;
        }));

        chatListBox.setSizeFull();
    }
}
