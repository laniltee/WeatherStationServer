
import java.net.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Lanil Marasinghe
 */
public class ServerCLI {

    public static void main(String[] args) {
        final String SERVER_KEY = "server123";
        final int SERVER_PORT = 9001;
      
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server Socket UP !");
            while(true){
                Socket baseStationSocket = serverSocket.accept();
                new Thread(new BaseStationController(baseStationSocket)).start();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

    }
    
}
