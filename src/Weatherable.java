/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Lanil Marasinghe
 */
import java.rmi.*;
import java.util.List;

public interface Weatherable extends Remote {

    public float getCurrentReading(String location, String sensor) throws RemoteException;

    public int getActiveLocationsCount() throws RemoteException;

    public int getActiveSensorCount() throws RemoteException;

    public boolean validateLogin(String key) throws RemoteException;

    public List getLocations() throws RemoteException;

    public String getTimeStamp(String location, String sensor) throws RemoteException;

    public void addClient(Monitorable m) throws RemoteException;
    
    public void removeClient(Monitorable m) throws RemoteException;
    
    public void notifyMonitors(String location, String sensor, String timestamp, float reading);
    
    public void sendReadings();

}
