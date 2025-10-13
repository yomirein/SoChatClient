package com.yomirein.sochatclient;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * A sample Vaadin view class.
 * <p>
 * To implement a Vaadin view just extend any Vaadin component and use @Route
 * annotation to announce it in a URL as a Spring managed bean.
 * <p>
 * A new instance of this class is created for every new user and every browser
 * tab/window.
 * <p>
 * The main view contains a text field for getting the user name and a button
 * that shows a greeting message in a notification.
 */
@Route
public class MainView extends VerticalLayout {

    /**
     * Construct a new Vaadin view.
     * <p>
     * Build the initial UI state for the user accessing the application.
     *
     * @param service
     *            The message service. Automatically injected Spring managed bean.
     */
    public MainView(GreetService service){

        Div chatDiv = new Div();
        MessageList list = new MessageList();
        MessageInput input = new MessageInput();
        input.addSubmitListener(submitEvent -> {
            MessageListItem newMessage = new MessageListItem(
                    submitEvent.getValue(), Instant.now(), "username");
            newMessage.setUserColorIndex(3);
            List<MessageListItem> items = new ArrayList<>(list.getItems());
            items.add(newMessage);
            list.setItems(items);
        });

        chatDiv.add(list, input);

        HorizontalLayout upLayout = new HorizontalLayout();


        HorizontalLayout everythingLayout = new HorizontalLayout(chatDiv);


        VerticalLayout chatLayout = new VerticalLayout(chatDiv);
        HorizontalLayout chatListLayout = new HorizontalLayout();


        everythingLayout.add(chatListLayout, chatLayout);

        chatLayout.expand(list);

        everythingLayout.setSizeFull();

        setSizeFull();

        everythingLayout.addClassName("everythingLayout");
        list.addClassName("chatList");
        upLayout.addClassName("upLayout");
        chatListLayout.addClassName("chatListLayout");
        chatLayout.addClassName("chatLayout");
        chatDiv.addClassName("chatDiv");


        add(everythingLayout);
    }
}
