package co.edu.univalle.ygo.logic;

import co.edu.univalle.ygo.model.Card;
import co.edu.univalle.ygo.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reglas del duelo simplificado entre el jugador y la máquina.
 * <p>
 * Reglas adoptadas (el enunciado deja estos puntos abiertos):
 * <ul>
 *   <li>Un sorteo decide quién ataca en la primera ronda; luego el atacante se alterna.</li>
 *   <li>La carta del atacante va en ataque y la del rival en defensa (ATK contra DEF).</li>
 *   <li>Cada carta se usa una sola vez, así que hay como máximo 3 rondas.</li>
 *   <li>Empate en la comparación: nadie suma punto.</li>
 *   <li>Gana quien llegue a 2 puntos. Si se acaban las cartas antes, gana quien tenga
 *       más puntos; con igual puntaje el duelo queda en empate.</li>
 * </ul>
 * No usa Swing: comunica todo a través de {@link BattleListener}.
 */
public class Duel {

    public static final String PLAYER = "Jugador";
    public static final String AI = "Máquina";
    public static final String DRAW = "Empate";

    public static final int HAND_SIZE = 3;
    private static final int POINTS_TO_WIN = 2;

    private final List<Card> playerHand;
    private final List<Card> aiHand;
    private final boolean[] playerUsed = new boolean[HAND_SIZE];
    private final boolean[] aiUsed = new boolean[HAND_SIZE];
    private final Random random;
    private final List<BattleListener> listeners = new ArrayList<>();

    private int playerScore;
    private int aiScore;
    private int round;
    private boolean playerAttacks;
    private boolean started;
    private boolean finished;

    public Duel(List<Card> playerHand, List<Card> aiHand) {
        this(playerHand, aiHand, new Random());
    }

    /** Permite inyectar el {@link Random} para pruebas reproducibles. */
    public Duel(List<Card> playerHand, List<Card> aiHand, Random random) {
        if (playerHand.size() != HAND_SIZE || aiHand.size() != HAND_SIZE) {
            throw new IllegalArgumentException("Cada jugador debe tener " + HAND_SIZE + " cartas.");
        }
        this.playerHand = List.copyOf(playerHand);
        this.aiHand = List.copyOf(aiHand);
        this.random = random;
    }

    public void addBattleListener(BattleListener listener) {
        listeners.add(listener);
    }

    /** Sortea quién ataca primero y abre la ronda 1. */
    public void start() {
        if (started) {
            throw new IllegalStateException("El duelo ya comenzó.");
        }
        started = true;
        round = 1;
        playerAttacks = random.nextBoolean();
        for (BattleListener l : listeners) {
            l.onRoundStarted(round, playerAttacks);
        }
    }

    /**
     * Juega una ronda con la carta elegida por el jugador; la máquina elige al azar
     * entre las que le quedan.
     *
     * @param playerIndex posición (0..2) de una carta del jugador aún no usada
     */
    public void playRound(int playerIndex) {
        if (!started || finished) {
            throw new IllegalStateException("No hay una ronda en curso.");
        }
        if (playerIndex < 0 || playerIndex >= HAND_SIZE || playerUsed[playerIndex]) {
            throw new IllegalArgumentException("Carta no disponible: " + playerIndex);
        }

        int aiIndex = pickRandomAvailable(aiUsed);
        playerUsed[playerIndex] = true;
        aiUsed[aiIndex] = true;
        Card playerCard = playerHand.get(playerIndex);
        Card aiCard = aiHand.get(aiIndex);

        Card attacker = playerAttacks ? playerCard : aiCard;
        Card defender = playerAttacks ? aiCard : playerCard;
        int result = compare(attacker, Position.ATTACK, defender, Position.DEFENSE);

        String winner;
        if (result == 0) {
            winner = DRAW;
        } else if ((result > 0) == playerAttacks) {
            winner = PLAYER;
            playerScore++;
        } else {
            winner = AI;
            aiScore++;
        }

        String detail = String.format("%s ataca con %s (ATK %d) contra %s en defensa (DEF %d).",
                playerAttacks ? PLAYER : AI, attacker.getName(), attacker.getAtk(),
                defender.getName(), defender.getDef());

        for (BattleListener l : listeners) {
            l.onCardsPlayed(playerIndex, aiIndex, detail);
            l.onTurn(playerCard.getName(), aiCard.getName(), winner);
            l.onScoreChanged(playerScore, aiScore);
        }

        String duelWinner = checkDuelWinner();
        if (duelWinner != null) {
            finished = true;
            for (BattleListener l : listeners) {
                l.onDuelEnded(duelWinner);
            }
        } else {
            round++;
            playerAttacks = !playerAttacks;
            for (BattleListener l : listeners) {
                l.onRoundStarted(round, playerAttacks);
            }
        }
    }

    /**
     * Compara dos cartas según sus posiciones.
     * <ul>
     *   <li>Ambas en ataque: gana el mayor ATK.</li>
     *   <li>Una en ataque y otra en defensa: ATK del atacante contra DEF del defensor.</li>
     *   <li>Ambas en defensa: no hay batalla (empate).</li>
     * </ul>
     *
     * @return positivo si gana {@code a}, negativo si gana {@code b}, 0 si empatan
     */
    public static int compare(Card a, Position posA, Card b, Position posB) {
        if (posA == Position.ATTACK && posB == Position.ATTACK) {
            return Integer.compare(a.getAtk(), b.getAtk());
        }
        if (posA == Position.ATTACK) {
            return Integer.compare(a.getAtk(), b.getDef());
        }
        if (posB == Position.ATTACK) {
            return Integer.compare(a.getDef(), b.getAtk());
        }
        return 0;
    }

    /** @return el ganador del duelo, o null si aún sigue */
    private String checkDuelWinner() {
        if (playerScore >= POINTS_TO_WIN) {
            return PLAYER;
        }
        if (aiScore >= POINTS_TO_WIN) {
            return AI;
        }
        if (round < HAND_SIZE) {
            return null;
        }
        // Se acabaron las cartas sin que nadie llegara a 2 (hubo empates en alguna ronda).
        if (playerScore == aiScore) {
            return DRAW;
        }
        return playerScore > aiScore ? PLAYER : AI;
    }

    private int pickRandomAvailable(boolean[] used) {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                available.add(i);
            }
        }
        return available.get(random.nextInt(available.size()));
    }

    public boolean isPlayerAttacking() {
        return playerAttacks;
    }

    public boolean isFinished() {
        return finished;
    }
}
