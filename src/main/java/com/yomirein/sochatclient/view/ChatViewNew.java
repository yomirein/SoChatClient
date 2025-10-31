package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.yomirein.sochatclient.layoutComponents.ChatLayout;
import com.yomirein.sochatclient.layoutComponents.ChatListLayout;
import com.yomirein.sochatclient.layoutComponents.HeaderLayout;

@Route("chatnew")
public class ChatViewNew extends VerticalLayout {
    /*

        ▒█▀▀█ ▒█▀▀▀█ ▀▀█▀▀ 　 ▒█▀▀▀█ ▒█░░░ ▒█▀▀▄ 　 ▒█▀▀█ ▒█░▒█ ░█▀▀█ ▀▀█▀▀ 　 ▒█▀▀▀ ▒█▀▀▀█ ▒█▀▀█ 　 ▀▀█▀▀ ▒█░▒█ ▀█▀ ▒█▀▀▀█ 　 　 ▀█▀
        ▒█░▄▄ ▒█░░▒█ ░▒█░░ 　 ▒█░░▒█ ▒█░░░ ▒█░▒█ 　 ▒█░░░ ▒█▀▀█ ▒█▄▄█ ░▒█░░ 　 ▒█▀▀▀ ▒█░░▒█ ▒█▄▄▀ 　 ░▒█░░ ▒█▀▀█ ▒█░ ░▀▀▀▄▄ 　 　 ▒█░
        ▒█▄▄█ ▒█▄▄▄█ ░▒█░░ 　 ▒█▄▄▄█ ▒█▄▄█ ▒█▄▄▀ 　 ▒█▄▄█ ▒█░▒█ ▒█░▒█ ░▒█░░ 　 ▒█░░░ ▒█▄▄▄█ ▒█░▒█ 　 ░▒█░░ ▒█░▒█ ▄█▄ ▒█▄▄▄█ 　 　 ▄█▄

        ▒█▄░▒█ ▒█▀▀▀ ▒█▀▀▀ ▒█▀▀▄ ▒█▀▀▀ ▒█▀▀▄ 　 ▀▀█▀▀ ▒█░▒█ ▀█▀ ▒█▀▀▀█ ░░ 　 ▒█░░▒█ ░█▀▀█ ▒█░░▒█ █
        ▒█▒█▒█ ▒█▀▀▀ ▒█▀▀▀ ▒█░▒█ ▒█▀▀▀ ▒█░▒█ 　 ░▒█░░ ▒█▀▀█ ▒█░ ░▀▀▀▄▄ ▄▄ 　 ▒█▄▄▄█ ▒█▄▄█ ▒█▄▄▄█ ▀
        ▒█░░▀█ ▒█▄▄▄ ▒█▄▄▄ ▒█▄▄▀ ▒█▄▄▄ ▒█▄▄▀ 　 ░▒█░░ ▒█░▒█ ▄█▄ ▒█▄▄▄█ ░█ 　 ░░▒█░░ ▒█░▒█ ░░▒█░░ ▄

    */
    private final ChatLayout chatLayout;
    private final ChatListLayout chatListLayout;
    private final HeaderLayout headerLayout;

    public ChatViewNew() {

        this.chatLayout = new ChatLayout();
        this.chatListLayout = new ChatListLayout();
        this.headerLayout = new HeaderLayout("Placeholder");

        setSizeFull();
        addClassName("main-view");
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);


        HorizontalLayout mainContent = new HorizontalLayout(
                chatListLayout, chatLayout
        );

        mainContent.setSizeFull();
        mainContent.addClassName("everythingLayout");

        add(headerLayout, mainContent);
    }
}
