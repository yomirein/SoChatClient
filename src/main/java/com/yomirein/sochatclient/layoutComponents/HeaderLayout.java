package com.yomirein.sochatclient.layoutComponents;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class HeaderLayout extends HorizontalLayout {

    private final Avatar avatar;

    public HeaderLayout(String username) {
        this.avatar = new Avatar(username);
        avatar.addClassName("avatarUp");

        add(avatar);
        addClassName("upLayout");
    }
}