package app;

import java.io.IOException;

public class UrlHandle {
    public static void main(String[] args) {
        Post post = new Post();
        post.downloadUrl.add("https://drive.google.com/open?id=11L8t1z81WrtTGyHXc_5q2wy6TinT5i_i");
        post.originUrl = "https://thelanb.com/share-preset-mau-trong-treo-phong-cach-au-my.html";
        post.title = "[Share Stock & Preset] Bộ Ảnh Indoor \u0027Cô Đơn\u0027 - Tác Giả: NAG Bằng Bụi - Chia Sẻ Tài Nguyên Cho Designer và Photographer";
        System.out.println(post.title);
        try {
            FileHandle.writeToJSON("a.json", post);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}