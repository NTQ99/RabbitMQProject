package app;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

public class Consumer {

    private static final String REQUEST_QUEUE_NAME = "request_queue";
    private static final String RESPONSE_QUEUE_NAME = "response_queue";
    private static final String HANDLE_QUEUE_NAME = "handle_queue";

    private static String recordFile = "record.txt";

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(REQUEST_QUEUE_NAME, true, false, false, null);
        channel.queueDeclare(RESPONSE_QUEUE_NAME, true, false, false, null);
        channel.queueDeclare(HANDLE_QUEUE_NAME, true, false, false, null);

        recvMessage(channel);

    }

    public static void sendMessage(Channel channel, String message) throws IOException {

        channel.basicPublish("", RESPONSE_QUEUE_NAME,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes(StandardCharsets.UTF_8));
            
    }

    public static void sendHandleMessage(Channel channel, String message) throws IOException {

        channel.basicPublish("", HANDLE_QUEUE_NAME,
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

    public static void doWork(Channel channel, String URL) throws IOException {

        Document doc = null;

        try {

            doc = Jsoup.connect(URL)
                        .timeout(10 * 1000)
                        .get();

        } catch (IOException e) {
            return;
        }

        if (checkTitle(doc.baseUri())) {

            sendHandleMessage(channel, URL);
            return;

        }

        Elements questions = doc.select("a[href]");
        for (Element link : questions) {

            String message = link.attr("abs:href");
            if (!message.equals("") && checkBaseURL(URL, message) ) {

                message = validateUrl(message);

                if (!FileHandle.checkExist(recordFile, message)) {
    
                    // insert to file
                    FileHandle.write(recordFile, message, true);
    
                    sendMessage(channel, message);
    
                }
            }
        }
    }
    
    public static boolean checkBaseURL(String URL, String URLCheck) {

        String baseURL = "";
        String baseURLCheck = "";

        try {
            baseURL = URL.split("/")[2];
            baseURLCheck = URLCheck.split("/")[2];
        } catch(Exception e){ };

        if (baseURL.equals(baseURLCheck)) return true;
        
        return false;

    }

    public static boolean checkTitle(String title) {
        
        if (title.contains("preset")) return true;
        if (title.contains("lightroom")) return true;
        if (title.contains("font")) return true;
        if (title.contains("typo")) return true;
        if (title.contains("tao-dang")) return true;
        if (title.contains("tu-the")) return true;

        return false;

    }

    public static String validateUrl(String url) {

        String resUrl = url;

        if (url.endsWith("/")) {
            resUrl = url.substring(0, url.length() - 1);
        }

        if (resUrl.contains("#")) {
            resUrl = resUrl.substring(0, resUrl.indexOf("#"));
        }

        return resUrl;
    }

}