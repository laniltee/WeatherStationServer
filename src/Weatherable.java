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

public interface Weatherable extends Remote{
    public float getCurrentReading(String location, String sensor) throws RemoteException;
}
