import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientAliasGenerator {

    private static final List<String> NAMES = List.of(
            "Alice", "Bob", "Charlie", "Diana", "Ethan", "Fiona", "George", "Hannah",
            "Isaac", "Julia", "Kevin", "Laura", "Mason", "Nora", "Oscar", "Paula",
            "Quentin", "Rachel", "Samuel", "Tina", "Victor", "Wendy", "Xavier", "Yvonne");

    private final Random random;
    private final Set<String> assignedAliases = ConcurrentHashMap.newKeySet();

    public ClientAliasGenerator() {
        this(new Random());
    }

    ClientAliasGenerator(Random random) {
        this.random = random;
    }

    public String nextAlias() {
        while (assignedAliases.size() < NAMES.size()) {
            String name = NAMES.get(random.nextInt(NAMES.size()));
            if (assignedAliases.add(name)) {
                return name;
            }
        }

        while (true) {
            String name = NAMES.get(random.nextInt(NAMES.size())) + "-" + (assignedAliases.size() + 1);
            if (assignedAliases.add(name)) {
                return name;
            }
        }
    }

    public static boolean isKnownName(String alias) {
        String baseName = alias.replaceFirst("-\\d+$", "");
        return NAMES.contains(baseName);
    }
}
