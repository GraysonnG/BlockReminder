package com.blanktheevil.blockreminder.patches.instruments

import com.blanktheevil.blockreminder.BlockPreview
import com.megacrit.cardcrawl.actions.common.GainBlockAction
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.clapper.util.classutil.ClassInfo

class PreviewInstrument(private val classInfo: ClassInfo) : ExprEditor() {
  @Suppress("SpellCheckingInspection")
  override fun edit(m: MethodCall) {
    if (m.methodName == "addToBot" || m.methodName == "addToBottom" || m.methodName == "addToTop") {
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
      if (m.className == classInfo.className) {
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