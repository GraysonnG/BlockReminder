package blockreminder

import basemod.BaseMod
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
import com.megacrit.cardcrawl.relics.AbstractRelic

@Suppress("unused")
@SpireInitializer
class BlockReminder {
    companion object Statics {
        @JvmField
        val previewRelics = mutableMapOf<String, (r: AbstractRelic) -> Int>()

        @JvmStatic
        fun initialize() {
            BlockReminder()
            log("Version", "1.0.0")
        }

        fun log(vararg items: String) {
            println(items.asList().joinToString(" : ", "Block Reminder"))
        }
    }
}