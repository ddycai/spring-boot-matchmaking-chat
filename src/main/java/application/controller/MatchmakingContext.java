package application.controller;

import application.model.Client;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Service;

/** Threadsafe context that keeps track of sender/recipient pairs and waiting clients. */
@Service
public class MatchmakingContext {

  /** Maps sender user ID to recipient client object. */
  private final Map<String, Client> matches = new ConcurrentHashMap<>();

  private final Deque<Client> clientsWaiting = new ConcurrentLinkedDeque<>();

  public void enqueueForMatchmaking(Client client) {
    clientsWaiting.addLast(client);
  }

  public boolean isWaiting(Client client) {
    return clientsWaiting.contains(client);
  }

  public Optional<Client> nextAvailableClient() {
    return Optional.ofNullable(clientsWaiting.pollFirst());
  }

  public void match(Client sender, Client recipient) {
    matches.put(sender.getId(), recipient);
    matches.put(recipient.getId(), sender);
  }

  /**
   * Removes the given user ID from list of clients waiting. If user is paired with another client,
   * removes both clients from list of matches.
   */
  public void remove(Client user) {
    Client recipient = matches.get(user.getId());
    if (recipient != null) {
      matches.remove(recipient.getId());
    }
    matches.remove(user.getId());
    clientsWaiting.removeFirstOccurrence(user);
  }

  public Client getRecipientOf(String senderId) {
    return matches.get(senderId);
  }
}
