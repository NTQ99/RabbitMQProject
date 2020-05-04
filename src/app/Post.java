package app;

import java.io.Serializable;
import java.util.ArrayList;

public class Post implements Serializable {

    /**
     * A post has originUrl, title, imgUrls and downloadUrl
     */
    private static final long serialVersionUID = 1L;
    
    public ArrayList<String> downloadUrl = new ArrayList<String>();
    public ArrayList<String> downloadTitle = new ArrayList<String>();
    public String originUrl = new String();
    public String title = new String();
    public ArrayList<String> imgUrls = new ArrayList<String>();

}