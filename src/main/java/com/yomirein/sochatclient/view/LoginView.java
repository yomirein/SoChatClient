package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Response;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("login")
public class LoginView extends VerticalLayout {

    @Autowired
    public LoginView(AuthService authService) {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Login");
        i18nForm.setSubmit("Login");
        i18n.setForm(i18nForm);

        LoginForm form = new LoginForm();

        form.setI18n(i18n);

        form.addLoginListener(e -> {
            try {
                Response.Auth response = authService.login(e.getUsername(), e.getPassword());

                if (response != null && response.getUser() != null) {
                    Notification.show("Login successful!");
                    VaadinSession.getCurrent().setAttribute(User.class, response.getUser());
                    getUI().ifPresent(ui -> ui.navigate(ChatView.class));
                } else {
                    Notification.show("Invalid credentials");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Login failed: " + ex.getMessage());
            }
        });

        Button registerButton = new Button("Register", e ->
                getUI().ifPresent(ui -> ui.navigate(RegisterView.class))
        );

        add(form, registerButton);
    }
}