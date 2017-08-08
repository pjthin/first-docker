package fr.pjthin.firstdocker;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static void log(String log) {
        System.out.println(getHeader() + log);
    }

    private static String getHeader() {
        long id = Context.getID();
        String header = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " [" + Thread.currentThread().getName() + "] ";
        if (id == Context.NOT_SET) {
            return header;
        }
        return header + "[id=" + id + "] ";
    }

}
