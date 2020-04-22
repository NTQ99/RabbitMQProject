package app;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

public class Consumer {

    private static final String REQUEST_QUEUE_NAME = "request_queue";
    private static final String RESPONSE_QUEUE_NAME = "response_queue";

    public static void main(String[] argv) throws Exception {
        
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

    private static boolean checkBaseURL(String baseURL, String URLCheck) {
        if (URLCheck.length() < baseURL.length())
            return false;
        String baseURLCheck = URLCheck.substring(0, baseURL.length());
        if (baseURL.equals(baseURLCheck)) return true;
        else return false;
    }

    private static void doWork(Channel channel, String URL) throws IOException {

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

            if (!message.equals("") && checkBaseURL(URL, message))
                sendMessage(channel, message);

        }

    }
}