package blockreminder;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SpireInitializer
public class BlockReminder {

    BlockReminder() {
    }

    @SuppressWarnings("unused")
    public static void initialize() {
        log("Version", "0.0.1");
        new BlockReminder();
    }

    private static void log(String ... items) {
        System.out.print("RelicSorter ");
        for(String item : items) {
            System.out.print(" : " + item);
        }
        System.out.println();
    }
}
