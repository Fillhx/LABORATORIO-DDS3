package co.edu.univalle.ygo.logic;

/**
 * Eventos que {@link Duel} emite para que la interfaz reaccione sin que la
 * lógica conozca nada de Swing.
 * <p>
 * Los tres primeros métodos son los exigidos por el enunciado; los métodos
 * {@code default} son extras opcionales para enriquecer la vista.
 */
public interface BattleListener {

    /**
     * Se resolvió una ronda.
     *
     * @param winner {@link Duel#PLAYER}, {@link Duel#AI} o {@link Duel#DRAW}
     */
    void onTurn(String playerCard, String aiCard, String winner);

    void onScoreChanged(int playerScore, int aiScore);

    /** @param winner {@link Duel#PLAYER}, {@link Duel#AI} o {@link Duel#DRAW} */
    void onDuelEnded(String winner);

    /** Comienza una ronda; indica si en ella ataca el jugador o la máquina. */
    default void onRoundStarted(int round, boolean playerAttacks) {
    }

    /**
     * Ambas cartas quedaron en juego.
     *
     * @param detail descripción legible de la comparación (quién ataca, valores enfrentados)
     */
    default void onCardsPlayed(int playerIndex, int aiIndex, String detail) {
    }
}
