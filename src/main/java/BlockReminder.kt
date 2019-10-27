import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer

@Suppress("unused")
@SpireInitializer
class BlockReminder {
    companion object Statics {
        @JvmStatic
        fun initialize() {
            BlockReminder()
            log("Version", "0.0.2")
        }

        fun log(vararg items: String) {
            println(items.asList().joinToString(" : ", "Block Reminder"))
        }
    }
}