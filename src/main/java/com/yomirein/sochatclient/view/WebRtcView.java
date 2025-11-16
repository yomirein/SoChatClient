package com.yomirein.sochatclient.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@JsModule("./webrtc.js")
@Route("WebRtcTest")
public class WebRtcView extends VerticalLayout {

    public WebRtcView() {
        setWidthFull();
        setHeightFull();

        Div local = new Div();
        local.setId("localVideo");
        local.setWidth("400px");
        local.setHeight("300px");

        Div remote = new Div();
        remote.setId("remoteVideo");
        remote.setWidth("400px");
        remote.setHeight("300px");

        Button call = new Button("Start call", e ->
                getElement().executeJs("startCall()")
        );

        add(call, local, remote);
    }

    @JsModule("./webrtc.js")
    // или @JavaScript("frontend://webrtc.js") для Vaadin 14
    private static class Scripts {}
}

