package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Response;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("login")
public class LoginView extends VerticalLayout {
    @Autowired
    public LoginView(AuthService authService) {
        LoginForm form = new LoginForm();

        form.addLoginListener(e -> {
            Response.Auth response = authService.login(e.getUsername(), e.getPassword());
            if (response != null && response.getToken() != null) {
                VaadinSession.getCurrent().setAttribute(User.class, response.getUser());
                VaadinSession.getCurrent().setAttribute("token", response.getToken());
                Notification.show("Login success!");
                getUI().ifPresent(ui -> ui.navigate(ChatView.class));
            } else {
                Notification.show("Invalid credentials");
            }
        });
        add(form, new Button("Register", e -> getUI().ifPresent(ui -> ui.navigate(RegisterView.class))));
    }
}