package za.co.taloms.common;

public class DuplicateRecordException extends RuntimeException {

    public DuplicateRecordException(String message) {
        super(message);
    }

    public DuplicateRecordException(String entity, String field, String value) {
        super(entity + " already exists with " + field + ": " + value);
    }
}