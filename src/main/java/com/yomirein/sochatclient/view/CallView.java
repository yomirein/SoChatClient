package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.User;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

@JsModule("./webrtc.js")
public class CallView extends HorizontalLayout {


    public CallView(User sender, User receiver) {
        addClassName("voice-panel");

        CookieStore cookieStore;
        String token = null;

        VaadinSession vaadinSession = VaadinSession.getCurrent();
        vaadinSession.lock();
        try {
            cookieStore = (CookieStore) vaadinSession.getAttribute("cookieStore");
            if (cookieStore.getCookies() != null) {
                for (Cookie c : cookieStore.getCookies()) {
                    if ("AUTH_TOKEN".equals(c.getName())) {
                        System.out.println("AUTH_TOKEN");
                        System.out.println(c.getValue());
                        token = c.getValue();
                        System.out.println("[GOT TOKEN]" + token);
                        UI.getCurrent().getPage().executeJs("window.authToken = $0;", token);
                    }
                }
            }
        } catch (Exception e)  {
            getUI().ifPresent(ui -> Notification.show(e.getMessage()));
            getUI().ifPresent(ui -> ui.access(() -> ui.navigate(LoginView.class)));
        }
        finally {
            vaadinSession.unlock();
        }

        if (token != null){
            System.out.println("[GOT TOKEN]" + token);
            List<CallUser> list = new ArrayList<>();
            VerticalLayout root = new VerticalLayout();
            root.setSizeFull();
            root.setPadding(false);
            root.setSpacing(false);
            root.addClassName("call-root");

            Div users = new Div();
            users.addClassName("voice-user-list");
            users.setSizeFull();

            list.add(new CallUser(sender.getUsername(), sender.getId(), true));
            list.add(new CallUser(receiver.getUsername(), receiver.getId(), false));
            Button toggleVideo = new Button("Toggle Video", e -> {
                UI.getCurrent().getPage().executeJs("window.toggleVideo()");
            });
            Button toggleMic = new Button("Toggle Mic", e -> {
                UI.getCurrent().getPage().executeJs("window.toggleMic()");
            });
            Button leaveCall = new Button("Leave Call", e -> {
                UI.getCurrent().getPage().executeJs("window.endCall()");
            });
            Button joinCall = new Button("Join Call", e -> {
                String targetUserId = receiver.getId().toString();
                UI.getCurrent().getPage().executeJs("window.acceptIncomingCall()");
            });
            Button callButton = new Button("Call", e -> {
                String targetUserId = receiver.getId().toString();
                UI.getCurrent().getPage().executeJs("window.startCallTo($0)", targetUserId);
            });
            Button leaveButton = new Button("Leave Call", e -> {
                UI.getCurrent().getPage().executeJs("window.endCall()");
            });



            Div status = new Div();
            status.setId("connectionStatus");
            status.setText("Соединение не установлено.");
            status.getStyle().set("font-weight", "bold");
            add(status);

            for (CallUser callUser : list) {
                users.add(callUser.getAvatarContainer());
            }



            HorizontalLayout controls = new HorizontalLayout(callButton, leaveButton, toggleVideo, toggleMic, leaveCall, joinCall);
            controls.setMaxWidth("80%");
            controls.addClassName("call-controls");
            root.add(users, controls);

            root.setFlexGrow(1, users);
            root.setFlexGrow(2, controls);

            add(root);
            String myUserId = sender.getId().toString();
            UI.getCurrent().getPage().executeJs("window.__MY_USER_ID = $0", myUserId);

            // подключаем STOMP сразу
            UI.getCurrent().getPage().executeJs("ensureStomp()");
        }
    }

    @Getter
    @Setter
    private static class CallUser{
        Avatar avatar;
        String userName;
        Long userId;
        VerticalLayout avatarContainer = new VerticalLayout();

        public CallUser(String userName_, Long userId_, boolean local) {
            userName = userName_;
            userId = userId_;
            avatar = new Avatar(userName_);

            if (local){
                avatarContainer.setId("localVideo");
            }
            else {
                avatarContainer.setId("remoteVideo");
            }

            //avatarContainer.addClassName("voice-user-block");
            avatarContainer.setWidthFull(); // теперь это нормально — grid сам ограничит
            avatarContainer.setHeight(null);
            avatarContainer.add(avatar);

            char firstChar = avatar.getName().charAt(0);
            int colorIndex = (Character.toLowerCase(firstChar) - 'a') % 10;
            avatar.setColorIndex(colorIndex);

        }
    }
}
