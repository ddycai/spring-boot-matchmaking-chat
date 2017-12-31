package application.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Client {

  public static Client create(String id, String username) {
    return new AutoValue_Client(id, username);
  }

  public abstract String getId();

  public abstract String getUsername();
}

