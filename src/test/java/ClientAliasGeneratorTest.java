import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientAliasGeneratorTest {

    @Test
    void generatesDifferentEnglishAliasesFromNamePool() {
        ClientAliasGenerator generator = new ClientAliasGenerator();

        String first = generator.nextAlias();
        String second = generator.nextAlias();

        assertFalse(first.isBlank());
        assertFalse(second.isBlank());
        assertNotEquals(first, second);
        assertTrue(ClientAliasGenerator.isKnownName(first));
        assertTrue(ClientAliasGenerator.isKnownName(second));
    }
}
