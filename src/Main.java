import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static ArrayList<ShooterServer> servers = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        printWelcome();
        
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    startServer();
                    break;
                case "2":
                    listServers();
                    break;
                case "3":
                    stopServer();
                    break;
                case "4":
                    stopAllServers();
                    break;
                case "5":
                    running = false;
                    stopAllServers();
                    System.out.println("\n[✓] Shutting down. Goodbye!");
                    break;
                default:
                    System.out.println("\n[✗] Invalid option. Please try again.");
            }
        }
        
        scanner.close();
    }
    
    private static void printWelcome() {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   ONLINE SHOOTER SERVER MANAGER - CLI      ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void printMenu() {
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.println("│              MAIN MENU                  │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ 1. Start new server                     │");
        System.out.println("│ 2. List active servers                  │");
        System.out.println("│ 3. Stop a server                        │");
        System.out.println("│ 4. Stop all servers                     │");
        System.out.println("│ 5. Exit                                 │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("\nSelect option: ");
    }
    
    private static void startServer() {
        System.out.println("\n--- START NEW SERVER ---");
        
        // Input porta
        System.out.print("Enter port (1-65535): ");
        String portInput = scanner.nextLine().trim();
        int port;
        
        try {
            port = Integer.parseInt(portInput);
            if (port < 1 || port > 65535) {
                System.out.println("[✗] Port must be between 1 and 65535!");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[✗] Invalid port number!");
            return;
        }
        
        // Verifica se la porta è già in uso
        for (ShooterServer s : servers) {
            if (s.getPort() == port) {
                System.out.println("[✗] Port " + port + " is already in use by another server!");
                return;
            }
        }
        
        // Input mappa
        System.out.print("Select map (0-4): ");
        String mapInput = scanner.nextLine().trim();
        int map;
        
        try {
            map = Integer.parseInt(mapInput);
            if (map < 0 || map > 4) {
                System.out.println("[✗] Map must be between 0 and 4!");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[✗] Invalid map number!");
            return;
        }
        
        // Avvia il server
        try {
            ShooterServer server = new ShooterServer(port, map);
            servers.add(server);
            System.out.println("[✓] Server started successfully on port " + port + " with map " + map);
        } catch (IOException e) {
            System.out.println("[✗] Error: Port already in use or invalid! " + e.getMessage());
        }
    }
    
    private static void listServers() {
        System.out.println("\n--- ACTIVE SERVERS ---");
        
        if (servers.isEmpty()) {
            System.out.println("No active servers.");
            return;
        }
        
        System.out.println("┌──────┬──────────┬────────┬────────────┐");
        System.out.println("│  ID  │   Port   │  Map   │   Status   │");
        System.out.println("├──────┼──────────┼────────┼────────────┤");
        
        for (int i = 0; i < servers.size(); i++) {
            ShooterServer server = servers.get(i);
            System.out.printf("│  %-2d  │  %-6d  │   %-2d   │   Active   │%n", 
                i, server.getPort(), server.getMap());
        }
        
        System.out.println("└──────┴──────────┴────────┴────────────┘");
        System.out.println("Total: " + servers.size() + " server(s)");
    }
    
    private static void stopServer() {
        if (servers.isEmpty()) {
            System.out.println("\n[✗] No active servers to stop.");
            return;
        }
        
        listServers();
        System.out.print("\nEnter server ID to stop: ");
        String idInput = scanner.nextLine().trim();
        
        try {
            int id = Integer.parseInt(idInput);
            
            if (id < 0 || id >= servers.size()) {
                System.out.println("[✗] Invalid server ID!");
                return;
            }
            
            ShooterServer server = servers.get(id);
            int port = server.getPort();
            server.stop();
            servers.remove(id);
            System.out.println("[✓] Server on port " + port + " stopped successfully.");
            
        } catch (NumberFormatException e) {
            System.out.println("[✗] Invalid ID!");
        }
    }
    
    private static void stopAllServers() {
        if (servers.isEmpty()) {
            return;
        }
        
        System.out.println("\n[!] Stopping all servers...");
        int count = servers.size();
        
        for (ShooterServer server : servers) {
            server.stop();
        }
        
        servers.clear();
        System.out.println("[✓] " + count + " server(s) stopped successfully.");
    }
}

class ShooterServer {
    private ServerSocket serverSocket;
    private Socket client1, client2;
    private PrintWriter writer1, writer2;
    private BufferedReader reader1, reader2;
    private int port;
    private int map;
    private volatile boolean running = true;
    private Thread serverThread;

    public ShooterServer(int port, int map) throws IOException {
        this.port = port;
        this.map = map;
        this.serverSocket = new ServerSocket(port);

        serverThread = new Thread(() -> {
            try {
                System.out.println("[Server:" + port + "] Waiting for clients...");
                
                client1 = serverSocket.accept();
                writer1 = new PrintWriter(client1.getOutputStream(), true);
                reader1 = new BufferedReader(new InputStreamReader(client1.getInputStream()));
                String nickname1 = reader1.readLine();
                System.out.println("[Server:" + port + "] Client 1 connected: " + nickname1);
                String screenSize1 = reader1.readLine();
                String[] wh1 = screenSize1.split("x");
                int width1 = Integer.parseInt(wh1[0]);
                int height1 = Integer.parseInt(wh1[1]);
                GameMap gameMap1 = new GameMap(map, width1, height1);
                writer1.println("" + map);
                int[] pos1 = gameMap1.getStartingPosition();
                writer1.println(pos1[0] + ";" + pos1[1]);

                System.out.println("[Server:" + port + "] Waiting for second client...");
                client2 = serverSocket.accept();
                writer2 = new PrintWriter(client2.getOutputStream(), true);
                reader2 = new BufferedReader(new InputStreamReader(client2.getInputStream()));
                String nickname2 = reader2.readLine();
                System.out.println("[Server:" + port + "] Client 2 connected: " + nickname2);
                String screenSize2 = reader2.readLine();
                String[] wh2 = screenSize2.split("x");
                int width2 = Integer.parseInt(wh2[0]);
                int height2 = Integer.parseInt(wh2[1]);
                GameMap gameMap2 = new GameMap(map, width2, height2);
                writer2.println("" + map);
                int[] pos2 = gameMap2.getStartingPosition();
                writer2.println(pos2[0] + ";" + pos2[1]);

                writer1.println("START");
                writer2.println("START");
                System.out.println("[Server:" + port + "] Game started! (" + nickname1 + " vs " + nickname2 + ")");

                Thread t1 = new Thread(() -> passMessages(reader1, writer2, nickname1));
                Thread t2 = new Thread(() -> passMessages(reader2, writer1, nickname2));

                t1.setDaemon(true);
                t2.setDaemon(true);
                t1.start();
                t2.start();

                // Gestione connessioni extra
                while (running) {
                    try {
                        Socket extra = serverSocket.accept();
                        PrintWriter pw = new PrintWriter(extra.getOutputStream(), true);
                        pw.println("FULL");
                        pw.close();
                        extra.close();
                        System.out.println("[Server:" + port + "] Rejected extra connection (server full)");
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("[Server:" + port + "] Error accepting connection: " + e.getMessage());
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                if (running) {
                    System.err.println("[Server:" + port + "] Error: " + e.getMessage());
                }
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void passMessages(BufferedReader reader, PrintWriter writer, String clientName) {
        try {
            String message;
            while ((message = reader.readLine()) != null && running) {
                writer.println(message);
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[Server:" + port + "] " + clientName + " disconnected");
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (writer1 != null) writer1.close();
            if (writer2 != null) writer2.close();
            if (reader1 != null) reader1.close();
            if (reader2 != null) reader2.close();
            if (client1 != null && !client1.isClosed()) client1.close();
            if (client2 != null && !client2.isClosed()) client2.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            System.err.println("[Server:" + port + "] Error stopping: " + e.getMessage());
        }
    }
    
    public int getPort() {
        return port;
    }
    
    public int getMap() {
        return map;
    }
}
