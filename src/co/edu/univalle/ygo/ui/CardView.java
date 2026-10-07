package co.edu.univalle.ygo.ui;

import co.edu.univalle.ygo.model.Card;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Muestra una carta con su imagen, nombre, ATK y DEF.
 * <p>
 * Es un botón seleccionable: el jugador marca la carta que quiere jugar.
 * Las cartas de la máquina usan la misma vista, pero ocultas y no seleccionables
 * hasta que se revelan.
 */
public class CardView extends JToggleButton {

    public static final int IMAGE_WIDTH = 140;
    public static final int IMAGE_HEIGHT = 204;

    private static final Color SELECTED_BORDER = new Color(0xE0A100);
    private static final Color NORMAL_BORDER = new Color(0x888888);

    /**
     * @param selectable false para las cartas de la máquina: se ven normales
     *                   pero ignoran los clics
     */
    public CardView(boolean selectable) {
        if (!selectable) {
            setModel(new ToggleButtonModel() {
                @Override
                public void setSelected(boolean b) {
                }

                @Override
                public void setPressed(boolean b) {
                }

                @Override
                public void setArmed(boolean b) {
                }
            });
            setFocusable(false);
        }
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setFocusPainted(false);
        setPreferredSize(new Dimension(IMAGE_WIDTH + 40, IMAGE_HEIGHT + 90));
        setBorder(BorderFactory.createLineBorder(NORMAL_BORDER, 2));
        // El borde resaltado hace más visible cuál carta está seleccionada.
        addItemListener(e -> setBorder(BorderFactory.createLineBorder(
                isSelected() ? SELECTED_BORDER : NORMAL_BORDER, isSelected() ? 4 : 2)));
    }

    /** Muestra la carta boca arriba. */
    public void showCard(Card card, Icon image) {
        setIcon(image);
        setText(describe(card) + (image == null ? "<br><i>(imagen no disponible)</i>" : "") + "</html>");
        setToolTipText(card.getType());
    }

    /** Muestra la carta boca abajo (cartas de la máquina aún no jugadas). */
    public void showHidden() {
        setIcon(null);
        setText("<html><center><b>?</b><br>Carta oculta</center></html>");
        setToolTipText(null);
    }

    /** Muestra un hueco vacío mientras se cargan las cartas. */
    public void showEmpty(String text) {
        setIcon(null);
        setText(text);
        setToolTipText(null);
    }

    private static String describe(Card card) {
        return "<html><center><b>" + escape(card.getName()) + "</b><br>ATK " + card.getAtk()
                + " / DEF " + card.getDef();
    }

    /** Escapa los caracteres especiales de HTML del nombre de la carta. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Escala una imagen al tamaño de la vista. Se llama desde el hilo de fondo
     * para no ocupar el hilo de eventos de Swing con el redimensionado.
     */
    public static Icon toIcon(BufferedImage image) {
        BufferedImage scaled = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        g.dispose();
        return new ImageIcon(scaled);
    }
}
