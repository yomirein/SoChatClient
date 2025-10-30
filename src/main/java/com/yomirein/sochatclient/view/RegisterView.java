package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Response;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
public class RegisterView extends FormLayout {
    @Autowired
    public RegisterView(AuthService authService) {
        LoginForm form = new LoginForm();
        form.addLoginListener(e -> {
            try {
                Response.Auth response = authService.register(e.getUsername(), e.getPassword());
                if (response != null && response.getToken() != null) {
                    VaadinSession.getCurrent().setAttribute(User.class, response.getUser());
                    VaadinSession.getCurrent().setAttribute("token", response.getToken());
                    Notification.show("Registered successfully!");
                    getUI().ifPresent(ui -> ui.navigate("chat"));
                } else {
                    Notification.show("Registration failed: " + (response != null ? response.getMessage() : "unknown"));
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });

        Button backButton = new Button("Back to login", event -> getUI().ifPresent(ui -> ui.navigate("login")));

        add(form, backButton);
    }
}