/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
    private String baseStationsString = "Kandy,Peradeniya,Watawala,Hikkaduwa,Kurunegala";
    private String passwordString = "";
    private final String SERVER_KEY = "123";
    private JSONParser jsonParser;
    private String baseStationsFilePath = System.getProperty("user.dir") + "\\src\\BaseStationsList.json";

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
                    output = "LOGIN_VALIDATED";
                    try {
                        new Thread(new BaseStationModel(
                                messageParameters[3], //sensor
                                messageParameters[1], //location
                                messageParameters[4], //unit
                                Float.parseFloat(messageParameters[5]), //min
                                Float.parseFloat(messageParameters[6]), //max
                                Integer.parseInt(messageParameters[7]), //interval
                                "localhost", 9001)
                        ).start();           
                        
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
        Object baseStationsFile = jsonParser.parse(new FileReader(System.getProperty("user.dir") + "\\src\\BaseStationsList.json"));
        JSONArray baseStationsArray = (JSONArray) baseStationsFile;
        Iterator stationsIterator = baseStationsArray.iterator();
        while (stationsIterator.hasNext()) {
            JSONObject baseStation = (JSONObject) stationsIterator.next();
            baseStationsString += baseStation.get("location") + ",";
            passwordString += baseStation.get("password") + ",";
        }
    }

    String loadStationSensors(String baseStation) throws FileNotFoundException, IOException, ParseException {
        String sensorsString = "";
        Object sensorsFile = jsonParser.parse(new FileReader(System.getProperty("user.dir") + "\\src\\Sensors_" + baseStation + ".json"));
        JSONArray sensorsArray = (JSONArray) sensorsFile;
        Iterator sensorsIterator = sensorsArray.iterator();
        while (sensorsIterator.hasNext()) {
            String sensor = sensorsIterator.next().toString();
            sensorsString += sensor + ",";
        }
        return sensorsString;
    }

    boolean validateServerKey(String input) {
        return SERVER_KEY.matches(input);
    }

    void addBaseStation(String location, String password) throws FileNotFoundException, IOException, ParseException {
        JSONObject baseStation = new JSONObject();
        baseStation.put("location", location);
        baseStation.put("password", password);

        Object baseStationsFile = jsonParser.parse(new FileReader(baseStationsFilePath));
        JSONArray baseStationsArray = (JSONArray) baseStationsFile;
        baseStationsArray.add(baseStation);

        FileWriter fileWriter = new FileWriter(baseStationsFilePath);
        fileWriter.write(baseStationsArray.toJSONString());
        fileWriter.flush();
    }
}
