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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private static final String SERVER_KEY = "123";

    private String storagePath = System.getProperty("user.dir") + "\\Storage\\";
    private PrintWriter logWriter;
    static volatile ConcurrentHashMap<String, PrintWriter> logWriters = new ConcurrentHashMap<>();

    static volatile ConcurrentHashMap<String, CopyOnWriteArrayList<String>> openedStations = new ConcurrentHashMap<>();
    HashMap<String, String> passwords = new HashMap<>();

    static volatile ConcurrentHashMap<String, Float> currentReading = new ConcurrentHashMap<>();
    static volatile ConcurrentHashMap<String, String> lastTimeStamp = new ConcurrentHashMap<>();
    static List<String> allStations;

    public BaseStationController(Socket sensorSocket) {
        this.sensorSocket = sensorSocket;
        try {
            out = new PrintWriter(sensorSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sensorSocket.getInputStream()));
            loadBaseStations();
            System.out.println("Conncetion Succesful With " + sensorSocket.getInetAddress());
        } catch (IOException ex) {
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
                    if (isSensorOpened(messageParameters[1], messageParameters[3])) {
                        output = "SENSOR_ALREADY_RUNNING";
                    } else {
                        String baseStationKey = messageParameters[1] + "_" + messageParameters[3];
                        try {
                            logWriters.putIfAbsent(baseStationKey, new PrintWriter(storagePath + "Readings_" + baseStationKey + ".txt"));
                        } catch (FileNotFoundException ex) {
                            System.err.println(ex.getMessage());
                        }
//                            new Thread(new BaseStationModel(
//                                    messageParameters[3], //sensor
//                                    messageParameters[1], //location
//                                    messageParameters[4], //unit
//                                    Float.parseFloat(messageParameters[5]), //min
//                                    Float.parseFloat(messageParameters[6]), //max
//                                    Integer.parseInt(messageParameters[7]), //interval
//                                    "localhost", 9001)
//                            ).start();
                        openedStations.get(messageParameters[1]).add(messageParameters[3]);
                        output = "LOGIN_VALIDATED";
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
                } catch (IOException e) {
                    output = e.getMessage();
                }
                break;
            case "NEW_BASE":
                if (validateServerKey(messageParameters[2])) {
                    try {
                        addBaseStation(messageParameters[1], "", messageParameters[3]);
                        output = "NEW_BASE_CREATED";
                    } catch (IOException ex) {
                        output = ex.getMessage();
                    }
                } else {
                    output = "BASE_CREATION_FAILED: INVALID_SERVER_KEY";
                }
                break;
            case "READING":
                updateCurrentReading(messageParameters[1], messageParameters[2], Float.parseFloat(messageParameters[4]));
                updateReadingTime(messageParameters[1], messageParameters[2], messageParameters[3]);
                 {
                    try {
                        writeLog(messageParameters[1], messageParameters[2], messageParameters[3], Float.parseFloat(messageParameters[4]));
                    } catch (FileNotFoundException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
                if (isWarning(messageParameters[2], Float.parseFloat(messageParameters[4]))) {
                    output = "READING_WARNING_" + messageParameters[3];
                } else {
                    output = "READING_ACK_" + messageParameters[3];
                }
                break;
            case "BASE_STATION_CLOSE":
                String baseStationKey = messageParameters[1] + "_" + messageParameters[2];
                removeSensor(messageParameters[1], messageParameters[2]);
                logWriters.get(baseStationKey).close();
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

    synchronized final void loadBaseStations() throws FileNotFoundException, IOException {
        baseStationsString = "";
        passwordString = "";
        String baseStation;

        FileReader stationsFile = new FileReader(storagePath + "\\BaseStationsList.txt");
        BufferedReader stationsFileReader = new BufferedReader(stationsFile);

        while ((baseStation = stationsFileReader.readLine()) != null) {
            String location = baseStation.split(":")[0];
            baseStationsString += location + ",";
            passwordString += baseStation.split(":")[1] + ",";
//            openedStations.put(location, fillerList);
            getList(location);
        }
        stationsFile.close();
        stationsFileReader.close();
        System.out.println("BASE STRING " + baseStationsString);
        System.out.println("PASS STRING " + passwordString);
    }

    String loadStationSensors(String baseStation) throws FileNotFoundException, IOException {
        String sensorsString = "";
        String sensor;

        FileReader sensorsFile = new FileReader(storagePath + "\\Sensors_" + baseStation + ".txt");
        BufferedReader sensorsFileReader = new BufferedReader(sensorsFile);

        while ((sensor = sensorsFileReader.readLine()) != null) {
            sensorsString += sensor + ",";
        }

        return sensorsString;
    }

    public static boolean validateServerKey(String input) {
        return SERVER_KEY.matches(input);
    }

    synchronized void addBaseStation(String location, String sensor, String password) throws FileNotFoundException, IOException {

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

    CopyOnWriteArrayList<String> getList(String key) {
        if (!openedStations.containsKey(key)) {
            openedStations.putIfAbsent(key, new CopyOnWriteArrayList<>());
        }
        return openedStations.get(key);
    }

    synchronized void updateCurrentReading(String location, String sensor, float reading) {
        String mapKey = location + "_" + sensor;
        if (currentReading.containsKey(mapKey)) {
            currentReading.replace(mapKey, reading);
        } else {
            currentReading.putIfAbsent(mapKey, reading);
        }
    }
    
    synchronized void updateReadingTime(String location, String sensor, String timestamp) {
        String mapKey = location + "_" + sensor;
        if (lastTimeStamp.containsKey(mapKey)) {
            lastTimeStamp.replace(mapKey, timestamp);
        } else {
            lastTimeStamp.putIfAbsent(mapKey, timestamp);
        }
    }

    public static float getCurrentReading(String location, String sensor) {
        String mapKey = location + "_" + sensor;
        if (currentReading.containsKey(mapKey) && currentReading.get(mapKey) != null) {
            return currentReading.get(mapKey);
        }
        return -99.0F;
    }
    
    public static String getTimeStamp(String location, String sensor){
        String mapKey = location + "_" + sensor;
        if (lastTimeStamp.containsKey(mapKey) && lastTimeStamp.get(mapKey) != null) {
            return lastTimeStamp.get(mapKey);
        }
        return "NOT_RUNNING";
    }

    boolean isWarning(String sensor, Float reading) {
        if (sensor.equals("Rainfall") && Float.compare(reading, 20.0F) > 0) {
            return true;
        }
        if (sensor.equals("Temparature") && ((Float.compare(reading, 35.0F) > 0) || (Float.compare(reading, 20.0F)) < 0)) {
            return true;
        }
        return false;
    }

    void writeLog(String location, String sensor, String time, float value) throws FileNotFoundException {
        String baseStationKey = location + "_" + sensor;
        logWriters.get(baseStationKey).println(time + " " + String.valueOf(value));
    }

    public static int getActiveLocationsCount() {
        int count = 0;
        Iterator locationIterator = openedStations.entrySet().iterator();
        while (locationIterator.hasNext()) {
            Entry temp = (Map.Entry) locationIterator.next();
            CopyOnWriteArrayList tempList = (CopyOnWriteArrayList) temp.getValue();
            if (tempList.size() > 0) {
                count++;
            }
        }
        return count;
    }

    public static int getActiveSensorsCount() {
        int count = 0;
        Iterator locationIterator = openedStations.entrySet().iterator();
        while (locationIterator.hasNext()) {
            Entry temp = (Map.Entry) locationIterator.next();
            CopyOnWriteArrayList tempList = (CopyOnWriteArrayList) temp.getValue();
            count = count + tempList.size();
        }
        return count;
    }

    public static List getAllLocations() {
        allStations = new ArrayList<>(openedStations.keySet());
        return allStations;
    }
}
