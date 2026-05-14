package decode;

public class DwgCrcMismatchException extends Exception {
    public DwgCrcMismatchException() {
        super();
    }
    
    public DwgCrcMismatchException(String message) {
        super(message);
    }
}
