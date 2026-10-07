package co.edu.univalle.ygo.model;

/**
 * Carta Monster obtenida de la API YGOProDeck.
 * Es inmutable: solo guarda los datos que el duelo y la vista necesitan.
 */
public final class Card {

    private final String name;
    private final String type;
    private final int atk;
    private final int def;
    private final String imageUrl;

    public Card(String name, String type, int atk, int def, String imageUrl) {
        this.name = name;
        this.type = type;
        this.atk = atk;
        this.def = def;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    /** Tipo tal como lo entrega la API, p. ej. "Effect Monster" o "Link Monster". */
    public String getType() {
        return type;
    }

    public int getAtk() {
        return atk;
    }

    /** Los Link Monster no tienen DEF; en ese caso vale 0. */
    public int getDef() {
        return def;
    }

    /** URL de la imagen oficial; puede ser null si la API no la trae. */
    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return name + " (ATK " + atk + " / DEF " + def + ")";
    }
}
