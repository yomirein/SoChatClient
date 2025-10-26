package com.yomirein.sochatclient;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Component
@Route("register")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    private final PasswordEncoder passwordEncoder;
    private final InMemoryUserDetailsManager userDetailsManager;

    public RegisterView(PasswordEncoder passwordEncoder, InMemoryUserDetailsManager userDetailsManager) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsManager = userDetailsManager;

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        Button registerButton = new Button("Register", e -> {
            if (userDetailsManager.userExists(username.getValue())) {
                Notification.show("User already exists");
                return;
            }
            userDetailsManager.createUser(
                    org.springframework.security.core.userdetails.User
                            .withUsername(username.getValue())
                            .password(passwordEncoder.encode(password.getValue()))
                            .roles("USER")
                            .build()
            );
            Notification.show("User registered! Please login.");
        });

        add(username, password, registerButton);
    }
}