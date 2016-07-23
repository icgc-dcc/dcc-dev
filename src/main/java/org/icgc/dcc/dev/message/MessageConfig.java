package org.icgc.dcc.dev.message;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
public class MessageConfig extends AbstractWebSocketMessageBrokerConfigurer {

  /**
   * Configuration.
   */
  @Value("${message.topicPrefix}")
  String topicPrefix;
  
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker(topicPrefix);
    config.setApplicationDestinationPrefixes("/service");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Websocket endpoint: ws://localhost:8080/messages
    registry.addEndpoint("/messages").withSockJS();
  }

}
