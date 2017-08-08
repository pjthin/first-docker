package fr.pjthin.firstdocker;

public class Context {

    public static final String CHANNEL_EVENTBUS = "fr.pjthin.";
    public static final long NOT_SET = -1L;
    private static long ID = NOT_SET;

    public static long getID() {
        return ID;
    }

    public static void setID(long iD) {
        ID = iD;
    }
}
