package app;

import java.beans.XMLEncoder;
import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FileHandle {

    public static boolean checkExist(String nameFile, String data) throws IOException {

        FileInputStream fis = new FileInputStream(nameFile);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

        String aLine = null;

        while ((aLine = in.readLine()) != null) {

            if (aLine.trim().contains(data)) {

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

    public static boolean checkExist(File file, String data) throws IOException {

        FileInputStream fis = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

        String aLine = null;

        while ((aLine = in.readLine()) != null) {

            if (aLine.trim().contains(data)) {

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

    public static void write(String nameFile, String data, boolean append) throws IOException {

        FileOutputStream fstream = new FileOutputStream(nameFile, append);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fstream, "UTF-8"));

        out.write(data);
        out.newLine();

        out.close();
        fstream.close();

    }

    public static void writeToXML(String nameFile, Object post) throws IOException {
        
        FileOutputStream fos = new FileOutputStream(nameFile, true);
        XMLEncoder encoder = new XMLEncoder(fos);
        
        encoder.writeObject(post);
        encoder.close();
        fos.close();

    }

    public static void writeToJSON(String nameFile, Object post) throws IOException {
        
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String data = gson.toJson(post) + ",";

        if (!checkExist(nameFile, data)) write(nameFile, data, true);

    }
}