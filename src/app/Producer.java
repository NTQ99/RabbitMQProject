package app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

public class Producer {

    private static final String REQUEST_QUEUE_NAME = "request_queue";
    private static final String RESPONSE_QUEUE_NAME = "response_queue";

    private static String recordFile = "record.txt";
    private static String postFileJSON = "post.json";

    public static void main(String[] argv) throws Exception {

        Scanner sc = new Scanner(System.in);
        
        System.out.print(" [x] Sent ");
        String message = sc.nextLine();

        FileHandle.write(recordFile, message, false);
        FileHandle.write(postFileJSON, "[", false);

        sc.close();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(REQUEST_QUEUE_NAME, true, false, false, null);
        channel.queueDeclare(RESPONSE_QUEUE_NAME, true, false, false, null);

        sendMessage(channel, message);
        recvMessage(channel);

    }

    public static void sendMessage(Channel channel, String message) throws IOException {

        if (message == "") return;
        message = validateUrl(message);

        channel.basicPublish("", REQUEST_QUEUE_NAME,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes(StandardCharsets.UTF_8));

    }

    public static void recvMessage(Channel channel) throws IOException {

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            String message = new String(delivery.getBody(), "UTF-8");

            sendMessage(channel, message);

        };

        boolean autoAck = true; // acknowledgment is covered below
        channel.basicConsume(RESPONSE_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });

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