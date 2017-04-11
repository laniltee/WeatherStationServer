
import java.awt.Container;
import java.awt.LayoutManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import javax.swing.JFrame;
import javax.swing.JLabel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Lanil Marasinghe
 */
public class BaseStationModel implements Runnable {

    String sensor, location, measuringUnit;
    Float minReading, maxReading;
    int refreshInterval;

    Socket controllerSocket;

    private PrintWriter out;
    private BufferedReader in;

    Random randGen;

    JFrame frame = new JFrame("Sensor Viewer");
    JLabel lblSensor = new JLabel("Sensor: ");
    JLabel lblLocation = new JLabel("Location: ");
    JLabel lblReading = new JLabel("Reading: ");

    public BaseStationModel(
            String sensor,
            String location,
            String measuringUnit,
            Float minReading,
            Float maxReading,
            int refreshInterval,
            String serverHost,
            int serverPort) throws IOException {

        this.sensor = sensor;
        this.location = location;
        this.measuringUnit = measuringUnit;
        this.minReading = minReading;
        this.maxReading = maxReading;
        this.refreshInterval = refreshInterval;

        randGen = new Random();

        controllerSocket = new Socket(serverHost, serverPort);
        out = new PrintWriter(controllerSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(controllerSocket.getInputStream()));

        out.println("New Sensor Opened_" + sensor + "_" + location);

        frame.getContentPane().add(lblSensor, "North");
        frame.getContentPane().add(lblLocation, "Center");
        frame.getContentPane().add(lblReading, "South");
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    @Override
    public void run() {
        this.lblLocation.setText("Location: " + location);
        this.lblSensor.setText("Sensor: " + sensor);
        while (true) {
            String timeStamp = getDateTime();
            float reading = getReading();
            out.println("READING&" + location + "&" + sensor + "&" + timeStamp + "&" + reading);
            this.lblReading.setText("Reading: " + reading);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                out.println(ex.getMessage());
            }
        }
    }

    String getDateTime() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    float getReading() {
        float finalX = randGen.nextFloat() * (maxReading - minReading) + minReading;
        return finalX;
    }
}
