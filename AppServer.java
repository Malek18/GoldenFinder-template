package com.example.goldfinder.server;

import com.example.goldfinder.Bot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class AppServer {

    public static final int  ROW_COUNT = 10;
    public static final int COLUMN_COUNT = 10;
    final static int serverPort = 1234;
    final static String serverIP = "localhost";

    public static Grid grid;

    private final static ArrayList<SocketChannel> clients = new ArrayList<>();
    private final static ArrayList<String> players = new ArrayList<>();
    private final static ArrayList<Integer> scors = new ArrayList<>();
    private final static ArrayList<Position> positions = new ArrayList<>();

    private final static boolean[][] discovered = new boolean[COLUMN_COUNT][ROW_COUNT];
    private static int discoveredTiles = COLUMN_COUNT*ROW_COUNT;
    private static int remainingGold = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
//        if (args.length >= 1) {
//            int nbPlayers = Integer.parseInt(args[0]);
//            int nbBots = 0;
//            if (args.length == 2) {
//                nbBots = Integer.parseInt(args[1]);
//            }
//            serverStart(nbPlayers, nbBots);
//        }
//        System.out.println("Error, Synthaxe : java AppServer {port} {nbPlayers} [nbBots]");
//        System.exit(-1);
        serverStart(1, 12);
    }
    //Methode de demarrage du serveur
    private static void serverStart(int nbPlayers, int nbBots) throws IOException, InterruptedException {
        if (nbPlayers+nbBots > (COLUMN_COUNT*ROW_COUNT)/4){
            System.exit(-1);
        }
        // Ouverture du canal de serveur et configuration du selecteur
        ServerSocketChannel serveur = ServerSocketChannel.open();
        Selector selector = Selector.open();
        serveur.bind(new InetSocketAddress(serverIP, serverPort));
        serveur.configureBlocking(false);
        serveur.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("server should be listening on port " + serverPort);

        boolean alreadySend = false;
        //Demarrage du Bot
        for (int i = 0; i < nbBots; i++){
            Bot bot = new Bot(serverIP, serverPort, "BOT"+ (i+1));
            (new Thread(bot)).start();
        }
        //Boucle principale du serveur
        while (true){
            selector.select();//Attente des evenements sur les canaux
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable() && !startingCondition(nbPlayers, nbBots)){
                   //Acceptation de nouvelles connexions clients
                    handleNewClient(serveur, selector);
                }
                if (key.isReadable()) {
                    //Lecture des données envoyées par les clients
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(16384);
                    int bytesRead = client.read(buffer);

                    if (bytesRead < 1){
                        int clientIndex = clients.indexOf(client);
                        Position latsPos = positions.get(clientIndex);
                        setEmpty(latsPos);
                        positions.set(clientIndex, new Position(-1, -1));

                        System.out.println("Player : " + players.get(clientIndex) + " is disconnected");
                        key.cancel();
                        client.close();
                    }

                    if (bytesRead > 0) {
                        buffer.flip();

                        if (!startingCondition(nbPlayers, nbBots)) {
                            //Gestion des nouveaux joueurs avant le demarrage du jeu
                            handleNewPlayer(buffer, bytesRead, client);
                            if (startingCondition(nbPlayers, nbBots) && !alreadySend){
                                // Envoi du signal de démarrage du jeu lorsque toutes les conditions sont réunies
                                sendStartGame();
                            }
                        }

                        else
                            // Gestion des messages des joueurs après le démarrage du jeu
                            handleMessage(buffer, bytesRead, client);
                    }
                    buffer.clear();
                }
            }
            // Vérification si tous les joueurs ont quitté le jeu
            allGone(nbPlayers);

            if (endingCondition()){ break; }
        }
        // Construction de la chaîne de fin de partie avec les scores des joueurs
        String endingString = "GAME_END ";
        for (int i = 0; i < clients.size(); i++){
            endingString += players.get(i) + ":" + scors.get(i) + " ";
        }
        endingString += "END";
        // Envoi de la chaîne de fin de partie à tous les joueurs connectés
        for (SocketChannel player : clients){
            if (player.isConnected()){
                player.write(ByteBuffer.wrap(endingString.getBytes()));
                Thread.sleep(10); // Pause pour assurer la transmission du message
            }
        }
        createScorsFile();

        Thread.sleep(1000);

        System.exit(0);//Fermeture du serveur apres la fin de partie
    }

    private static boolean startingCondition(int nbPlayers, int nbBots){
        return players.size() == (nbPlayers + nbBots);
    }

    private static boolean endingCondition() {
        return discoveredTiles == 0 && remainingGold == 0;
    }

    private static void sendStartGame() throws IOException, InterruptedException {
        System.out.println("Sending GAME_START");
        grid = new Grid(COLUMN_COUNT, ROW_COUNT, new Random());

        for (int x = 0; x < COLUMN_COUNT; x++){
            for (int y = 0; y < ROW_COUNT; y++){
                if (grid.hasGold(x, y))
                    remainingGold++;
            }
        }

        String game_start = "GAME_START ";
        for (int i = 0; i < players.size(); i++){
            game_start += players.get(i) + ":" + i + " ";
        }
        game_start += "END";

        for (int i = 0; i < clients.size(); i++){
            clients.get(i).write(ByteBuffer.wrap(game_start.getBytes(StandardCharsets.UTF_8)));
            Thread.sleep(10);
            clients.get(i).write(ByteBuffer.wrap(("POSITION:" + positions.get(i).getX() + "," + positions.get(i).getY()).getBytes()));
            Thread.sleep(10);
        }
    }
    //Methode qui gere l'arrivee d'un nouveau client sur le serveur
    private static void handleNewClient(ServerSocketChannel serveur, Selector selector) throws IOException{
        //Acceptation d'une nouvelle connexio,
        SocketChannel client = serveur.accept();

        //Configuration s'un canal du client en mode non bloquant
        //et l'enregistre pour la lecture avec le selecteur
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("New player");
    }

    private static void handleNewPlayer(ByteBuffer buffer, int bytesRead, SocketChannel client) {
        byte[] bytes = new byte[bytesRead];
        buffer.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);
        if (message.contains("GAME_JOIN:")){
            //Ajout d'un clienr à la liste des clients connectés
            clients.add(client);
            //Extraction du nom du joueur à partir du message
            String playerName = message.substring(10);
            players.add(playerName);

            scors.add(0);
            //Creation d'une position aleatoire
            Position position = createPosition();

            positions.add(position);
        }
        else {
            System.out.println("Error on : GAME_JOIN:playerName");
            System.exit(-1);
        }
    }

    private static Position createPosition(){
        //Creation d'une position aleatoire
        Random random = new Random();
        Position position = new Position(random.nextInt(COLUMN_COUNT), random.nextInt(ROW_COUNT));

        for (Position pos : positions){
            if (pos.getX() == position.getX() && pos.getY() == position.getY()){
                position = createPosition();
                break;
            }
        }

        return position;
    }

    private static void handleMessage(ByteBuffer buffer, int bytesRead, SocketChannel client) throws IOException {
        byte[] bytes = new byte[bytesRead];
        buffer.get(bytes);
        String request = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(request);

        Position clientPosition = positions.get(clients.indexOf(client));
        System.out.println(players.get(positions.indexOf(clientPosition)) + " : " + clientPosition);

        //SURROUNDING
        if (request.compareTo("SURROUNDING") == 0) {
            String surrounding = "";

            //UP side
            surrounding += getUpItem(clientPosition);

            //RIGHT side
            surrounding += getRightItem(clientPosition);

            //DOWN side
            surrounding += getDownItem(clientPosition);

            //LEFT side
            surrounding += getLeftItem(clientPosition);

            surrounding += "END";

            System.out.println(surrounding);
            client.write(ByteBuffer.wrap(surrounding.getBytes()));
        }

        //dir
        else if (request.compareTo("UP") == 0 || request.compareTo("RIGHT") == 0 || request.compareTo("DOWN") == 0 || request.compareTo("LEFT") == 0) {
            String response = "VALIDMOVE:EMPTY";
            switch (request){
                case "UP" :
                    if (grid.upWall(clientPosition.getX(), clientPosition.getY())){
                        response = "INVALIDMOVE";
                        break;
                    }

                    else {
                        Position nextPosition = new Position(clientPosition.getX(), clientPosition.getY() - 1);
                        for (Position position : positions){
                            if (position.getX() == nextPosition.getX() && position.getY() == nextPosition.getY()){
                                response = "INVALIDMOVE";
                                break;
                            }
                        }
                        if (response.compareTo("INVALIDMOVE") == 0)
                            break;

                        setEmpty(clientPosition);
                        clientPosition.move(request);
                        setPlayerOrBot(clientPosition);
                        if (grid.hasGold(nextPosition.getX(), nextPosition.getY())){
                            scors.set(positions.indexOf(clientPosition), scors.get(positions.indexOf(clientPosition)) + 1);
                            grid.removeGold(nextPosition.getX(), nextPosition.getY());
                            remainingGold--;
                            response = "VALIDMOVE:GOLD";
                            break;
                        }
                    }
                    break;

                case "RIGHT" :
                    if (grid.rightWall(clientPosition.getX(), clientPosition.getY())){
                        response = "INVALIDMOVE";
                        break;
                    }
                    else {
                        Position nextPosition = new Position(clientPosition.getX() + 1, clientPosition.getY());
                        for (Position position : positions){
                            if (position.getX() == nextPosition.getX() && position.getY() == nextPosition.getY()){
                                response = "INVALIDMOVE";
                                break;
                            }
                        }
                        if (response.compareTo("INVALIDMOVE") == 0)
                            break;

                        setEmpty(clientPosition);
                        clientPosition.move(request);
                        setPlayerOrBot(clientPosition);
                        if (grid.hasGold(nextPosition.getX(), nextPosition.getY())){
                            scors.set(positions.indexOf(clientPosition), scors.get(positions.indexOf(clientPosition)) + 1);
                            grid.removeGold(nextPosition.getX(), nextPosition.getY());
                            remainingGold--;
                            response = "VALIDMOVE:GOLD";
                            break;
                        }
                    }
                    break;

                case "DOWN" :
                    if (grid.downWall(clientPosition.getX(), clientPosition.getY())){
                        response = "INVALIDMOVE";
                        break;
                    }
                    else {
                        Position nextPosition = new Position(clientPosition.getX(), clientPosition.getY() + 1);
                        for (Position position : positions){
                            if (position.getX() == nextPosition.getX() && position.getY() == nextPosition.getY()){
                                response = "INVALIDMOVE";
                                break;
                            }
                        }
                        if (response.compareTo("INVALIDMOVE") == 0)
                            break;

                        setEmpty(clientPosition);
                        clientPosition.move(request);
                        setPlayerOrBot(clientPosition);
                        if (grid.hasGold(nextPosition.getX(), nextPosition.getY())){
                            scors.set(positions.indexOf(clientPosition), scors.get(positions.indexOf(clientPosition)) + 1);
                            grid.removeGold(nextPosition.getX(), nextPosition.getY());
                            remainingGold--;
                            response = "VALIDMOVE:GOLD";
                            break;
                        }
                    }
                    break;

                case "LEFT" :
                    if (grid.leftWall(clientPosition.getX(), clientPosition.getY())){
                        response = "INVALIDMOVE";
                        break;
                    }
                    else {
                        Position nextPosition = new Position(clientPosition.getX() - 1, clientPosition.getY());
                        for (Position position : positions){
                            if (position.getX() == nextPosition.getX() && position.getY() == nextPosition.getY()){
                                response = "INVALIDMOVE";
                                break;
                            }
                        }
                        if (response.compareTo("INVALIDMOVE") == 0)
                            break;

                        setEmpty(clientPosition);
                        clientPosition.move(request);
                        setPlayerOrBot(clientPosition);
                        if (grid.hasGold(nextPosition.getX(), nextPosition.getY())){
                            scors.set(positions.indexOf(clientPosition), scors.get(positions.indexOf(clientPosition)) + 1);
                            grid.removeGold(nextPosition.getX(), nextPosition.getY());
                            remainingGold--;
                            response = "VALIDMOVE:GOLD";
                            break;
                        }
                    }
                    break;
            }
            System.out.println(response);
            client.write(ByteBuffer.wrap(response.getBytes()));
        }

        //LEADER:n
        else if (request.startsWith("LEADER:")){
            int n = Math.min(Integer.parseInt(request.split(":")[1]), clients.size());

            sendLeader(n, client);
        }

        System.out.println("discoveredTiles:" + discoveredTiles + ", remainingGold:" + remainingGold);
    }

    private static String getUpItem(Position clientPosition){
        String up = "UP:";
        String item = "EMPTY ";
        if (clientPosition.getY() > 0 && grid.hasGold(clientPosition.getX(), clientPosition.getY() - 1)){
            item = "GOLD ";
        }
        for (Position position : positions){
            if (clientPosition.getY() > 0 && position.getX() == clientPosition.getX() && position.getY() == clientPosition.getY() - 1){
                if (players.get(positions.indexOf(position)).startsWith("BOT"))
                    item = players.get(positions.indexOf(position)) + " ";
                else
                    item = "PLAYER" + (positions.indexOf(position) + 1) + " ";
                break;
            }
        }
        if (grid.upWall(clientPosition.getX(), clientPosition.getY())){
            item = "WALL ";
        }
        up += item;

        if (clientPosition.getY() > 0) {
            if (!discovered[clientPosition.getX()][clientPosition.getY() - 1]) {
                if (!grid.upWall(clientPosition.getX(), clientPosition.getY())) {
                    discovered[clientPosition.getX()][clientPosition.getY() - 1] = true;
                    discoveredTiles--;
                }
            }
        }

        return up;
    }

    private static String getRightItem(Position clientPosition){
        String right = "RIGHT:";
        String item = "EMPTY ";
        if (clientPosition.getX() < COLUMN_COUNT - 1 && grid.hasGold(clientPosition.getX() + 1, clientPosition.getY())){
            item = "GOLD ";
        }
        for (Position position : positions){
            if (position.getX() == clientPosition.getX() + 1 && position.getY() == clientPosition.getY()){
                if (players.get(positions.indexOf(position)).startsWith("BOT"))
                    item = players.get(positions.indexOf(position)) + " ";
                else
                    item = "PLAYER" + (positions.indexOf(position) + 1) + " ";
                break;
            }
        }
        if (grid.rightWall(clientPosition.getX(), clientPosition.getY())){
            item = "WALL ";
        }
        right += item;

        if (clientPosition.getX() < COLUMN_COUNT - 1) {
            if (!discovered[clientPosition.getX() + 1][clientPosition.getY()]) {
                if (!grid.rightWall(clientPosition.getX(), clientPosition.getY())){
                    discovered[clientPosition.getX() + 1][clientPosition.getY()] = true;
                    discoveredTiles--;
                }
            }
        }

        return right;
    }

    private static String getDownItem(Position clientPosition){
        String down = "DOWN:";
        String item = "EMPTY ";
        if (clientPosition.getY() < ROW_COUNT - 1 && grid.hasGold(clientPosition.getX(), clientPosition.getY() + 1)){
            item = "GOLD ";
        }
        for (Position position : positions){
            if (position.getX() == clientPosition.getX() && position.getY() == clientPosition.getY() + 1){
                if (players.get(positions.indexOf(position)).startsWith("BOT"))
                    item = players.get(positions.indexOf(position)) + " ";
                else
                    item = "PLAYER" + (positions.indexOf(position) + 1) + " ";
                break;
            }
        }
        if (grid.downWall(clientPosition.getX(), clientPosition.getY())){
            item = "WALL ";
        }
        down += item;

        if (clientPosition.getY() < ROW_COUNT - 1) {
            if (!discovered[clientPosition.getX()][clientPosition.getY() + 1]) {
                if (!grid.downWall(clientPosition.getX(), clientPosition.getY())) {
                    discovered[clientPosition.getX()][clientPosition.getY() + 1] = true;
                    discoveredTiles--;
                }
            }
        }

        return down;
    }

    private static String getLeftItem(Position clientPosition){
        String left = "LEFT:";
        String item = "EMPTY ";
        if (clientPosition.getX() > 0 && grid.hasGold(clientPosition.getX() - 1, clientPosition.getY())){
            item = "GOLD ";
        }
        for (Position position : positions){
            if (position.getX() == clientPosition.getX() - 1 && position.getY() == clientPosition.getY()){
                if (players.get(positions.indexOf(position)).startsWith("BOT"))
                    item = players.get(positions.indexOf(position)) + " ";
                else
                    item = "PLAYER" + (positions.indexOf(position) + 1) + " ";
                break;
            }
        }
        if (grid.leftWall(clientPosition.getX(), clientPosition.getY())){
            item = "WALL ";
        }
        left += item;

        if (clientPosition.getX() > 0) {
            if (!discovered[clientPosition.getX() - 1][clientPosition.getY()]) {
                if (!grid.leftWall(clientPosition.getX(), clientPosition.getY())) {
                    discovered[clientPosition.getX() - 1][clientPosition.getY()] = true;
                    discoveredTiles--;
                }
            }
        }

        return left;
    }

    private static void setEmpty(Position clientPosition) throws IOException {
        for (Position position : positions){
            if (position.getX() == clientPosition.getX() && position.getY()+1 == clientPosition.getY() && !grid.upWall(clientPosition.getX(), clientPosition.getY())) {
                clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("UP:EMPTY").getBytes()));
            }
            else if (position.getX()-1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.rightWall(clientPosition.getX(), clientPosition.getY())) {
                clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("RIGHT:EMPTY").getBytes()));
            }
            else if (position.getX() == clientPosition.getX() && position.getY()-1 == clientPosition.getY() && !grid.downWall(clientPosition.getX(), clientPosition.getY())) {
                clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("DOWN:EMPTY").getBytes()));
            }
            else if (position.getX()+1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.leftWall(clientPosition.getX(), clientPosition.getY())) {
                clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("LEFT:EMPTY").getBytes()));
            }
        }
    }

    private static void setPlayerOrBot(Position clientPosition) throws IOException {
        if (players.get(positions.indexOf(clientPosition)).startsWith("BOT")){
            for (Position position : positions){
                if (position.getX() == clientPosition.getX() && position.getY()+1 == clientPosition.getY() && !grid.upWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("DOWN:BOT" + players.get(positions.indexOf(clientPosition))).getBytes()));
                }
                else if (position.getX()-1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.rightWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("LEFT:BOT" + players.get(positions.indexOf(clientPosition))).getBytes()));
                }
                else if (position.getX() == clientPosition.getX() && position.getY()-1 == clientPosition.getY() && !grid.downWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("UP:BOT" + players.get(positions.indexOf(clientPosition))).getBytes()));
                }
                else if (position.getX()+1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.leftWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("RIGHT:BOT" + players.get(positions.indexOf(clientPosition))).getBytes()));
                }
            }
        }
        else {
            for (Position position : positions){
                if (position.getX() == clientPosition.getX() && position.getY()+1 == clientPosition.getY() && !grid.upWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("DOWN:PLAYER" + (positions.indexOf(clientPosition)+1)).getBytes()));
                }
                else if (position.getX()-1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.rightWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("LEFT:PLAYER" + (positions.indexOf(clientPosition)+1)).getBytes()));
                }
                else if (position.getX() == clientPosition.getX() && position.getY()-1 == clientPosition.getY() && !grid.downWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("UP:PLAYER" + (positions.indexOf(clientPosition)+1)).getBytes()));
                }
                else if (position.getX()+1 == clientPosition.getX() && position.getY() == clientPosition.getY() && !grid.leftWall(clientPosition.getX(), clientPosition.getY())) {
                    clients.get(positions.indexOf(position)).write(ByteBuffer.wrap(("RIGHT:PLAYER" + (positions.indexOf(clientPosition)+1)).getBytes()));
                }
            }
        }
    }
    //Obtient les n meilleurs leaders avec leurs scores respectifs.
    private static String getLeaders(int n){
        int[] leadersScorsTab = new int[n];
        String[] leadersNameTab = new String[n];
        ArrayList<Integer> minScors = new ArrayList<>();
        for (int score : scors){
            minScors.add(score);
        }
        ArrayList<String> minName = new ArrayList<>();
        for (String player : players){
            minName.add(player);
        }

        for (int i = 0; i < n; i++){
            int max = -1;
            int index = 0;
            for (int y = 0; y < minScors.size(); y++){
                if (minScors.get(y) > max){
                    max = minScors.get(y);
                    index = y;
                }
            }
            leadersScorsTab[i] = max;
            leadersNameTab[i] = minName.get(index);
            minScors.remove(index);
            minName.remove(index);
        }

        String nLeader = "";
        for (int i = 0; i < n; i++){
            nLeader += leadersNameTab[i] + ":" + leadersScorsTab[i] + " ";
        }
        nLeader = nLeader.substring(0, nLeader.length()-1);
        System.out.println(nLeader);

        return nLeader;
    }
    //Envoie au client les n meilleurs leaders avec leurs scores respectifs.
    private static void sendLeader(int n, SocketChannel client) throws IOException {
        String nLeaders = "SCORE: " + getLeaders(n) + " END";
        client.write(ByteBuffer.wrap(nLeaders.getBytes()));
    }

    //Crée un fichier de scores contenant les n meilleurs leaders avec leurs scores respectifs.
    private static void createScorsFile() throws IOException {
        int n = clients.size();
        String leaders = getLeaders(n);

        String[] leaderTab = leaders.split(" ");
        File file = new File("src/main/resources/score.txt");
        FileWriter fw = new FileWriter(file);
        for (String player_score : leaderTab){
            fw.write(player_score + "\n");
        }
        fw.close();
    }

    //Vérifie si tous les joueurs sont déconnectés.
    private static void allGone(int nbPlayers){
        int disconected = 0;
        for (SocketChannel client : clients){
            if (!client.isConnected()){
                disconected++;
            }
        }
        if (disconected == nbPlayers){
            System.out.println("All of players are gone");
            System.exit(0);
        }
    }
}
