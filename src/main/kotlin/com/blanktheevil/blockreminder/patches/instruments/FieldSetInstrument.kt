package com.blanktheevil.blockreminder.patches.instruments

import com.blanktheevil.blockreminder.BlockPreview
import javassist.expr.ExprEditor
import javassist.expr.FieldAccess

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