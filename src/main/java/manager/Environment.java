package manager;

import java.util.Objects;
import java.util.Properties;

public enum Environment {
    Instance(
        new Properties() {
            {
                this.setProperty("env", "");
            }
        }
    );

    public final Properties properties;

    Environment(Properties properties) {
        this.properties = Objects.requireNonNull( properties);
    }
}
