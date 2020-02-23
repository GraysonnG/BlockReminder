package com.blanktheevil.blockreminder.patches

import com.evacipated.cardcrawl.modthespire.lib.SpireField
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.megacrit.cardcrawl.core.AbstractCreature

@SpirePatch(clz = AbstractCreature::class, method = SpirePatch.CLASS)
class BlockPreviewFieldPatch {
  companion object {
    @JvmField
    var blockPreview: SpireField<Int?> = SpireField { 0 }
  }
}