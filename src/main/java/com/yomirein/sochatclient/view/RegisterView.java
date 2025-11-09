package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.yomirein.sochatclient.model.Response;
import com.yomirein.sochatclient.model.User;
import com.yomirein.sochatclient.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
public class RegisterView extends VerticalLayout {
    @Autowired
    public RegisterView(AuthService authService) {
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Register");
        i18nForm.setSubmit("Register");
        i18n.setForm(i18nForm);
        LoginForm form = new LoginForm();

        form.setI18n(i18n);
        form.addLoginListener(e -> {
            try {
                Response.Auth response = authService.register(e.getUsername(), e.getPassword());

                if (response != null && response.getUser() != null) {
                    Notification.show("Register successful!");
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

        Button registerButton = new Button("Login", e ->
                getUI().ifPresent(ui -> ui.navigate(LoginView.class))
        );

        add(form, registerButton);
    }
}