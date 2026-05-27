import com.google.gson.Gson;

public record Message(String type, String content) {

    private static final Gson GSON = new Gson();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }
}
