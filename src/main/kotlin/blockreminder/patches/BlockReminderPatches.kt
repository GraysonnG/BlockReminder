package blockreminder.patches

import blockreminder.BlockPreview
import blockreminder.BlockReminder
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.evacipated.cardcrawl.modthespire.Loader
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
  companion object {
    private val endOfTurnMethodNames = mutableListOf<String>()

    init {
      endOfTurnMethodNames.add("atEndOfTurn")
      endOfTurnMethodNames.add("atEndOfTurnPreEndTurnCards")
      endOfTurnMethodNames.add("onEndOfTurn")
      endOfTurnMethodNames.add("onPlayerEndTurn")
    }

    @JvmStatic
    fun getEndOfTurnMethodFromCtClass(ctClass: CtClass): CtMethod? {
      var method: CtMethod? = null
      endOfTurnMethodNames.forEach {
        try {
          method = ctClass.getDeclaredMethod(it) as CtMethod
        } catch (e: NotFoundException) {
          // do nothing
        }
      }
      return method
    }
  }


  @SpirePatch(clz = AbstractCreature::class, method = SpirePatch.CLASS)
  class BlockPreviewField {
    companion object {
      @JvmField
      var blockPreview: SpireField<Int?> = SpireField { 0 }
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

        Loader.MODINFOS.asList().stream()
          .filter {
            it.jarURL != null
          }
          .forEach {
            try {
              finder.add(File(it.jarURL.toURI()))
            } catch (e: URISyntaxException) {
              // do nothing
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

        val foundClasses = ArrayList<ClassInfo>().also {
          finder.findClasses(it, filter)
        }

        println("\t- Done Finding Classes...\n\t- Begin Patching...")
        var cInfo: ClassInfo? = null
        try {
          foundClasses.stream()
            .map {
              cInfo = it
              ctBehavior.declaringClass.classPool.get(it.className)
            }
            .map {
              getEndOfTurnMethodFromCtClass(it)
            }
            .filter {
              it != null && Locator().Locate(it).isNotEmpty()
            }
            .map {
              it.let { it!! }
            }
            .forEach {
              println("\t|\t- Patch Class: [${cInfo?.className}]")
              try {
                it.instrument(FieldSetInstrument())
                cInfo?.let { it1 -> it.instrument(PreviewInstrument(it1)) }
                cInfo?.className?.let { it1 -> BlockReminder.endTurnBlockClasses.add(it1) }
                println("\t|\tSuccess...\n\t|")
              } catch (e: PatchingException) {
                println("\t|\tFailure...\n\t|")
                e.printStackTrace()
              }
            }
        } catch (e: Exception) {
          println("\t- Failed to Patch Classes")
          e.printStackTrace()
        }
        println("\t- Done Patching...")
        BlockReminder.initConfig()
        BlockReminder.saveEndTurnClasses()
        println("\t- Saving Patched Class Names...")
      }

      class PreviewInstrument(private val classInfo: ClassInfo) : ExprEditor() {
        override fun edit(m: MethodCall?) {
          if (m?.methodName == "addToBot" || m?.methodName == "addToBottom" || m?.methodName == "addToTop") {
            println("\t|\t\t- Replacing Method Call: ${m.className}.${m.methodName}")
            m.replace("{" +
                "if (${BlockPreview::class.java.name}.isPreview) {" +
                "if ($1 instanceof ${GainBlockAction::class.java.name}) {" +
                "${BlockPreview::class.java.name}.Statics.runPreview($1.amount);" +
                "}" +
                "} else {" +
                "${'$'}proceed($$);" +
                "}" +
                "}")
          } else {
            if (m != null && m.className == classInfo.className) {
              println("\t|\t\t- Replacing Method Call: ${m.className}.${m.methodName}")
              m.replace("{" +
                  "if (!${BlockPreview::class.java.name}.isPreview) {" +
                  "${'$'}_=${'$'}proceed($$);" +
                  "}" +
                  "}")
            }
          }
        }
      }

      class FieldSetInstrument : ExprEditor() {
        override fun edit(f: FieldAccess) {
          if (f.isWriter) {
            println("\t|\t\t- Replacing Field Access: ${f.className}.${f.fieldName}")
            f.replace("{" +
                "if (!${BlockPreview::class.java.name}.isPreview) {" +
                "${'$'}_=${'$'}proceed($$);" +
                "}" +
                "}")
          }
        }
      }

      class Locator : SpireInsertLocator() {
        override fun Locate(ctBehavior: CtBehavior?): IntArray {
          val matcher = Matcher.NewExprMatcher(GainBlockAction::class.java)
          return try {
            LineFinder.findInOrder(ctBehavior, matcher)
          } catch (e: Exception) {
            IntArray(0)
          }
        }
      }

      class StsClassFilter(private val clz: Class<*>) : ClassFilter {
        override fun accept(classInfo: ClassInfo?, classFinder: ClassFinder?): Boolean {
          return if (classInfo != null) {
            val superClasses = mutableMapOf<String, ClassInfo>()
            classFinder?.findAllSuperClasses(classInfo, superClasses)
            superClasses.containsKey(clz.name)
          } else {
            false
          }
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
        if (__instance is AbstractPlayer && AbstractDungeon.overlayMenu.endTurnButton.enabled) {
          BlockPreview.render(sb, x, y, blockOffset, __instance.currentBlock, BlockPreviewField.blockPreview.get(__instance)
            ?: 0)
        }
      }
    }
  }
}