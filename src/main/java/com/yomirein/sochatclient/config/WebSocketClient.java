package com.yomirein.sochatclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yomirein.sochatclient.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;



@Component
public class WebSocketClient {


    private final WebSocketStompClient stompClient;
    public StompSession stompSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);
    private final Map<Long, Consumer<Message>> chatHandlers = new HashMap<>();
    private final String wsUrl = "http://localhost:8080/ws";

    public WebSocketClient() {
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport()
        );
        SockJsClient sockJsClient = new SockJsClient(transports);

        this.stompClient = new WebSocketStompClient(sockJsClient);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter jacksonConverter = new MappingJackson2MessageConverter();
        jacksonConverter.setObjectMapper(mapper);

        this.stompClient.setMessageConverter(jacksonConverter);
    }
    public interface ConnectionCallback {
        void onConnected(StompSession session);
    }

    public CompletableFuture<StompSession> connect(String token) {
        CompletableFuture<StompSession> future = new CompletableFuture<>();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        System.out.println("Connecting with token: " + token.substring(0, Math.min(10, token.length())) + "...");

        stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
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


    public StompSession.Subscription subscribeToChat(Long chatId, String token, Consumer<Message> handler) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected to WS");
        }

        String topic = "/topic/chat/" + chatId;
        chatHandlers.put(chatId, handler);

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + token);
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

    public void sendMessage(Long chatId, String content, String token) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected yet");
        }

        Message message = new Message();
        message.setChatId(chatId);
        message.setContent(content);
        //message.setTimestamp(LocalDateTime.now());
        //message.setId(0L);
        //message.setSenderId(0L);


        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/chat/" + chatId + "/send");
        headers.add("Authorization", "Bearer " + token);


        stompSession.send(headers, message);
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