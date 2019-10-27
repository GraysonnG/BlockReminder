package blockreminder.patches

import blockreminder.BlockPreview
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.evacipated.cardcrawl.modthespire.lib.SpireField
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.dungeons.AbstractDungeon

@Suppress("unused")
class BlockReminderPatches {
    @SpirePatch(clz = AbstractCreature::class, method = SpirePatch.CLASS)
    class BlockPreviewField {
        companion object {
            @JvmField
            var blockPreview: SpireField<Int?> = SpireField{0}
        }
    }

    @SpirePatch(clz = AbstractPlayer::class, method = "update")
    object BlockPreviewUpdate {
        @SpirePostfixPatch
        @JvmStatic
        fun update(__instance: AbstractCreature) {
            BlockPreview.update(__instance)
        }
    }

    @SpirePatch(clz = AbstractCreature::class, method = "renderHealth")
    class BlockPreviewRender {
        companion object {
            @SpireInsertPatch(
                    rloc = 18,
                    localvars = [
                        "x",
                        "y",
                        "blockOffset"
                    ]
            )
            @JvmStatic
            fun render(__instance: AbstractCreature, sb: SpriteBatch, x: Float, y: Float, blockOffset: Float) {
                if(__instance is AbstractPlayer && AbstractDungeon.overlayMenu.endTurnButton.enabled) {
                    BlockPreview.render(sb, x, y, blockOffset, __instance.currentBlock, BlockPreviewField.blockPreview.get(__instance) ?: 0)
                }
            }
        }
    }
}