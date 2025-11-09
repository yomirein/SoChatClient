package com.yomirein.sochatclient.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@Push
@PWA(name = "SoChat", shortName = "SoChat")
@Theme("my-theme")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
public class SoChatAppShell implements AppShellConfigurator {
}