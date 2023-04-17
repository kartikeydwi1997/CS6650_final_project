public interface MessageCallback {
  void onError(Exception e);

  void onSuccess( String clientID,String messageContent);
}