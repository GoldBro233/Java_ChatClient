import java.util.UUID;

public record ChatMessage(UUID clientId, String clientAlias, String direction, Message message) {
}
