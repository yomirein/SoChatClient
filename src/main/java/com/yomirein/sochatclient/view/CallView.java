package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;


public class CallView extends HorizontalLayout {
    public CallView() {
        addClassName("voice-panel");

        List<CallUser> list = new ArrayList<>();
        Button toggleVideo = new Button("Toggle Video");
        Button toggleMic = new Button("Toggle Mic");
        Button leaveCall = new Button("Leave Call");
        Button joinCall = new Button("Join Call");

        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        root.setPadding(false);
        root.setSpacing(false);
        root.addClassName("call-root");

        Div users = new Div();
        users.addClassName("voice-user-list");
        users.setSizeFull();

        for (CallUser callUser : list) {
            users.add(callUser.getAvatarContainer());
        }

        list.add(new CallUser("1username", 1L));
        list.add(new CallUser("2username2", 2L));

        for (CallUser callUser : list) {
            users.add(callUser.getAvatarContainer());
        }
        HorizontalLayout controls = new HorizontalLayout(toggleVideo, toggleMic, leaveCall, joinCall);
        controls.setMaxWidth("80%");
        controls.addClassName("call-controls");
        root.add(users, controls);

        root.setFlexGrow(1, users);
        root.setFlexGrow(2, controls);

        add(root);
    }

    @Getter
    @Setter
    private static class CallUser{
        Avatar avatar;
        String userName;
        Long userId;
        VerticalLayout avatarContainer = new VerticalLayout();

        public CallUser(String userName_, Long userId_) {
            userName = userName_;
            userId = userId_;
            avatar = new Avatar(userName_);

            avatarContainer.addClassName("voice-user-block");
            avatarContainer.setWidthFull(); // теперь это нормально — grid сам ограничит
            avatarContainer.setHeight(null);
            avatarContainer.add(avatar);

            char firstChar = avatar.getName().charAt(0);
            int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;
            avatar.setColorIndex(colorIndex);

        }
    }
}
