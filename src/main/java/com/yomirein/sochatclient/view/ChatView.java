package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("chat")
public class ChatView extends VerticalLayout {
    private Div messages = new Div();
    private TextField input = new TextField();

    public ChatView() {
        messages.setHeight("300px");
        messages.getStyle().set("overflow", "auto");

        Button send = new Button("Send", e -> {
            if (!input.isEmpty()) {
                messages.add(new Div(new com.vaadin.flow.component.Text(input.getValue())));
                input.clear();
            }
        });

        add(messages, input, send);
    }
}