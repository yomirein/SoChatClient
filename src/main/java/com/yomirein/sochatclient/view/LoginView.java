package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("")
public class LoginView extends VerticalLayout {
    @Autowired
    public LoginView(AuthService authService) {
        LoginForm form = new LoginForm();
        form.addLoginListener(e -> {
            if (authService.login(e.getUsername(), e.getPassword())) {
                Notification.show("Login success!");
                getUI().ifPresent(ui -> ui.navigate(ChatView.class));
            } else {
                Notification.show("Invalid credentials");
            }
        });
        add(form, new Button("Register", e -> getUI().ifPresent(ui -> ui.navigate(RegisterView.class))));
    }
}