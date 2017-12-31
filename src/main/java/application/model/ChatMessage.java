package application.model;

public class ChatMessage {

  public enum MessageType {
    JOIN,
    LEAVE,
    FINDING_MATCH,
    FOUND_MATCH,
    CHAT
  }

  private MessageType messageType;
  private String sender;

  private String content;

  public ChatMessage() {}

  public ChatMessage(MessageType type, String sender) {
    this.messageType = type;
    this.sender = sender;
  }

  public MessageType getType() {
    return messageType;
  }

  public void setType(MessageType messageType) {
    this.messageType = messageType;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
