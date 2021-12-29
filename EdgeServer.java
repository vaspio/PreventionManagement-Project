import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;
import java.util.Properties;
import javax.xml.parsers.*;

import org.eclipse.paho.client.mqttv3.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;


public class EdgeServer {
    
    // Tracking Mqtt messaging statistics
    static int messages_received = 0;
    static int messages_completed = 0;

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
        fnParseXMLFile("android_1.xml");
		connectToServer();
	}


    public static void connectToServer() {

        // Server settings
        String broker = "tcp://localhost:1883";
		String clientId = "JavaServer";
		MqttClientPersistence persistence = null;
		MqttAsyncClient sampleClient;

		// Topics
		String publishTopicAndroid = "pub_android";
		String publishTopicIot = "pub_iot";
		String subscribeTopicAndroid = "sub_android";
		String subscribeTopicIot = "sub_iot";

        // Establish mqtt client
		try {
			sampleClient = new MqttAsyncClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setMaxInflight(3000);

            connOpts.setUserName("emqx_test");
            connOpts.setPassword("emqx_test_password".toCharArray());

            // callback
            sampleClient.setCallback(mqttCallback);


            System.out.println("Connecting to broker: " + broker);
            (sampleClient.connect(connOpts)).waitForCompletion();
            System.out.println(" Connected ");

            // int[] qualities = {0, 0};

            sampleClient.subscribe(subscribeTopicAndroid, 0);
            sampleClient.subscribe(subscribeTopicIot, 0);
            
            if(!sampleClient.isConnected()){
                //bad
            }

            String set_pub = "mhnuma gia android";
            MqttMessage pub_message = new MqttMessage();
            pub_message.setPayload(set_pub.getBytes());

            sampleClient.publish(publishTopicAndroid, pub_message);
            //System.out.println("subscribing to topics " + subTopics[0] + " and " + subTopics[1]);
            //System.out.println("publishing to topic " + pubTopic1);
            //System.out.println("publishing to topic " + pubTopic2);
        }
        catch (MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            return ;
        }        
	}


    // Callback functionality to handle incoming mqtt messages
    static MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable connection) {
            System.out.println("\n\nConnection lost..");
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws MqttException {
            // Get message payload
            String messageFromPublish = new String(mqttMessage.getPayload());
            System.out.println("Topic:" + topic + " Message:" + messageFromPublish);
            messages_received++;

            // Split info in an array call the handler function
            String[] messageFromPublishArray = messageFromPublish.split("\\|");
            fnHandlePublish(messageFromPublishArray);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                // System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
                if(token.getMessage() == null){
                    messages_completed++;
                    System.out.println("Messages completed:" + messages_completed);
                }
            }
            catch(MqttException exception) {
                exception.getMessage();
            }
        }
    };

    // Receiving
    static void fnHandlePublish(String[] messageFromPublish){

        // Information given through mqtt
        try{
            double longitude = Double.parseDouble(messageFromPublish[0]);
            double latitude = Double.parseDouble(messageFromPublish[1]);
            double battery = Double.parseDouble(messageFromPublish[2]);
            double current_smoke = Double.parseDouble(messageFromPublish[3]);
            double current_gas = Double.parseDouble(messageFromPublish[4]);
            double current_temperature = Double.parseDouble(messageFromPublish[5]);
            double current_radiation = Double.parseDouble(messageFromPublish[6]);

            String timestamp = fnGetTimestamp();
            double distance_from_android = fnCalculateDistanceFromAndroid(longitude, latitude);
            int danger_level = fnComputeDangerLevel(current_smoke, current_gas, current_temperature, current_radiation);

            // Contruct SQL query
            String sqlInsert = "INSERT INTO events VALUES(NULL, " + timestamp + ", " + longitude + ", " + latitude + ", " + danger_level + ", " + distance_from_android + ", " + battery + ", " + current_smoke + ", " + current_gas + ", " + current_temperature + ", " + current_radiation + ")";

            // Create connection with DB and execute query
            Connection conn = fnGetDatabaseConnection();
            try {
                PreparedStatement statement = conn.prepareStatement(sqlInsert);
                int row = statement.executeUpdate();
                System.out.println("mySQL affected rows" + row);
            }
            catch(SQLException ex){
                System.out.println(ex.getMessage());
            }
        }
        catch(Exception e){
            // handle wrong published messages
            System.out.println("The recieved message is in the wrong format.");
        }
    }


    /********************
    ** ASSET FUNCTIONS **
    ********************/
    // Get a connection with the DB
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
            System.out.println(e.getMessage());
        }

        return conn;
    }


    // Get current date formatted for mysql
    static String fnGetTimestamp(){
        long millisNow = System.currentTimeMillis();
        SimpleDateFormat timestamp2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date result = new Date(millisNow);
        String timestamp = "'" + timestamp2.format(result) + "'";
        return timestamp;
    }


    // Calculate distance of device from the android
    static Double fnCalculateDistanceFromAndroid(Double x, Double y){
        //https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
        
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


    // Compute danger level
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

        return danger_level;
    }


    // Parse xml
    static void fnParseXMLFile(String fileName){

        File inputFile = new File(fileName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document doc = builder.parse(inputFile);
                doc.getDocumentElement().normalize();
                System.out.println("root element : " + doc.getDocumentElement().getNodeName());
                NodeList nList = doc.getElementsByTagName("timestamp");
                System.out.println("-------------------------------");

                for(int temp = 0; temp < nList.getLength(); temp++){
                    Node nNode = nList.item(temp);
                    System.out.println("\nCurrent Element :" + nNode.getNodeName());

                    if(nNode.getNodeType() == Node.ELEMENT_NODE){
                        Element eElement = (Element) nNode;
                        System.out.println("timestamp : " + eElement.getAttribute("vehicle"));
                        System.out.println("timestamp : " + eElement.getAttribute("id"));
                    }
                }
            }
            catch(SAXException ex){
                System.out.println(ex.getMessage());
            }
            catch(IOException IOEx){
                System.out.println(IOEx.getMessage());
            }
        }
        catch(ParserConfigurationException e){
            System.out.println(e.getMessage());
        }
    }
}