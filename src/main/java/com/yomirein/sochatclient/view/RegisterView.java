package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
public class RegisterView extends FormLayout {
    @Autowired
    public RegisterView(AuthService authService) {
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        Button register = new Button("Register", e -> {
            if (authService.register(username.getValue(), password.getValue())) {
                Notification.show("Registered!");
            } else {
                Notification.show("Username already exists");
            }
        });
        add(username, password, register);
    }
}