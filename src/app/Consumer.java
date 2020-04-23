package app;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

public class Consumer {

    private static final String REQUEST_QUEUE_NAME = "request_queue";
    private static final String RESPONSE_QUEUE_NAME = "response_queue";

    private static String loc;

    public static void main(String[] argv) throws Exception {

        File dir = new File(".");

        loc = dir.getCanonicalPath() + File.separator + "record.txt";
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(REQUEST_QUEUE_NAME, true, false, false, null);
        channel.queueDeclare(RESPONSE_QUEUE_NAME, true, false, false, null);

        recvMessage(channel);

    }

    public static void sendMessage(Channel channel, String message) throws IOException {

        channel.basicPublish("", RESPONSE_QUEUE_NAME,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes(StandardCharsets.UTF_8));
            
    }

    public static void recvMessage(Channel channel) throws IOException {

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
          
            System.out.println(" [x] Received '" + message + "'");
            try {
                doWork(channel, message);
            } finally {
                System.out.println(" [x] Done");
            }
        };

        boolean autoAck = true; // acknowledgment is covered below
        channel.basicConsume(REQUEST_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
    }

    private static void doWork(Channel channel, String URL) throws IOException {

        File file = new File(loc);
        Document doc = null;

        try {

            doc = Jsoup.connect(URL)
                        .timeout(10 * 1000)
                        .get();

        } catch (IOException e) {
            return;
        }

        Elements questions = doc.select("a[href]");

        for (Element link : questions) {

            String message = link.attr("abs:href");

            if (!message.equals("") && checkBaseURL(URL, message)) {

                if (!checkExist(message, file)) {
    
                    // insert to file
    
                    FileWriter fstream = new FileWriter(loc, true);
                    BufferedWriter out = new BufferedWriter(fstream);
    
                    out.write(message);
                    out.newLine();
    
                    out.close();
                    fstream.close();
    
                    sendMessage(channel, message);
    
                }

            }

        }

    }

    public static boolean checkExist(String s, File fin) throws IOException {

        FileInputStream fis = new FileInputStream(fin);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        String aLine = null;

        while ((aLine = in.readLine()) != null) {

            if (aLine.trim().contains(s)) {

                in.close();
                fis.close();

                return true;

            }

        }
        // do not forget to close the buffer reader

        in.close();
        fis.close();

        return false;

    }
    
    private static boolean checkBaseURL(String URL, String URLCheck) {

        String baseURL = "";
        String baseURLCheck = "";

        try {
            baseURL = URL.split("/")[2];
            baseURLCheck = URLCheck.split("/")[2];
        } catch(Exception e){ };

        if (baseURL.equals(baseURLCheck)) return true;
        
        return false;

    }

}