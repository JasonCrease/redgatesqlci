package redgatesqlci;

import java.util.Optional;

public enum TransactionIsolationLevel {
    Serializable(), Snapshot(), RepeatableRead("Repeatable Read"), ReadCommitted("Read Committed"), ReadUncommitted("Read Uncommitted");

    private final Optional<String> displayName;

    private TransactionIsolationLevel() {
        displayName = Optional.empty();
    }

    private TransactionIsolationLevel(String value) {
        displayName = Optional.ofNullable(value);
    }

    public String getDisplayName() {
        return displayName.orElse(name());
    }
}