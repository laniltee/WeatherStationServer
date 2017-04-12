/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Lanil Marasinghe
 */
public class BaseStationController implements Runnable {

    private Socket sensorSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String baseStationsString = "";
    private String passwordString = "";
    private final String SERVER_KEY = "123";
    private JSONParser jsonParser;
    private String baseStationsFilePath = System.getProperty("user.dir") + "\\src\\BaseStationsList.json";
    private String storagePath = System.getProperty("user.dir") + "\\src\\Storage\\";

    static volatile ConcurrentHashMap<String, CopyOnWriteArrayList<String>> openedStations = new ConcurrentHashMap<>();
    HashMap<String, String> passwords = new HashMap<>();

    public BaseStationController(Socket sensorSocket) {
        jsonParser = new JSONParser();
        this.sensorSocket = sensorSocket;
        try {
            out = new PrintWriter(sensorSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sensorSocket.getInputStream()));
            loadBaseStations();
            System.out.println("Conncetion Succesful With " + sensorSocket.getInetAddress());
        } catch (IOException | ParseException ex) {
            System.err.println(ex.getMessage());
        }

    }

    @Override
    public void run() {
        try {
            while (true) {
                String messageIn = in.readLine();
                System.out.println("MESSAGE IN [" + sensorSocket.getInetAddress() + ":" + sensorSocket.getPort() + "]: " + messageIn);
                String messageOut = filterMessage(messageIn);
                out.println(messageOut);
                System.out.println("MESSAGE OUT [" + sensorSocket.getInetAddress() + ":" + sensorSocket.getPort() + "]: " + messageOut);
                if (messageIn.split("&")[0].equals("DISCONNECT")) {
                    break;
                }
            }

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        System.out.println("SERVER CLOSED CONNETION WITH " + sensorSocket.getInetAddress() + ":" + sensorSocket.getPort());
    }

    public String filterMessage(String messageQuery) {
        String messageParameters[] = messageQuery.split("&");
        String output = "";
        switch (messageParameters[0]) {
            case "GET_BASE_STATIONS":
                output = baseStationsString;
                break;
            case "CONNECT":
                output = "CONNECTED";
                break;
            case "DISCONNECT":
                output = "DISCONNETED";
                break;
            case "OPEN_BASE":
                if (validateLogin(messageParameters[1], messageParameters[2])) {
                    try {
                        if (isSensorOpened(messageParameters[1], messageParameters[3])) {
                            output = "SENSOR_ALREADY_RUNNING";
                        } else {
                            new Thread(new BaseStationModel(
                                    messageParameters[3], //sensor
                                    messageParameters[1], //location
                                    messageParameters[4], //unit
                                    Float.parseFloat(messageParameters[5]), //min
                                    Float.parseFloat(messageParameters[6]), //max
                                    Integer.parseInt(messageParameters[7]), //interval
                                    "localhost", 9001)
                            ).start();
                            openedStations.get(messageParameters[1]).add(messageParameters[3]);
                            output = "LOGIN_VALIDATED";
                        }
                    } catch (IOException ex) {
                        output = ex.getMessage();
                    }
                } else {
                    output = "LOGIN_FAILED";
                }
                break;
            case "OPEN_BASE_CONSOLE":
                output = "BASE_CONSOLE_OPENED";
                break;
            case "GET_SENSORS":
                try {
                    output = loadStationSensors(messageParameters[1]);
                } catch (IOException | ParseException e) {
                    output = e.getMessage();
                }
                break;
            case "NEW_BASE":
                if (validateServerKey(messageParameters[2])) {
                    try {
                        addBaseStation(messageParameters[1], messageParameters[3]);
                        output = "NEW_BASE_CREATED";
                    } catch (IOException | ParseException ex) {
                        output = ex.getMessage();
                    }
                } else {
                    output = "BASE_CREATION_FAILED: INVALID_SERVER_KEY";
                }
                break;
            case "READING":
                output = "READING_ACK_" + messageParameters[3];
                break;
            case "BASE_STATION_CLOSE":
                removeSensor(messageParameters[1], messageParameters[2]);
                output = "SENSOR_CLOSED&" + messageParameters[1] + "&" + messageParameters[2];
                break;
            default:
                output = "ACK: " + messageParameters[0];
                break;
        }
        return output;
    }

    boolean validateLogin(String username, String password) {
        String baseArray[] = baseStationsString.split(",");
        String passwordArray[] = passwordString.split(",");
        for (int i = 0; i < baseArray.length; i++) {
            if (baseArray[i].equals(username) && passwordArray[i].equals(password)) {
                return true;
            }
        }
        return false;
    }

    final void loadBaseStations() throws FileNotFoundException, IOException, ParseException {
        baseStationsString = "";
        passwordString = "";
        String baseStation;

        FileReader stationsFile = new FileReader(storagePath + "\\BaseStationsList.txt");
        BufferedReader stationsFileReader = new BufferedReader(stationsFile);

        while ((baseStation = stationsFileReader.readLine()) != null) {
            String location = baseStation.split(":")[0];
            baseStationsString += location + ",";
            passwordString += baseStation.split(":")[1] + ",";
            ArrayList<String> fillerList = new ArrayList<>();
//            openedStations.put(location, fillerList);
            getList(location);
        }
    }

    String loadStationSensors(String baseStation) throws FileNotFoundException, IOException, ParseException {
        String sensorsString = "";
        String sensor;

        FileReader sensorsFile = new FileReader(storagePath + "\\Sensors_" + baseStation + ".txt");
        BufferedReader sensorsFileReader = new BufferedReader(sensorsFile);

        while ((sensor = sensorsFileReader.readLine()) != null) {
            sensorsString += sensor + ",";
        }

        return sensorsString;
    }

    boolean validateServerKey(String input) {
        return SERVER_KEY.matches(input);
    }

    void addBaseStation(String location, String password) throws FileNotFoundException, IOException, ParseException {

    }

    synchronized boolean isSensorOpened(String location, String sensor) {
        return getList(location).contains(sensor);
    }

    synchronized void removeSensor(String location, String sensor) {
//        ArrayList<String> sensors = openedStations.get(location);
//        for (int i = 0; i < sensors.size(); i++) {
//            if (sensors.get(i).equals(sensor)) {
//                sensors.remove(i);
//                openedStations.replace(location, sensors);
//                break;
//            }
//        }
        getList(location).remove(sensor);
    }

    synchronized void addSensor(String location, String sensor) {
//        ArrayList<String> sensors = openedStations.get(location);
//        sensors.add(sensor);
//        openedStations.replace(location, sensors);
        getList(location).add(sensor);
    }
    
    CopyOnWriteArrayList<String> getList(String key){
        if(!openedStations.containsKey(key)){
            openedStations.putIfAbsent(key, new CopyOnWriteArrayList<>());
        }
        return openedStations.get(key);
    }
}
