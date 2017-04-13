
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Lanil Marasinghe
 */
public class ServerInterface extends UnicastRemoteObject implements Weatherable{

    public ServerInterface() throws RemoteException {
        super();
    }
    
    @Override
    public float getCurrentReading(String location, String sensor) throws RemoteException {
        return BaseStationController.getCurrentReading(location, sensor);
    }
    
}
