package application.controller;

import application.model.ChatMessage;
import application.model.ChatMessage.MessageType;
import application.model.Client;
import java.security.Principal;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

  /**
   * STOMP automatically handles "/user"-prefixed destinations for user-specific messages. Sending a
   * message via {@link SimpMessageSendingOperations#convertAndSendToUser} for "/someTopic"
   * corresponds to "/user/someTopic" on client side.
   */
  private static final String DESTINATION = "/match";

  @Inject private MatchmakingContext matchmakingContext;

  @Inject private SimpMessageSendingOperations messenger;

  /**
   * Adds a user to the matchmaking queue. If a match is found, sends a message back to both clients
   * to start communicating.
   */
  @MessageMapping("/addUser")
  public void addUser(
      @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor, Principal user) {
    Client sender = Client.create(user.getName() /* user id */, chatMessage.getSender());
    if (matchmakingContext.getRecipientOf(sender.getId()) != null
        || matchmakingContext.isWaiting(sender)) {
      return;
    }

    headerAccessor.getSessionAttributes().put("username", sender.getUsername());
    messenger.convertAndSendToUser(
        sender.getId(), DESTINATION, new ChatMessage(MessageType.FINDING_MATCH, ""));

    Optional<Client> maybeRecipient = matchmakingContext.nextAvailableClient();
    if (maybeRecipient.isPresent()) {
      Client recipient = maybeRecipient.get();
      logger.info("Pairing {} with {}", sender.getUsername(), recipient.getUsername());

      ChatMessage recipientMessage =
          new ChatMessage(MessageType.FOUND_MATCH, chatMessage.getSender());
      messenger.convertAndSendToUser(recipient.getId(), DESTINATION, recipientMessage);

      ChatMessage senderMessage = new ChatMessage(MessageType.FOUND_MATCH, recipient.getUsername());
      messenger.convertAndSendToUser(sender.getId(), DESTINATION, senderMessage);

      matchmakingContext.match(sender, recipient);
    } else {
      logger.info("Adding client {} to queue", sender.getUsername());
      matchmakingContext.enqueueForMatchmaking(sender);
    }
  }

  @MessageMapping("/sendMessage")
  @SendToUser(DESTINATION)
  public ChatMessage sendMessage(@Payload ChatMessage chatMessage, Principal user) {
    String senderId = user.getName();
    Client recipient = matchmakingContext.getRecipientOf(senderId);
    if (recipient != null) {
      messenger.convertAndSendToUser(recipient.getId(), DESTINATION, chatMessage);
    }

    // Also send the message back to the user.
    return chatMessage;
  }
}
