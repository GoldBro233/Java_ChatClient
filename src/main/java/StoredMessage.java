import java.time.Instant;
import java.util.UUID;

public record StoredMessage(
        long id,
        UUID clientId,
        String clientAlias,
        String direction,
        String type,
        String content,
        Instant createdAt) {
}
