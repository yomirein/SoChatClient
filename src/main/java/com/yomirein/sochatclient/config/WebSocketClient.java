package com.yomirein.sochatclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomirein.sochatclient.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;



@Component
public class WebSocketClient {

    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);
    // Хранилище обработчиков для разных чатов
    private final Map<Long, Consumer<Message>> chatHandlers = new HashMap<>();

    // Адрес WS сервера (без query). При подключении добавим ?token=...
    private final String wsUrl = "http://localhost:8080/ws";

    public WebSocketClient() {
        // SockJS + транспорт
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport() // fallback
        );
        SockJsClient sockJsClient = new SockJsClient(transports);
        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    /**
     * Подключаемся к серверу, передаём token как query param и в CONNECT header.
     * @param token JWT токен
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public synchronized void connect(String token) throws ExecutionException, InterruptedException {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        // Подключаемся по URL с query param token — серверный HandshakeInterceptor может читать его
        String connectUrl = wsUrl + "?token=" + token;

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);


        CompletableFuture<StompSession> f = stompClient.connectAsync(connectUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {

                log.info("STOMP connected, sessionId={}", session.getSessionId());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.warn("Transport error: {}", exception.getMessage());
            }
        }, connectHeaders);

        this.stompSession = f.get();
    }

    /**
     * Подписываемся на /topic/chat/{chatId}.
     * Если ещё не подключены — бросает исключение.
     */
    public synchronized void subscribeToChat(Long chatId, Consumer<Message> handler) {
        if (stompSession == null || !stompSession.isConnected()) {
            throw new IllegalStateException("Not connected to WS");
        }
        String topic = "/topic/chat/" + chatId;
        // отписываемся от старой подписки для этого чата, если есть
        chatHandlers.put(chatId, handler);

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Message.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, @Nullable Object payload) {
                if (payload == null) return;
                Message msg = (Message) payload;
                Consumer<Message> h = chatHandlers.get(chatId);
                if (h != null) {
                    h.accept(msg);
                }
            }
        });
    }

    /**
     * Отправить сообщение на endpoint /app/chat/{chatId}/send
     */
    public void sendMessage(Long chatId, String content) {
        if (stompSession == null) throw new IllegalStateException("Not connected yet");

        Message message = new Message();
        message.setChatId(chatId);
        message.setContent(content);

        stompSession.send("/app/chat/" + chatId + "/send", message);
    }

    public synchronized void disconnect() {
        try {
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
            }
        } finally {
            stompSession = null;
            chatHandlers.clear();
        }
    }

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}