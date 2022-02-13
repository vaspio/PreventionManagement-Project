import java.io.*;
import java.awt.*;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.*;
import java.net.URL;
import java.awt.image.BufferedImage;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.text.html.HTMLEditorKit;

import org.eclipse.paho.client.mqttv3.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.json.simple.*;
import org.json.simple.parser.*;
import javax.swing.*;

public class EdgeServer{
    
    // Keeping the mqtt client available globally
    static MqttAsyncClient sampleClient;
    static String current_topic;

    // Tracking Mqtt messaging statistics
    static int messages_sent = 0;
    static int messages_sent_succesfully = 0;
    static int messages_received = 0;
    static int messages_incoming_correct = 0;

    // Keeping track of android position
    static double android_position_x = 35.58;
    static double android_position_y = 28.12;
    
    // Limits and thresholds
    static double smoke_min_value = 0;
    static double smoke_max_value = 0.25;
    static double smoke_upper_threshold = 0.14;
    static double gas_min_value = 0;
    static double gas_max_value = 11;
    static double gas_upper_threshold = 1.0065; // 9.15%
    static double temperature_min_value = -5;
    static double temperature_max_value = 80;
    static double temperature_upper_threshold = 50;
    static double radiation_min_value = 0;
    static double radiation_max_value = 11;
    static double radiation_upper_threshold = 6;

    // Init server
    public static void main(String[] args) {
        // gui();

        connectToServer();
    }


    /*
    ** Show the GUI
    */
    public static void gui() {

        try{
            String url = "http://localhost:8080/";
            Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch(java.io.IOException ex){
            System.out.println(ex.getMessage());
        }

    }


    /*
    ** Set up MQTT Server
    */
    public static void connectToServer() {

        // Server settings
        String broker = "tcp://localhost:1883";
		String clientId = "JavaServer";
		MqttClientPersistence persistence = null;

		// Set the topics
        String edge_iot = "IoT/+";
        String edge_android = "Android/+";

        // Establish mqtt client
		try {

            // Settings for mqtt server
            sampleClient = new MqttAsyncClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setMaxInflight(3000);

            // useless?     probably
            connOpts.setUserName("emqx_test");
            connOpts.setPassword("emqx_test_password".toCharArray());

            // Set the callback for handling messaging
            sampleClient.setCallback(mqttCallback);

            // Actually set the connection up
            System.out.println("Connecting to broker: " + broker);
            (sampleClient.connect(connOpts)).waitForCompletion();
            System.out.println(" Connected ");

            // Subscribe to topics
            System.out.println("Subscribing to topics " + edge_android + " and " + edge_iot);
            sampleClient.subscribe(edge_iot, 0);
            sampleClient.subscribe(edge_android, 0);

            // Handle failure
            if(!sampleClient.isConnected()){
                System.out.println("Client is not connected!");
                System.exit(0);
            }

        }
        catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
        }
	}


    /*
    ** Callback functionality to handle incoming mqtt messages
    */
    static MqttCallback mqttCallback = new MqttCallback() {

        /*
        ** Connection has been lost
        ** 
        ** (e.g. the mosquitto service is stopped)
        */
        @Override
        public void connectionLost(Throwable connection) {
            System.out.println("\n\nConnection has been lost.");
        }

        /*
        ** Handle a message arriving
        */
        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws MqttException {
            
            // Get message payload
            String messageFromPublish = new String(mqttMessage.getPayload());
            System.out.println("Topic:" + topic + " Message:" + messageFromPublish);

            // Keep current topic
            current_topic = topic;
            
            // Statistics
            messages_received++;

            fnHandlePublish(messageFromPublish);
        }

        /*
        ** Handle the publishing/delivery process completing
        */
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                // System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
                if(token.getMessage() == null){
                    messages_sent_succesfully++;
                }
            }
            catch(MqttException exception) {
                System.out.println(exception.getMessage());
            }
        }

    };

    // Receiving
    static void fnHandlePublish(String messageFromPublish){

        // Information given through mqtt
        try {
            
            fnParseJsonFile(messageFromPublish);

            // double longitude = Double.parseDouble(messageFromPublish[0]);
            // double latitude = Double.parseDouble(messageFromPublish[1]);
            // double battery = Double.parseDouble(messageFromPublish[2]);
            // double current_smoke = Double.parseDouble(messageFromPublish[3]);
            // double current_gas = Double.parseDouble(messageFromPublish[4]);
            // double current_temperature = Double.parseDouble(messageFromPublish[5]);
            // double current_radiation = Double.parseDouble(messageFromPublish[6]);

            // String timestamp = fnGetTimestamp();
            // double distance_from_android = fnCalculateDistanceFromAndroid(longitude, latitude);
            // int danger_level = fnComputeDangerLevel(current_smoke, current_gas, current_temperature, current_radiation);

            // // Contruct SQL query
            // String sqlInsert = "INSERT INTO events VALUES(NULL, " + timestamp + ", " + longitude + ", " + latitude + ", " + danger_level + ", " + distance_from_android + ", " + battery + ", " + current_smoke + ", " + current_gas + ", " + current_temperature + ", " + current_radiation + ")";

            // // Create connection with DB and execute query
            // Connection conn = fnGetDatabaseConnection();
            // try {
            //     PreparedStatement statement = conn.prepareStatement(sqlInsert);
            //     int row = statement.executeUpdate();
            //     System.out.println("mySQL affected rows" + row);
            // }
            // catch(SQLException ex){
            //     System.out.println(ex.getMessage());
            // }
        }
        catch(Exception e){
            /*
            ** Handle published messages which were not in the expected format
            */
            System.out.println("The message is in a format not expected and was not accepted.");
            //int successful_percentage = messages_incoming_correct * 100 / messages_received;
            //System.out.println("So far we have received " + messages_received + " messages, while " + successful_percentage + " % of them were properly formatted.");
        }
    }


    /********************
    ** ASSET FUNCTIONS **
    ********************/
    
    /*
    ** Get connection with DB
    */
    public static Connection fnGetDatabaseConnection(){

        Connection conn = null;
        
        try {
            String driver = "com.mysql.jdbc.Driver";
            String url = "jdbc:mysql://localhost:3306/preventiondb?characterEncoding=latin1";
            String user = "root";
            String password = "Project123!";

            conn = DriverManager.getConnection(url, user, password);   
        } 
        catch(SQLException e){
            System.out.println("\nCan't establish connection!");
            System.out.println(e.getMessage());
        }

        return conn;

    }


    /*
    ** Get current date formatted for mysql
    */
    static String fnGetTimestamp(){
        long millisNow = System.currentTimeMillis();
        SimpleDateFormat timestamp2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date result = new Date(millisNow);
        String timestamp = "'" + timestamp2.format(result) + "'";
        return timestamp;
    }


    /*
    ** Calculate distance between 2 spots on the map
    */ 
    static Double fnCalculateDistanceFromAndroid(Double x, Double y){
        
        // Reference: https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude

        /*
        ** Initial implementation takes height differences into account yet this is not provided here
        ** It is suggested to pass 0.00 if height is deemed inconsequencial, and such is the case below
        ** @returns Distance in Meters
        */
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(android_position_x - x);
        double lonDistance = Math.toRadians(android_position_y - y);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(android_position_y)) * Math.cos(Math.toRadians(android_position_x))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // convert to meters
        double distance = R * c * 1000; 

        double height = 0.00;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);

    }


    /*
    ** Compute danger level based on measurements
    */
    static int fnComputeDangerLevel(Double current_smoke, Double current_gas, Double current_temperature, Double current_radiation){

        // Values over threshold
        boolean smoke_over_threshold = false;
        if(current_smoke > smoke_upper_threshold) {
            smoke_over_threshold = true;
        }
        boolean gas_over_threshold = false;
        if(current_gas > gas_upper_threshold) {
            gas_over_threshold = true;
        }
        boolean temperature_over_threshold = false;
        if(current_temperature > temperature_upper_threshold) {
            temperature_over_threshold = true;
        }
        boolean radiation_over_threshold = false;
        if(current_radiation > radiation_upper_threshold) {
            radiation_over_threshold = true;
        }

        // Calculate danger level based on the sensor measurements
        int danger_level = 0;
        if(smoke_over_threshold && gas_over_threshold){
            danger_level = 2;
        } else if(!smoke_over_threshold && !gas_over_threshold && temperature_over_threshold && radiation_over_threshold){
            danger_level = 1;
        } else if(gas_over_threshold && !smoke_over_threshold && !temperature_over_threshold && !radiation_over_threshold){
            danger_level = 2;
        } else if(smoke_over_threshold && gas_over_threshold && temperature_over_threshold && radiation_over_threshold){
            danger_level = 2;
        }

        System.out.println("Danger level: " + danger_level + " . Computed from smoke " + current_smoke + ", gas " + current_gas + ", temperature " + current_temperature + ", radiation " + current_radiation);

        return danger_level;

    }

    /*
    ** Get last entry from database
    */
    static float[] fnGetLastEntry(){
        float longitude = 0;
        float latitude = 0;

        try{
            Connection con = fnGetDatabaseConnection();

            Statement state = con.createStatement();
            ResultSet results = state.executeQuery("select * from devices");

            while(results.next()){
                latitude = results.getFloat(4);
                longitude = results.getFloat(5);
            }
        }
        catch(SQLException e){
            System.out.println("\nFailed to execute query..");
            System.out.println(e.getMessage());
        }

        System.out.println("Last Entry:" + latitude + ", " + longitude);

        float[] pos = {latitude,longitude};
        return pos;
    }


    /*
    ** Parse the JSON
    */
    static void fnParseJsonFile(String message){

        JSONParser parser = new JSONParser();
        String messageTemp = message;

        try {

            JSONObject jsonObject = (JSONObject) parser.parse(messageTemp);            

            // Get the measurements array and go through it
            JSONArray tempParsedArray = (JSONArray) jsonObject.get("data");


            // Get the device related info
            String tempParsedLatitude = jsonObject.get("Latitude").toString();
            double latitude = Double.parseDouble(tempParsedLatitude);

            String tempParsedLongitude = jsonObject.get("Longitude").toString();
            double longitude = Double.parseDouble(tempParsedLongitude);

            String tempParseBattery = jsonObject.get("Battery").toString();
            double battery = Double.parseDouble(tempParseBattery);

            String tempParsedDeviceId = jsonObject.get("DeviceID").toString();
            tempParsedDeviceId = "\"" + tempParsedDeviceId + "\"";

            // Set device type for the db
            String deviceType;
            if(current_topic.startsWith("Android")){
                deviceType = "android";
            }
            else{
                deviceType = "iot";
            }
            deviceType = "\"" + deviceType + "\"";



            // Parse events
            Iterator<?> iterator = tempParsedArray.iterator();
            
            String arrayParsedType, arrayParsedNumber, arrayParsedValue;
            double maxSmoke = -1.0;
            double maxGas = -1.0;
            double maxTemperature = -1.0;
            double maxRadiation = -1.0;

            while(iterator.hasNext()) {

                Object object = iterator.next();
                JSONObject objectJson = (JSONObject) object;
                System.out.println(object);
                // System.out.println(object.getClass().getName());

                arrayParsedType = objectJson.get("Sensor Type").toString();
                arrayParsedNumber = objectJson.get("Sensor Number").toString();
                arrayParsedValue = objectJson.get("Sensor Value").toString();
                // System.out.println(arrayParsedType);
                double arrayParsedValueDouble = Double.parseDouble(arrayParsedValue);
                // System.out.println(arrayParsedValueDouble);

                // Calculate how the measurement affects current standings
                if(arrayParsedType.equals("Smoke Sensor")){
                    if(arrayParsedValueDouble > maxSmoke){
                        maxSmoke = arrayParsedValueDouble;
                    }
                } else if(arrayParsedType.equals("Gas Sensor")){
                    // System.out.println(arrayParsedValueDouble);
                    if(arrayParsedValueDouble > maxGas){
                        maxGas = arrayParsedValueDouble;
                    }
                } else if(arrayParsedType.equals("Temperature Sensor")){
                    // System.out.println(arrayParsedValueDouble);
                    if(arrayParsedValueDouble > maxTemperature){
                        maxTemperature = arrayParsedValueDouble;
                    }
                } else if(arrayParsedType.equals("Radiation Sensor")){
                    // System.out.println(arrayParsedValueDouble);
                    if(arrayParsedValueDouble > maxRadiation){
                        maxRadiation = arrayParsedValueDouble;
                    }
                }

                // Prepare values for mysql
                arrayParsedType = "\"" + arrayParsedType + "\"";
                arrayParsedNumber = "\"" + arrayParsedNumber + "\"";

                // Contruct SQL query
                String timestamp = fnGetTimestamp();
                String insertEventQuery = "INSERT INTO events VALUES(NULL, " + timestamp + ", " + arrayParsedType + ", " + arrayParsedValueDouble + ", " + arrayParsedNumber + ", " + tempParsedDeviceId + ")";
                executeVoidSqlQuery(insertEventQuery);
            }

            int danger_level = fnComputeDangerLevel(maxSmoke, maxGas, maxTemperature, maxRadiation);
            if(danger_level != 0){
                fnAlertAboutDangerLevel(danger_level);
            }

            // Save new device information
            String insertDeviceQuery = "INSERT INTO devices VALUES(NULL, " + tempParsedDeviceId + ", " + deviceType + ", " + latitude + ", " + longitude + ", " + danger_level + ", " + battery + ") ON DUPLICATE KEY UPDATE latitude=" + latitude + ", longitude=" + longitude + ", danger_level=" + danger_level + ", battery=" + battery;
            executeVoidSqlQuery(insertDeviceQuery);


        } catch(ParseException pe) {

            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        }
    }


    /*
    ** Execute SQL insert query
    */
    static void executeVoidSqlQuery(String query){

        Connection conn = fnGetDatabaseConnection();

        System.out.println(query);

        try {
            PreparedStatement statement = conn.prepareStatement(query);
            statement.executeUpdate();

            // System.out.println("Executed query");
        }
        catch(SQLException ex){
            System.out.println(ex.getMessage());
        }

    }

    
    /*
    ** Alert users for danger levels
    */
    static void fnAlertAboutDangerLevel(int danger_level){
    
        String publishMessageString = "The danger levels in the area are abnormal. Current danger level estimated: " + danger_level + " . Please get to one of the designated safe zones as soon as possible.";
        MqttMessage publishMqttMessage = new MqttMessage();
        publishMqttMessage.setPayload(publishMessageString.getBytes());

        try {
            System.out.println("Before publishing danger");
            sampleClient.publish("Android/", publishMqttMessage);
            System.out.println("After publishing danger");
        }
        catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
        }
    }

}
