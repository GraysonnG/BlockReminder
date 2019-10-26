package blockreminder;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SuppressWarnings("unused")
@SpireInitializer
public class BlockReminder {

    private BlockReminder() {
    }

    @SuppressWarnings("unused")
    public static void initialize() {
        log("Version", "0.0.1");
        new BlockReminder();
    }

    private static void log(String ... items) {
        System.out.print("BlockReminder ");
        for(String item : items) {
            System.out.print(" : " + item);
        }
        System.out.println();
    }
}
