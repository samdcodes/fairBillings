package fairBilling;

import java.time.LocalTime;

public record LogEntry (
        LocalTime timestamp,
        Action action
) {}
