
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.*;

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
        

        try {
            Weatherable interfaceSkeleton = new ServerInterface();
            Naming.rebind("rmi://localhost/weatherservice", interfaceSkeleton);
            System.out.println("Weather Service Rebinded On localhost:1099 !");
        } catch (RemoteException ex) {
            System.err.println(ex.getMessage());
        } catch (MalformedURLException ex) {
            System.err.println(ex.getMessage());
        }

        final int SERVER_PORT = 9001;

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server Socket UP !");
            while (true) {
                Socket baseStationSocket = serverSocket.accept();
                new Thread(new BaseStationController(baseStationSocket)).start();
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

    }

}
