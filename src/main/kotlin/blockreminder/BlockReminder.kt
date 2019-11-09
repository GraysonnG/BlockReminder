package blockreminder

import basemod.BaseMod
import basemod.interfaces.PostInitializeSubscriber
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.relics.AbstractRelic
import com.megacrit.cardcrawl.relics.CloakClasp
import com.megacrit.cardcrawl.relics.Orichalcum

@Suppress("unused")
@SpireInitializer
class BlockReminder {
    companion object Statics : PostInitializeSubscriber {
        @JvmField
        val previewRelics = mutableMapOf<String, (r: AbstractRelic) -> Int>()

        @JvmStatic
        fun initialize() {
            BlockReminder()
            BaseMod.subscribe(this)
            log("Version", "0.0.2")
        }

        fun log(vararg items: String) {
            println(items.asList().joinToString(" : ", "Block Reminder"))
        }

        @JvmStatic
        fun addRelicToPreview(relic: AbstractRelic, preview: (r: AbstractRelic) -> Int) {
            log("Registered Relic: ${relic.relicId}")
            previewRelics.putIfAbsent(relic.relicId, preview)
        }

        override fun receivePostInitialize() {
            addRelicToPreview(Orichalcum()) {
                if (AbstractDungeon.player.currentBlock > 0) 0 else 6
            }
            addRelicToPreview(CloakClasp()) {
                AbstractDungeon.player.hand.size()
            }
        }
    }
}