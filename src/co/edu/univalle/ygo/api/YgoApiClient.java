package co.edu.univalle.ygo.api;

import co.edu.univalle.ygo.model.Card;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente de la API YGOProDeck: pide cartas al azar y descarga sus imágenes.
 * <p>
 * Todos los métodos son bloqueantes (hacen peticiones HTTP), así que se deben
 * llamar desde un hilo de fondo y nunca desde el hilo de eventos de Swing.
 */
public class YgoApiClient {

    public static final String RANDOM_CARD_URL = "https://db.ygoprodeck.com/api/v7/randomcard.php";

    /** Intentos máximos para conseguir una carta Monster (la API también devuelve Spell/Trap). */
    private static final int MAX_ATTEMPTS = 15;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    // randomcard.php responde con un 301 hacia cardinfo.php, y HttpClient no sigue redirects por defecto.
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Pide cartas al azar hasta obtener una de tipo Monster.
     *
     * @throws ApiException si hay un error de red o no aparece ningún Monster tras {@link #MAX_ATTEMPTS} intentos
     */
    public Card fetchRandomMonster() throws ApiException {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Card card = parseCard(send(RANDOM_CARD_URL, HttpResponse.BodyHandlers.ofString()));
            if (card != null) {
                return card;
            }
        }
        throw new ApiException("No se pudo cargar la carta: no se obtuvo un Monster tras "
                + MAX_ATTEMPTS + " intentos.");
    }

    /** Descarga y decodifica la imagen de una carta. */
    public BufferedImage fetchImage(String url) throws ApiException {
        if (url == null || url.isEmpty()) {
            throw new ApiException("La carta no tiene imagen.");
        }
        byte[] bytes = send(url, HttpResponse.BodyHandlers.ofByteArray());
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ApiException("No se pudo leer la imagen de la carta.");
            }
            return image;
        } catch (IOException e) {
            throw new ApiException("No se pudo leer la imagen de la carta.", e);
        }
    }

    /**
     * Convierte la respuesta JSON en una {@link Card}.
     *
     * @return la carta, o null si no es un Monster (el llamador debe volver a pedir)
     */
    static Card parseCard(String body) throws ApiException {
        try {
            JSONObject root = new JSONObject(body);
            // La API actual envuelve la carta en {"data":[{...}]}; versiones anteriores la enviaban suelta.
            JSONObject data = root.has("data") ? root.getJSONArray("data").getJSONObject(0) : root;

            String type = data.optString("type", "");
            if (!type.contains("Monster")) {
                return null;
            }

            String imageUrl = null;
            JSONArray images = data.optJSONArray("card_images");
            if (images != null && !images.isEmpty()) {
                JSONObject image = images.getJSONObject(0);
                imageUrl = image.optString("image_url_small", image.optString("image_url", null));
            }

            // Los Link Monster no traen "def", y ATK/DEF "?" pueden llegar negativos o ausentes.
            int atk = Math.max(0, data.optInt("atk", 0));
            int def = Math.max(0, data.optInt("def", 0));
            return new Card(data.getString("name"), type, atk, def, imageUrl);
        } catch (JSONException e) {
            throw new ApiException("No se pudo cargar la carta: respuesta inválida de la API.", e);
        }
    }

    /** Hace un GET y traduce los fallos a mensajes entendibles para el usuario. */
    private <T> T send(String url, HttpResponse.BodyHandler<T> handler) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "YgoDuelLite/1.0")
                .GET()
                .build();
        try {
            HttpResponse<T> response = http.send(request, handler);
            if (response.statusCode() != 200) {
                throw new ApiException("No se pudo cargar la carta (HTTP " + response.statusCode() + ").");
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException("Error de red: no se pudo conectar con YGOProDeck.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("La carga de cartas fue cancelada.", e);
        }
    }
}
