package server;

import javafx.util.Pair;
import server.models.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Classe du serveur
 */
public class Server {

    public final static String REGISTER_COMMAND = "INSCRIRE";
    public final static String LOAD_COMMAND = "CHARGER";
    private final ServerSocket server;
    private Socket client;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ArrayList<EventHandler> handlers;

    /**
     * Ouvre un nouveau socket avec le port spécifié en paramètre
     * @param port le port auquel on veut attacher le serveur créé
     * @throws IOException Si la communication entre le client et le serveur a un problème (problème d'entrée/sortie)
     */
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1);
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     * Ajoute un handler à la liste de handler du serveur
     * Fait appel à la fonction notable : Serveur.handlers.add(EventHandler)
     * @param h Le handler en question, voir la classe EventHandler
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     * Transmet la commande a tous les Handlers et leur demande de l'exécuter, on suppose que seuls
     * les handlers capables d'exécuter la commande sont supposés être concernés
     * Fait appel à la fonction notable : EventHandler.handle(cmd, args)
     * @param cmd La premiere partie de la commande, l'instruction en elle même qui dicte
     * que doit faire le serveur
     * @param arg La deuxième partie de la commande, les arguments, qui donne à l'instruction
     * ses modalitées
     */
    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     * Boucle principale du programme, va verifier si un utilisateur se connecte au serveur et
     * va traiter toutes les instructions données en donnant des informations sur les erreurs rencontrées.
     * Fait appel aux fonctions notables : serveur.accept(), listen(), disconnect()
     */
    public void run() {
        while (true) {
            try {
                client = server.accept();
                System.out.println("Connecté au client: " + client);
                objectInputStream = new ObjectInputStream(client.getInputStream());
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                listen();
                disconnect();
                System.out.println("Client déconnecté!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ouvre l ecoute active entre le serveur et le client, chaque commande entrée sera transmise au serveur
     * depuis le client à travers une paire de Strings transmises à la fonction alertHandlers tel que l'on aura
     * handler.handle(commande, arguments) pour chaque commande donnée au serveur (à travers un système de dictionnaire)
     * En français --> On aura des gestionnaires de commandes qui vont prendre une par une chaque commande entrée
     * et les traiter
     * Fait appel aux fonctions notables : Pair.getKey(), Pair.getValue(), processCommandLine(ligne), alertHandlers(cmd, args)
     * @throws IOException Si la communication entre le client et le serveur a un problème (problème d'entrée/sortie)
     * @throws ClassNotFoundException Si le Handler n'a pas trouvé de classe correspondant à la commande entrée
     */
    public void listen() throws IOException, ClassNotFoundException {
        String line;
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     * Associe une ligne de commande telle que "commande arguments" donnée en entrée à une paire
     * formée par une commande(String) et des arguments(String) formant un objet (une "Pair") avec ces deux-là
     * @param line La ligne de commande donnée en entrée
     * @return Une paire (objet "Pair") telle que : Pair(commande, arguments)
     */
    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     * Fermer toutes les méthodes d'entrée et de sortie, déconnecter le serveur du client
     * Fait appel aux fonctions notables : client.close()
     * @throws IOException Si une erreur se produit lors de la communication/deconnection entre le serveur et le client
     */
    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /**
     *
     * @param cmd
     * @param arg
     */
    public void handleEvents(String cmd, String arg) {
        if (cmd.equals(REGISTER_COMMAND)) {
            handleRegistration();
        } else if (cmd.equals(LOAD_COMMAND)) {
            handleLoadCourses(arg);
        }
    }

    /**
     Lire un fichier texte contenant des informations sur les cours et les transofmer en liste d'objets 'Course'.
     La méthode filtre les cours par la session spécifiée en argument.
     Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) {
        ArrayList<Course> courses = new ArrayList<>();
        String coursesString = "";
        try {
            File cours = new File("./data/cours.txt");
            Scanner reader = new Scanner(cours);
            while (reader.hasNextLine()){
                String line = reader.nextLine();
                String[] data = line.split(" ");
                Course course = new Course(data[1], data[0], data[2]);
                courses.add(course);
            }
            reader.close();
            for (int i = courses.size(); i > 0 ; i--){
                coursesString += courses.get(i).toString() + "\n";
            }
            objectOutputStream.writeUTF(coursesString);
        } catch (IOException e) {
            System.out.println("Error : ");
            e.printStackTrace();
        }
    }

    /**
     Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     et renvoyer un message de confirmation au client.
     La méthode gére les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        String inscriptionString = "";
        try {
            RegistrationForm input = (RegistrationForm) objectInputStream.readObject();
            FileWriter printer = new FileWriter("./data/inscription.txt");
            inscriptionString += input.getCourse().getSession() + "\t" + input.getCourse().getCode() + "\t" +
                    input.getMatricule() + "\t" + input.getPrenom() + "\t" +
                    input.getNom() + "\t" + input.getEmail();
            printer.write(inscriptionString);
            printer.close();
        } catch (IOException e) {
            System.out.println(" Error : ");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
