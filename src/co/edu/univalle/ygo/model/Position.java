package co.edu.univalle.ygo.model;

/** Posición de batalla de una carta durante una ronda. */
public enum Position {
    ATTACK("ataque"),
    DEFENSE("defensa");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    /** Nombre en español para mostrar en el log. */
    public String getLabel() {
        return label;
    }
}
