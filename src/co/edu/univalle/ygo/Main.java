package co.edu.univalle.ygo;

import co.edu.univalle.ygo.api.YgoApiClient;
import co.edu.univalle.ygo.ui.DuelFrame;

import javax.swing.SwingUtilities;

/** Punto de entrada: crea la ventana en el hilo de eventos de Swing. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DuelFrame(new YgoApiClient()).setVisible(true));
    }
}
