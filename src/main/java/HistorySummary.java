import java.time.Instant;
import java.util.UUID;

public record HistorySummary(UUID clientId, String clientAlias, int messageCount, Instant lastMessageAt) {
}
