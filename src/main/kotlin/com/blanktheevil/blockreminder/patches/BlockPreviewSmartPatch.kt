package com.blanktheevil.blockreminder.patches

import com.blanktheevil.blockreminder.BlockReminder
import com.blanktheevil.blockreminder.patches.filters.StsClassFilter
import com.blanktheevil.blockreminder.patches.instruments.FieldSetInstrument
import com.blanktheevil.blockreminder.patches.instruments.PreviewInstrument
import com.blanktheevil.blockreminder.patches.locators.GainBlockActionLocator
import com.evacipated.cardcrawl.modthespire.Loader
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException
import com.megacrit.cardcrawl.core.CardCrawlGame
import com.megacrit.cardcrawl.orbs.AbstractOrb
import com.megacrit.cardcrawl.powers.AbstractPower
import com.megacrit.cardcrawl.relics.AbstractRelic
import javassist.CtBehavior
import javassist.Modifier
import org.clapper.util.classutil.*
import java.io.File
import java.net.URISyntaxException

@Suppress("unused")
@SpirePatch(clz = CardCrawlGame::class, method = SpirePatch.CONSTRUCTOR)
class BlockPreviewSmartPatch {
  companion object {
    @Suppress("FunctionName")
    @JvmStatic
    fun Raw(ctBehavior: CtBehavior) {
      println("\nBlock Reminder: Smart Block Preview Patch")
      val finder = ClassFinder()
      println("\t- Finding Classes...")
      finder.add(File(Loader.STS_JAR))

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
            BlockReminder.getEndOfTurnMethodFromCtClass(it)
          }
          .filter {
            it != null && GainBlockActionLocator().Locate(it).isNotEmpty()
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
  }
}