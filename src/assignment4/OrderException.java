package assignment4;

/** A checkout or cancellation failure, with the original reason preserved as its cause. */
public class OrderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
