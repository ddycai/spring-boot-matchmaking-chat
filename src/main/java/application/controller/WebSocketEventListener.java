package application.controller;

import application.model.ChatMessage;
import application.model.ChatMessage.MessageType;
import application.model.Client;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

  @Inject private MatchmakingContext matchmakingContext;
  @Inject private SimpMessageSendingOperations messenger;

  @EventListener
  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    logger.info("Received a new web socket connection.");
  }

  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

    String userId = headerAccessor.getUser().getName();
    String username = (String) headerAccessor.getSessionAttributes().get("username");
    if (username != null) {
      logger.info("User Disconnected: " + username);

      ChatMessage leaveMessage = new ChatMessage(MessageType.LEAVE, username);
      Client recipient = matchmakingContext.getRecipientOf(userId);
      if (recipient != null) {
        messenger.convertAndSendToUser(recipient.getId(), "/match", leaveMessage);
      }
      matchmakingContext.remove(Client.create(userId, username));
    }
  }
}
