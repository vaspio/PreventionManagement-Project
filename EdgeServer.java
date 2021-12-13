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


import org.eclipse.paho.client.mqttv3.*;

public class EdgeServer {
    static int messages_completed = 0;

	// Init server
	public static void main(String[] args) {
		connectToServer();
	}

    public static Connection getConn(){
        Connection conn = null;
        try{

            /* db parameters */
            String driver = "com.mysql.jdbc.Driver";
            String url = "jdbc:mysql://localhost:3306/preventiondb?characterEncoding=latin1";
            String user = "root";
            String password = "Project123!";

            /* connection */
            conn = DriverManager.getConnection(url, user, password);
            
        } 
        catch(SQLException e){
            System.out.println(e.getMessage());
        }

        return conn;
    }


    public static void connectToServer() {

		System.out.println("Setting the Server info...");
		System.out.println("Setting Broker IP...");
		String broker = "tcp://localhost:1883";
		System.out.println("Setting client ID for Mqtt client...");
		String clientId = "JavaServer";
		MqttClientPersistence persistence = null;

		String serverUrl;
		MqttAsyncClient sampleClient;

		// topics
		System.out.println("Setting the topics...");
		String publishTopicAndroid = "pub_android";
		String publishTopicIot = "pub_iot";
		String subscribeTopicAndroid = "sub_android";
		String subscribeTopicIot = "sub_iot";
        System.out.println("Setting topics for the android and iot to subscribe and publish to...");

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


        /*************
        ** DATABASE **
        *************/
        
        Connection conn = getConn();
        System.out.println("Connection to Database has been successful!");

        // finally{
        //     try {
        //         if(conn != null){
        //             conn.close();
        //         }
        //     } 
        //     catch(SQLException ex){
        //         System.out.println(ex.getMessage());
        //     }
        // }

	}


    static MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable connection) {
            System.out.println("\n\nConnection lost..");
            return;
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws MqttException {
            String message = new String(mqttMessage.getPayload());
            System.out.println("Topic:" + topic + " Message:" + message + " qos " + mqttMessage.getQos());

            String messageFromPublish = "30.01|29.99|68.86|0.15|1.2|55|7";
 
            String[] words = messageFromPublish.split("\\|"); 
            for(int i=0;i<words.length;i++){
                System.out.println(words[i]);    
            }  

            //handle the message
            fnHandlePublish(words);

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                // System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
                if(token.getMessage()==null){
                    messages_completed++;
                    System.out.println("Messages completed:" + messages_completed);
                }
            }
            catch(MqttException exception){
                exception.getMessage();
            }
        }
    };

    static void fnHandlePublish(String[] messageFromPublish){

        // CREATE TIMESTAMP
        long millisNow = System.currentTimeMillis();
        SimpleDateFormat timestamp2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date result = new Date(millisNow);
        String timestamp = "'" + timestamp2.format(result) + "'";

        // Information given through mqtt
        double longitude = 10.01;
        double latitude = 9.9;
        int danger_level = 1;            
        int battery = 68;
        int sensor_type = 0;
        double sensor_measurement = 89.71;

        // Calculate danger level based on the type of sensor
        //int danger_level;

        // Limits and thresholds
        double smoke_min_value = 0;
        double smoke_max_value = 0.25;
        double smoke_upper_threshold = 0.14;
        double gas_min_value = 0;
        double gas_max_value = 11;
        double gas_upper_threshold = 1.0065; // 9.15%
        double temperature_min_value = -5;
        double temperature_max_value = 80;
        double temperature_upper_threshold = 50;
        double radiation_min_value = 0;
        double radiation_max_value = 11;
        double radiation_upper_threshold = 6;

        // Current values
        double current_smoke = 0.15;
        double current_gas = 1.2;
        double current_temperature = 55;
        double current_radiation = 7;

        // Values over threshhold
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

        // Smoke + Gas
        

        /* Calculate distance from the android device */
        double distance_from_android = 1234.56;
        
        String sqlInsert = "INSERT INTO events VALUES(NULL, ?, longitude, latitude, danger_level, distance_from_android, battery, sensor_type, sensor_measurement)";
        sqlInsert = "INSERT INTO events VALUES(NULL, "+timestamp+", "+longitude+", "+latitude+", "+danger_level+", "+distance_from_android+", "+battery+", "+sensor_type+", "+sensor_measurement+")";

        Connection conn = getConn();
        try {
            PreparedStatement statement = conn.prepareStatement(sqlInsert);
            //statement.setString(1, timestamp);
            int row = statement.executeUpdate();
            System.out.println(row);
        } catch(SQLException ex){
            System.out.println(ex.getMessage());
        } finally {
            // System.out.println("Finally closing connection to DB");            
            // conn.close();
        }

        //danger_level = 1;
        //distance_from_android = 1234.56;


    }

	// void messageArrived(String topic, MqttMessage message) throws MqttException {
	// 	System.out.println(String.format("[%s] %s", topic, new String(message.getPayload())));
	// 	System.out.println("\tMessage published on topic 'Area1'");
	// }
}

