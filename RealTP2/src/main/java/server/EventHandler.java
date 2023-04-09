package server;

/**
 * Chaque fonction qui étendra EventHandler devra posséder la fonction handle
 */
@FunctionalInterface
public interface EventHandler {
    /**
     * Doit etre capable de traiter la commande donnée par le serveur
     * @param cmd L'instruction de la commande 
     * @param arg Les paramètres de la commande
     */
    void handle(String cmd, String arg);
}
