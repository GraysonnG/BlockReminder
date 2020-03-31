package com.blanktheevil.blockreminder.patches

import com.blanktheevil.blockreminder.BlockPreview
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature

@Suppress("unused")
@SpirePatch(clz = AbstractPlayer::class, method = "update")
class BlockPreviewUpdatePatch {
  companion object {
    @SpirePostfixPatch
    @JvmStatic
    fun update(__instance: AbstractCreature) {
      BlockPreview.update(__instance)
    }
  }
}