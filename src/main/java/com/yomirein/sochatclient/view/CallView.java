package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;


public class CallView extends HorizontalLayout {
    public CallView() {
        addClassName("voice-panel");

        List<CallUser> list = new ArrayList<>();
        Button toggleMic = new Button("Toggle Mic");
        Button leaveCall = new Button("Leave Call");
        Button joinCall = new Button("Join Call");
        Div users = new Div();
        users.setClassName("voice-users");
        users.setWidthFull();

        for (CallUser callUser : list) {
            Div block = new Div();
            block.setClassName("voice-user-block");
            block.add(callUser.getAvatar());
            users.add(block);
        }

        list.add(new CallUser("1username", 1L));
        list.add(new CallUser("2username2", 2L));

        for (CallUser callUser : list){
            users.add(callUser.getAvatar());
        }
        add(users, toggleMic, leaveCall, joinCall);
    }

    @Getter
    @Setter
    private static class CallUser{
        Avatar avatar;
        String userName;
        Long userId;

        public CallUser(String userName_, Long userId_) {
            userName = userName_;
            userId = userId_;
            avatar = new Avatar(userName_);

            char firstChar = avatar.getName().charAt(0);
            int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;

            avatar.setColorIndex(colorIndex);

        }
    }
}
