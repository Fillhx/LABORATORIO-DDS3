package co.edu.univalle.ygo.ui;

import co.edu.univalle.ygo.api.ApiException;
import co.edu.univalle.ygo.api.YgoApiClient;
import co.edu.univalle.ygo.logic.BattleListener;
import co.edu.univalle.ygo.logic.Duel;
import co.edu.univalle.ygo.model.Card;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal del duelo.
 * <p>
 * Carga las cartas en segundo plano con un {@link SwingWorker} para no bloquear la
 * interfaz, y escucha los eventos de {@link Duel} a través de {@link BattleListener}.
 */
public class DuelFrame extends JFrame implements BattleListener {

    private static final Color ERROR_COLOR = new Color(0xC62828);

    private final YgoApiClient apiClient;

    private final JButton startButton = new JButton("Iniciar duelo");
    private final JButton chooseButton = new JButton("Elegir carta");
    private final JLabel scoreLabel = new JLabel();
    private final JLabel turnLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Pulsa \"Iniciar duelo\" para repartir las cartas.");
    private final JTextArea logArea = new JTextArea(8, 60);

    private final List<CardView> playerViews = new ArrayList<>();
    private final List<CardView> aiViews = new ArrayList<>();
    private final ButtonGroup playerGroup = new ButtonGroup();

    private Duel duel;
    private Hands hands;

    public DuelFrame(YgoApiClient apiClient) {
        super("Yu-Gi-Oh! Duel Lite");
        this.apiClient = apiClient;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildLayout();
        updateScore(0, 0);
        pack();
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        // Barra superior: botones, marcador y de quién es el turno.
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        chooseButton.setEnabled(false);
        startButton.addActionListener(e -> loadCards());
        chooseButton.addActionListener(e -> playSelectedCard());
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 16f));
        top.add(startButton);
        top.add(chooseButton);
        top.add(scoreLabel);
        top.add(turnLabel);

        // Centro: cartas de la máquina arriba y del jugador abajo.
        JPanel aiPanel = new JPanel(new GridLayout(1, Duel.HAND_SIZE, 10, 0));
        aiPanel.setBorder(BorderFactory.createTitledBorder("Cartas de la máquina"));
        JPanel playerPanel = new JPanel(new GridLayout(1, Duel.HAND_SIZE, 10, 0));
        playerPanel.setBorder(BorderFactory.createTitledBorder("Tus cartas (selecciona una y pulsa \"Elegir carta\")"));
        for (int i = 0; i < Duel.HAND_SIZE; i++) {
            CardView aiView = new CardView(false);
            aiView.showEmpty("Sin carta");
            aiViews.add(aiView);
            aiPanel.add(aiView);

            CardView playerView = new CardView(true);
            playerView.showEmpty("Sin carta");
            playerView.setEnabled(false);
            playerViews.add(playerView);
            playerGroup.add(playerView);
            playerPanel.add(playerView);
        }
        JPanel board = new JPanel(new GridLayout(2, 1, 0, 8));
        board.add(aiPanel);
        board.add(playerPanel);

        // Abajo: log de batalla desplazable y línea de estado.
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de batalla"));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(logScroll, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(top, BorderLayout.NORTH);
        content.add(board, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        setContentPane(content);
    }

    // ---------------------------------------------------------------------
    // Carga de cartas (en segundo plano)
    // ---------------------------------------------------------------------

    /** Cartas e imágenes de ambos jugadores, ya listas para mostrar. */
    private static final class Hands {
        final List<Card> playerCards = new ArrayList<>();
        final List<Icon> playerIcons = new ArrayList<>();
        final List<Card> aiCards = new ArrayList<>();
        final List<Icon> aiIcons = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
    }

    /** Pide las 6 cartas a la API sin bloquear la interfaz. */
    private void loadCards() {
        startButton.setEnabled(false);
        chooseButton.setEnabled(false);
        duel = null;
        resetBoard();
        logArea.setText("");
        setStatus("Cargando cartas desde YGOProDeck...", false);

        new SwingWorker<Hands, String>() {
            @Override
            protected Hands doInBackground() throws ApiException {
                Hands loaded = new Hands();
                int total = Duel.HAND_SIZE * 2;
                for (int i = 0; i < total; i++) {
                    boolean forPlayer = i < Duel.HAND_SIZE;
                    publish("Cargando carta " + (i + 1) + " de " + total + "...");
                    Card card = apiClient.fetchRandomMonster();
                    Icon icon = loadIcon(card, loaded.warnings);
                    (forPlayer ? loaded.playerCards : loaded.aiCards).add(card);
                    (forPlayer ? loaded.playerIcons : loaded.aiIcons).add(icon);
                }
                return loaded;
            }

            @Override
            protected void process(List<String> messages) {
                setStatus(messages.get(messages.size() - 1), false);
            }

            @Override
            protected void done() {
                try {
                    startDuel(get());
                } catch (ExecutionException e) {
                    String message = e.getCause() instanceof ApiException
                            ? e.getCause().getMessage()
                            : "No se pudo cargar la carta: " + e.getCause();
                    showError(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("La carga de cartas fue interrumpida.");
                }
            }
        }.execute();
    }

    /**
     * Descarga la imagen de la carta. Si falla, la carta se juega igual sin imagen
     * y se deja un aviso en el log.
     */
    private Icon loadIcon(Card card, List<String> warnings) {
        try {
            return CardView.toIcon(apiClient.fetchImage(card.getImageUrl()));
        } catch (ApiException e) {
            warnings.add("Aviso: no se pudo cargar la imagen de " + card.getName() + ". " + e.getMessage());
            return null;
        }
    }

    /** Se ejecuta en el hilo de Swing cuando ya están las 6 cartas: arma el tablero y arranca. */
    private void startDuel(Hands loaded) {
        hands = loaded;
        for (int i = 0; i < Duel.HAND_SIZE; i++) {
            CardView view = playerViews.get(i);
            view.showCard(loaded.playerCards.get(i), loaded.playerIcons.get(i));
            view.setEnabled(true);
            aiViews.get(i).showHidden();
        }
        for (String warning : loaded.warnings) {
            log(warning);
        }

        duel = new Duel(loaded.playerCards, loaded.aiCards);
        duel.addBattleListener(this);
        log("¡Cartas repartidas! Comienza el duelo (gana quien llegue a 2 puntos).");
        setStatus("Cartas cargadas.", false);
        startButton.setText("Nuevo duelo");
        chooseButton.setEnabled(true);
        duel.start();
        pack();
    }

    private void resetBoard() {
        playerGroup.clearSelection();
        for (int i = 0; i < Duel.HAND_SIZE; i++) {
            playerViews.get(i).showEmpty("Cargando...");
            playerViews.get(i).setEnabled(false);
            aiViews.get(i).showEmpty("Cargando...");
        }
        updateScore(0, 0);
        turnLabel.setText(" ");
    }

    // ---------------------------------------------------------------------
    // Jugadas
    // ---------------------------------------------------------------------

    private void playSelectedCard() {
        if (duel == null || duel.isFinished()) {
            return;
        }
        int selected = -1;
        for (int i = 0; i < playerViews.size(); i++) {
            CardView view = playerViews.get(i);
            if (view.isSelected() && view.isEnabled()) {
                selected = i;
            }
        }
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Primero selecciona una de tus cartas.",
                    "Elegir carta", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        duel.playRound(selected);
    }

    // ---------------------------------------------------------------------
    // Eventos del duelo (BattleListener)
    // ---------------------------------------------------------------------

    @Override
    public void onRoundStarted(int round, boolean playerAttacks) {
        String who = playerAttacks ? "Atacas tú (cuenta tu ATK)" : "Ataca la máquina (cuenta tu DEF)";
        if (round == 1) {
            log("Sorteo del turno inicial: " + (playerAttacks ? Duel.PLAYER : Duel.AI) + " ataca primero.");
        }
        log("--- Ronda " + round + ": " + who + " ---");
        turnLabel.setText("Ronda " + round + " · " + who);
    }

    @Override
    public void onCardsPlayed(int playerIndex, int aiIndex, String detail) {
        playerGroup.clearSelection();
        playerViews.get(playerIndex).setEnabled(false);
        aiViews.get(aiIndex).showCard(hands.aiCards.get(aiIndex), hands.aiIcons.get(aiIndex));
        log(detail);
    }

    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        log(Duel.PLAYER + " jugó " + playerCard + " | " + Duel.AI + " jugó " + aiCard);
        log(Duel.DRAW.equals(winner)
                ? "Resultado: empate, nadie suma punto."
                : "Resultado: gana la ronda " + winner + ".");
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        updateScore(playerScore, aiScore);
        log("Marcador: " + Duel.PLAYER + " " + playerScore + " - " + aiScore + " " + Duel.AI);
    }

    @Override
    public void onDuelEnded(String winner) {
        String message;
        if (Duel.PLAYER.equals(winner)) {
            message = "¡Ganaste el duelo!";
        } else if (Duel.AI.equals(winner)) {
            message = "La máquina ganó el duelo.";
        } else {
            message = "El duelo terminó en empate.";
        }
        log("=== " + message + " ===");
        turnLabel.setText("Duelo terminado");
        chooseButton.setEnabled(false);
        startButton.setEnabled(true);
        for (CardView view : playerViews) {
            view.setEnabled(false);
        }
        // Se muestra después de que Swing repinte el último turno.
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message,
                "Fin del duelo", JOptionPane.INFORMATION_MESSAGE));
    }

    // ---------------------------------------------------------------------
    // Utilidades de la vista
    // ---------------------------------------------------------------------

    private void updateScore(int playerScore, int aiScore) {
        scoreLabel.setText(Duel.PLAYER + " " + playerScore + " - " + aiScore + " " + Duel.AI);
    }

    private void log(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.setForeground(error ? ERROR_COLOR : getForeground());
    }

    /** Error visible: línea de estado en rojo, entrada en el log y diálogo. */
    private void showError(String message) {
        setStatus(message, true);
        log("ERROR: " + message);
        startButton.setEnabled(true);
        for (CardView view : playerViews) {
            view.showEmpty("Sin carta");
        }
        for (CardView view : aiViews) {
            view.showEmpty("Sin carta");
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
