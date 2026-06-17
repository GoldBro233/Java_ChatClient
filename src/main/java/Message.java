import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public record Message(String type, String content) {

    private static final Gson GSON = new Gson();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Message fromJson(String json) {
        try {
            Message message = GSON.fromJson(json, Message.class);
            if (message == null || isBlank(message.type()) || isBlank(message.content())) {
                throw new IllegalArgumentException("Message must include non-empty type and content");
            }
            return message;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid message JSON", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
