package blockreminder.patches

import blockreminder.BlockPreview
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.evacipated.cardcrawl.modthespire.Loader
import com.evacipated.cardcrawl.modthespire.ModInfo
import com.evacipated.cardcrawl.modthespire.lib.*
import com.megacrit.cardcrawl.actions.common.GainBlockAction
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.core.CardCrawlGame
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.AbstractOrb
import com.megacrit.cardcrawl.powers.AbstractPower
import javassist.*
import org.clapper.util.classutil.*
import java.net.URISyntaxException
import kotlin.collections.ArrayList
import java.io.File

@Suppress("unused")
class BlockReminderPatches {
    @SpirePatch(clz = AbstractCreature::class, method = SpirePatch.CLASS)
    class BlockPreviewField {
        companion object {
            @JvmField
            var blockPreview: SpireField<Int?> = SpireField{0}
        }
    }

    @SpirePatch(clz = AbstractPower::class, method =  "flash")
    class FlashPatch {
        companion object {
            @JvmStatic
            @SpirePrefixPatch
            fun stopInPreview(__instance: AbstractPower): SpireReturn<Void> {
                return if (BlockPreview.isPreview) SpireReturn.Return(null) else SpireReturn.Continue()
            }
        }
    }

    @SpirePatch(clz = CardCrawlGame::class, method = SpirePatch.CONSTRUCTOR)
    class SmartBlockPreview {
        companion object {
            @JvmStatic
            fun Raw(ctBehavior: CtBehavior) {
                println("\nBlock Reminder: Smart Block Preview Patch")
                val finder = ClassFinder()
                val url = Loader.STS_JAR
                println("\t- Finding Classes...")
                finder.add(File(url))

                for (modInfo: ModInfo in Loader.MODINFOS) {
                    if (modInfo.jarURL != null) {
                        try {
                            var f = File(modInfo.jarURL.toURI())
                            finder.add(f)
                        } catch (e: URISyntaxException) {
                            // do nothing
                        }
                    }
                }

                // Get all classes
                val filter = AndClassFilter(
                        NotClassFilter(InterfaceOnlyClassFilter()),
                        NotClassFilter(AbstractClassFilter()),
                        ClassModifiersClassFilter(Modifier.PUBLIC),
                        OrClassFilter(
                                PowerClassFilter(),
                                OrbClassFilter()
                        )
                )

                val foundClasses = ArrayList<ClassInfo>()
                finder.findClasses(foundClasses, filter)

                println("\t- Done Finding Classes...\n\t- Begin Patching...")

                for ( classInfo: ClassInfo in foundClasses ) {
                    val ctClass: CtClass = ctBehavior.declaringClass.classPool.get(classInfo.className)
                    var endOfTurn: CtMethod? = null
                    var amountRef: String? = ""

                    try {
                        endOfTurn = ctClass.getDeclaredMethod("atEndOfTurn")
                        amountRef = "this.amount"
                    } catch (e:  NotFoundException) {
                        // do nothing
                    }

                    try {
                        endOfTurn = ctClass.getDeclaredMethod("onEndOfTurn")
                        amountRef = "this.passiveAmount"
                    } catch (e:  NotFoundException) {
                        // do nothing
                    }

                    val lines = Locator().Locate(endOfTurn)
                    if (endOfTurn != null && lines != null) {
                        val patch: String = "${BlockPreview::class.java.name}.Statics.runPreview($amountRef); if (${BlockPreview::class.java.name}.isPreview) { return; }"
                        val lineToInsert = lines[0]
                        println("\t|\t- Patch Class: [${classInfo.className}]")
                        println("\t|\t\tInsert loc: $lineToInsert")
                        endOfTurn.insertAt(lineToInsert, patch)
                        println("\t|\tSuccess...\n\t|")
                    }
                }
                println("\t- Done Patching...")
            }

            class Locator : SpireInsertLocator() {
                override fun Locate(ctBehavior: CtBehavior?): IntArray? {
                    val matcher = Matcher.NewExprMatcher(GainBlockAction::class.java)
                    try {
                        return LineFinder.findInOrder(ctBehavior, matcher)
                    } catch (e: Exception) {
                        return null
                    }
                }
            }

            class PowerClassFilter : ClassFilter {
                override fun accept(classInfo: ClassInfo?, classFinder: ClassFinder?): Boolean {
                    return classInfo?.superClassName == AbstractPower::class.java.name
                }
            }

            class OrbClassFilter : ClassFilter {
                override fun accept(classInfo: ClassInfo?, classFinder: ClassFinder?): Boolean {
                    return classInfo?.superClassName == AbstractOrb::class.java.name
                }
            }
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