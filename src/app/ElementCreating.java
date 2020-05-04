package app;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.rabbitmq.client.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class ElementCreating {

    private static final String HANDLE_QUEUE_NAME = "handle_queue";

    private static String postFileJSON = "post.json";
    private static String ignoreUrl = "https://thelanb.com/wp-content/uploads/2020/05/20_image.pnghttps://thelanb.com/wp-content/uploads/2020/05/21_image.png";

    public static void main(String[] argv) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(HANDLE_QUEUE_NAME, true, false, false, null);

        recvHandleMessage(channel);

    }

    public static void recvHandleMessage(Channel channel) throws IOException {

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            System.out.println(" [x] Received '" + message + "'");
            try {
                doWork(message);
            } finally {
                System.out.println(" [x] Done");
            }
        };

        boolean autoAck = true; // acknowledgment is covered below
        channel.basicConsume(HANDLE_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> {
        });
    }

    public static void doWork(String URL) throws IOException {

        Document doc = null;

        try {
            
            doc = Jsoup.connect(URL).timeout(10 * 1000).get();

        } catch (IOException e) {
            return;
        }

        Post post = new Post();
        
        Elements downloadlinks = doc.select(".ext-link:containsOwn(LINK):containsOwn(LOAD)");
        if (downloadlinks.isEmpty()) downloadlinks = doc.select("a[href]:containsOwn(LINK):containsOwn(LOAD)");
        if (downloadlinks.isEmpty())
            return;
        
        Elements passwords = doc.select(":containsOwn(Pass)");
        for (Element downloadlink : downloadlinks) {

            post.downloadTitle.add(downloadlink.text());

            if (!passwords.isEmpty())
                post.downloadUrl.add(
                    getRedirectUrl(downloadlink.attr("abs:href"), "#" + passwords.first().text().replace(" ", "")));
            else
                post.downloadUrl.add(getRedirectUrl(downloadlink.attr("abs:href"), ""));
            
        }

        post.title = doc.title();
        post.originUrl = doc.baseUri();

        Elements imgs = doc.select("img[class*=wp-image]");
        if (imgs.isEmpty()) imgs = doc.select("img[class*=aligncenter]");

        for (Element img : imgs) {

            String imgUrl = img.attr("src");
            if(!ignoreUrl.contains(imgUrl) )post.imgUrls.add(imgUrl);

        }

        FileHandle.writeToJSON(postFileJSON, post);

    }

    public static String getRedirectUrl(String originUrl, String password) {

        String redirectUrl = originUrl;
        try {

            redirectUrl = URLDecoder.decode(originUrl.replace("https://thelanb.com/chuyen-huong?url=", ""), "UTF-8")
                    + password;
            return redirectUrl;

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return redirectUrl;
    }

    
}