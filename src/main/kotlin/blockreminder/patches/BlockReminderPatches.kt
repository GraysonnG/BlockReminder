package blockreminder.patches

import blockreminder.BlockPreview
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.evacipated.cardcrawl.modthespire.Loader
import com.evacipated.cardcrawl.modthespire.ModInfo
import com.evacipated.cardcrawl.modthespire.lib.*
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException
import com.megacrit.cardcrawl.actions.common.GainBlockAction
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.core.CardCrawlGame
import com.megacrit.cardcrawl.dungeons.AbstractDungeon
import com.megacrit.cardcrawl.orbs.AbstractOrb
import com.megacrit.cardcrawl.powers.AbstractPower
import com.megacrit.cardcrawl.relics.AbstractRelic
import javassist.*
import javassist.expr.ExprEditor
import javassist.expr.FieldAccess
import javassist.expr.MethodCall
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
                println("\t- Finding Classes...")
                finder.add(File(Loader.STS_JAR))

                for (modInfo: ModInfo in Loader.MODINFOS) {
                    if (modInfo.jarURL != null) {
                        try {
                            finder.add(File(modInfo.jarURL.toURI()))
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
                            StsClassFilter(AbstractRelic::class.java),
                            StsClassFilter(AbstractPower::class.java),
                            StsClassFilter(AbstractOrb::class.java)
                        )
                )

                val foundClasses = ArrayList<ClassInfo>()
                finder.findClasses(foundClasses, filter)

                println("\t- Done Finding Classes...\n\t- Begin Patching...")

                for ( classInfo: ClassInfo in foundClasses ) {
                    val ctClass: CtClass = ctBehavior.declaringClass.classPool.get(classInfo.className)
                    var endOfTurn: CtMethod? = null

                    try {
                        endOfTurn = ctClass.getDeclaredMethod("atEndOfTurn")
                    } catch (e:  NotFoundException) {
                        // do nothing
                    }

                    try {
                        endOfTurn = ctClass.getDeclaredMethod("onEndOfTurn")
                    } catch (e:  NotFoundException) {
                        // do nothing
                    }

                    try {
                        endOfTurn = ctClass.getDeclaredMethod("onPlayerEndTurn")
                    } catch (e:  NotFoundException) {
                        // do nothing
                    }

                    val lines = Locator().Locate(endOfTurn)

                    if (endOfTurn != null && lines != null && lines.isNotEmpty()) {
                        println("\t|\t- Patch Class: [${classInfo.className}]")
                        try {
                            endOfTurn.instrument(FieldSetInstrument())
                            endOfTurn.instrument(PreviewInstrument())
                            println("\t|\tSuccess...\n\t|")
                        } catch(e: PatchingException) {
                            println("\t|\tFailure...\n\t|")
                            e.printStackTrace()
                        }
                    }
                }
                println("\t- Done Patching...")
            }

            class PreviewInstrument : ExprEditor() {
                override fun edit(m: MethodCall?) {
                    if(m?.methodName == "addToBot" || m?.methodName == "addToBottom" || m?.methodName == "addToTop") {
                        println("\t|\t\t- Replacing Method Call: ${m?.className}.${m?.methodName}")
                        m.replace("{" +
                                "if (${BlockPreview::class.java.name}.isPreview) {" +
                                    "if ($1 instanceof ${GainBlockAction::class.java.name}) {" +
                                        "${BlockPreview::class.java.name}.Statics.runPreview($1.amount);" +
                                    "}" +
                                "} else {" +
                                    "$" + "proceed($$);" +
                                "}" +
                        "}")
                    } else {
                        if(m != null) {
                            m.replace("{" +
                                "if (!${BlockPreview::class.java.name}.isPreview) {" +
                                    "$" + "_=$" + "proceed($$);" +
                                "}" +
                            "}")
                        }
                    }
                }
            }

            class FieldSetInstrument : ExprEditor() {
                override fun edit(f: FieldAccess) {
                    println()
                    if(f.isWriter) {
                        f.replace("{" +
                            "if (!${BlockPreview::class.java.name}.isPreview) {" +
                                "$" + "_=$" + "proceed($$);" +
                            "}" +
                        "}")
                    }
                }
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

            class StsClassFilter(private val clz: Class<*>) : ClassFilter {
                override fun accept(classInfo: ClassInfo?, classFinder: ClassFinder?): Boolean {
                    return classInfo?.superClassName == clz.name
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