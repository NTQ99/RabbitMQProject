package app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static String loc;

    public static void main(String[] argv) throws Exception {

        File dir = new File(".");

        loc = dir.getCanonicalPath() + File.separator + "record.txt";

        FileWriter fstream = new FileWriter(loc, true);
        BufferedWriter out = new BufferedWriter(fstream);

        out.newLine();
        out.close();
        
        Scanner sc = new Scanner(System.in);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(REQUEST_QUEUE_NAME, true, false, false, null);
        channel.queueDeclare(RESPONSE_QUEUE_NAME, true, false, false, null);

        System.out.print(" [x] Sent ");
        String message = sc.nextLine();

        sendMessage(channel, message);

        recvMessage(channel);

        sc.close();

    }

    public static void sendMessage(Channel channel, String message) throws IOException {
        channel.basicPublish("", REQUEST_QUEUE_NAME,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes(StandardCharsets.UTF_8));
    }

    public static void recvMessage(Channel channel) throws IOException {
        File file = new File(loc);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            if (!checkExist(message, file)) {

                System.out.println("------ :  " + message);

                // insert to file

                FileWriter fstream = new FileWriter(loc, true);
                BufferedWriter out = new BufferedWriter(fstream);

                out.write(message);
                out.newLine();

                out.close();

                sendMessage(channel, message);

            }
        };

        boolean autoAck = true; // acknowledgment is covered below
        channel.basicConsume(RESPONSE_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> {
        });
    }

    public static boolean checkExist(String s, File fin) throws IOException {


        FileInputStream fis = new FileInputStream(fin);

        // //Construct the BufferedReader object

        BufferedReader in = new BufferedReader(new InputStreamReader(fis));


        String aLine = null;

        while ((aLine = in.readLine()) != null) {

            // //Process each line

            if (aLine.trim().contains(s)) {

                // System.out.println("contains " + s);

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

}