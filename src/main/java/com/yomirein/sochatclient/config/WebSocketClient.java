package com.yomirein.sochatclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yomirein.sochatclient.events.EventMessage;
import com.yomirein.sochatclient.model.Chat;
import com.yomirein.sochatclient.model.Message;
import com.yomirein.sochatclient.model.Request;
import com.yomirein.sochatclient.model.Response;
import org.apache.hc.client5.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.awt.print.Pageable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
public class WebSocketClient {


    private final WebSocketStompClient stompClient;
    public StompSession stompSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);
    private final Map<Long, Consumer<Message>> chatHandlers = new HashMap<>();
    private final Map<Long, Consumer<List<Message>>> messageHandlers = new HashMap<>();
    private final String wsUrl = "https://localhost:8443/ws";
    private final Map<Long, Consumer<EventMessage>> chatEventHandlers = new HashMap<>();

    private String token;
    String cookieHeader;

    public WebSocketClient(List<Cookie> cookieList) {
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        SockJsClient sockJsClient = new SockJsClient(transports);

        this.stompClient = new WebSocketStompClient(sockJsClient);

        String tmp = null;
        for (Cookie c : cookieList) {
            if ("AUTH_TOKEN".equals(c.getName())) {
                tmp = c.getValue();
                break;
            }
        }
        this.token = tmp;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter jacksonConverter = new MappingJackson2MessageConverter();
        jacksonConverter.setObjectMapper(mapper);

        this.stompClient.setMessageConverter(jacksonConverter);
    }
    public interface ConnectionCallback {
        void onConnected(StompSession session);
    }

    public CompletableFuture<StompSession> connect() {
        CompletableFuture<StompSession> future = new CompletableFuture<>();

        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        StompHeaders stompHeaders = new StompHeaders();


        if (token != null) {
            String bearer = "Bearer " + token;
            stompHeaders.add("Authorization", bearer);
            httpHeaders.add("Authorization", bearer);
        }

        stompClient.connectAsync(wsUrl, httpHeaders, stompHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Connected to WS successfully");
                stompSession = session;
                future.complete(session);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("STOMP Exception: " + exception.getMessage());
                exception.printStackTrace();
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.err.println("Transport error: " + exception.getMessage());
                future.completeExceptionally(exception);
            }
        });
        System.out.println("ада");

        return future;
    }


    public StompSession.Subscription subscribeToChat(Long chatId, Consumer<Message> handler) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected to WS");
        }

        String topic = "/topic/chat/" + chatId;
        chatHandlers.put(chatId, handler);

        StompHeaders headers = new StompHeaders();
        if (token != null) headers.add("Authorization", "Bearer " + token);
        headers.setDestination(topic);

        StompSession.Subscription subscription = stompSession.subscribe(headers, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload == null) return;
                Message msg = (Message) payload;

                Consumer<Message> h = chatHandlers.get(chatId);
                if (h != null) {
                    h.accept(msg);
                } else {
                    System.out.println("No handler for chat " + chatId);
                }
            }
        });
        System.out.println("Subscribed to " + topic);
        return subscription;
    }

    public StompSession.Subscription subscribeToChatCreation(Consumer<Chat> handler) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected yet");
        }

        String topic = "/topic/create";
        StompHeaders headers = new StompHeaders();
        if (token != null) headers.add("Authorization", "Bearer " + token);
        headers.setDestination(topic);

        return stompSession.subscribe(headers, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Chat.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload == null) return;
                Chat chat = (Chat) payload;
                handler.accept(chat);
            }
        });
    }

    public StompSession.Subscription subscribeChatEvents(Long userId, Consumer<EventMessage> handler) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected yet");
        }

        String topic = "/topic/chats/" + userId;
        chatEventHandlers.put(userId, handler);

        StompHeaders headers = new StompHeaders();
        if (token != null) headers.add("Authorization", "Bearer " + token);
        headers.setDestination(topic);

        StompSession.Subscription subscription = stompSession.subscribe(headers, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return EventMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload == null) return;
                Consumer<EventMessage> h = chatEventHandlers.get(userId);
                EventMessage event = (EventMessage) payload;

                System.out.println("Received event " + event.getClass().getName());

                if (h != null) {
                    h.accept(event);
                }
                    /*
                switch (event.getEventType()) {
                    case "chatMessage":
                        Message msg = (Message) event.getPayload();
                        System.out.println("New chat message: " + msg.getContent());
                        break;
                    case "chatCreate":
                        Chat chat = (Chat) event.getPayload();
                        break;
                */
            }
        });
        System.out.println("Subscribed to " + topic);
        return subscription;
    }


    public void sendMessage(Long chatId, String content) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected yet");
        }

        Message message = new Message();
        message.setChatId(chatId);
        message.setContent(content);

        StompHeaders headers = new StompHeaders();
        if (token != null) headers.add("Authorization", "Bearer " + token);
        headers.setDestination("/app/chat/" + chatId + "/send");


        stompSession.send(headers, message);
    }

    public void createChat(String username){
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected yet");
        }

        StompHeaders headers = new StompHeaders();
        if (token != null) headers.add("Authorization", "Bearer " + token);
        headers.setDestination("/app/create");
        stompSession.send(headers, username);
    }

    public synchronized void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = null;
        chatHandlers.clear();
    }

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}