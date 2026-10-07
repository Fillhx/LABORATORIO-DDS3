package co.edu.univalle.ygo.api;

/**
 * Error al consultar la API YGOProDeck.
 * El mensaje ya viene listo para mostrarse al usuario.
 */
public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
