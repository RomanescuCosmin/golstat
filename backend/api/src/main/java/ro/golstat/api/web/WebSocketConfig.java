package ro.golstat.api.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket (STOMP) pentru push-ul LIVE. Frontend-ul se conecteaza la {@code /ws} si se aboneaza la
 * {@code /topic/live/{fixtureId}} ca sa primeasca scor/status in timp real (vezi {@link LiveBroadcaster}).
 * Broker simplu in-memory — suficient pentru un singur nod; la scalare se pune un broker dedicat.
 *
 * <p>CSRF e ignorat pe {@code /ws/**} (SockJS), deci verificarea de origine e cea care opreste
 * deturnarea cross-site: aceleasi origini ca la CORS.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;

    public WebSocketConfig(@Value("${golstat.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins).withSockJS();
    }
}
