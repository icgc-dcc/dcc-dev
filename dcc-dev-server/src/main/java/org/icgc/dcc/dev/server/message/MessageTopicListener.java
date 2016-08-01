package org.icgc.dcc.dev.server.message;

import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

import org.icgc.dcc.dev.server.message.Messages.FirstSubscriberMessage;
import org.icgc.dcc.dev.server.message.Messages.LastSubscriberMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens for topic state transitions.
 * <p>
 * Adds higher level events.
 */
@Slf4j
@Component
class MessageTopicListener {

  /**
   * Dependencies.
   */
  @Autowired
  MessageService messages;

  /**
   * State.
   */
  final SetMultimap<String, String> topicSubscriptions = synchronizedSetMultimap(HashMultimap.create());

  @EventListener
  void handle(AbstractSubProtocolEvent event) {
    val headers = StompHeaderAccessor.wrap(event.getMessage());
    val topic = headers.getDestination();
    val sessionId = headers.getSessionId();
    
    val command = headers.getCommand();
    if (command == null) return;

    switch (command) {
    case SUBSCRIBE:
      handleSubscribe(topic, sessionId);
      break;
    case UNSUBSCRIBE:
      handleUnsubscribe(topic, sessionId);
      break;
    case DISCONNECT:
      handleDisconnect(sessionId);
      break;
    default:
      break;
    }
  }

  private void handleSubscribe(String topic, String sessionId) {
    log.info("Session {} subscribed to: {}", sessionId, topic);
    topicSubscriptions.put(topic, sessionId);

    if (!isFirst(topic)) return;
    
    messages.sendMessage(new FirstSubscriberMessage(topic));
  }

  private void handleUnsubscribe(String topic, String sessionId) {
    log.info("Session {} unsubscribed from: {}", sessionId, topic);
    topicSubscriptions.remove(topic, sessionId);

    if (!isLast(topic)) return;
    
    messages.sendMessage(new LastSubscriberMessage(topic));
  }

  private void handleDisconnect(String sessionId) {
    val iterator = topicSubscriptions.entries().iterator();
    while (iterator.hasNext()) {
      val entry = iterator.next();
      val topic = entry.getKey();
      
      val targetSessionId = entry.getValue();
      if (!targetSessionId.equals(sessionId)) continue;
      
      log.info("Session {} disconnected from: {}", sessionId, topic);
      iterator.remove();
      
      if (!isLast(topic)) continue;
      
      messages.sendMessage(new LastSubscriberMessage(topic));
    }
  }

  private boolean isFirst(String topic) {
    return getSessionCount(topic) == 1;
  }

  private boolean isLast(String topic) {
    return getSessionCount(topic) == 0;
  }

  private int getSessionCount(String topic) {
    return topicSubscriptions.get(topic).size();
  }

}