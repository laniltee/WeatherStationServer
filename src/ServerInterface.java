
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Lanil Marasinghe
 */
public class ServerInterface extends UnicastRemoteObject implements Weatherable {

    List<Monitorable> monitors;
    private static ServerInterface singleInstance;

    private ServerInterface() throws RemoteException {
        super();
        monitors = new ArrayList<>();
    }
    
    synchronized public static ServerInterface getObject() throws RemoteException{
        if(singleInstance == null){
            singleInstance = new ServerInterface();
        }
        return singleInstance;
    }

    @Override
    public float getCurrentReading(String location, String sensor) throws RemoteException {
        return BaseStationController.getCurrentReading(location, sensor);
    }

    @Override
    public int getActiveLocationsCount() throws RemoteException {
        return BaseStationController.getActiveLocationsCount();
    }

    @Override
    public int getActiveSensorCount() throws RemoteException {
        return BaseStationController.getActiveSensorsCount();
    }

    @Override
    public boolean validateLogin(String key) throws RemoteException {
        return BaseStationController.validateServerKey(key);
    }

    @Override
    public List getLocations() throws RemoteException {
        return BaseStationController.getAllLocations();
    }

    @Override
    public String getTimeStamp(String location, String sensor) throws RemoteException {
        return BaseStationController.getTimeStamp(location, sensor);
    }

    @Override
    public void addClient(Monitorable m) throws RemoteException {
        monitors.add(m);
    }

    @Override
    public void removeClient(Monitorable m) throws RemoteException {
        monitors.remove(m);
    }

    @Override
    public void notifyMonitors(String location, String sensor, String timestamp, float reading) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendReadings() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
