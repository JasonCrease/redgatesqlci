package redgatesqlci;

// Enum names must match the accepted input in SQL Change Automation as we use .name()
public enum TransactionIsolationLevel {
    Serializable("Serializable"),
    Snapshot("Snapshot"),
    RepeatableRead("Repeatable Read"),
    ReadCommitted("Read Committed"),
    ReadUncommitted("Read Uncommitted");

    private final String displayName;

    TransactionIsolationLevel(String value) {
        displayName = value;
    }

    public String getDisplayName() {
        return displayName;
    }
}