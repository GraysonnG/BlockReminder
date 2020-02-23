package com.blanktheevil.blockreminder.patches

import com.blanktheevil.blockreminder.BlockPreview
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.blanktheevil.blockreminder.patches.locators.AccessCurrentBlockLocator
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch
import com.megacrit.cardcrawl.characters.AbstractPlayer
import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.dungeons.AbstractDungeon

@Suppress("unused")
@SpirePatch(clz = AbstractCreature::class, method = "renderHealth")
class BlockPreviewRenderPatch {
  companion object {
    @SpireInsertPatch(
      locator = AccessCurrentBlockLocator::class,
      localvars = [
        "x",
        "y",
        "blockOffset"
      ]
    )
    @JvmStatic
    fun render(__instance: AbstractCreature, sb: SpriteBatch, x: Float, y: Float, blockOffset: Float) {
      if (__instance is AbstractPlayer && AbstractDungeon.overlayMenu.endTurnButton.enabled) {
        BlockPreview.render(sb, x, y, blockOffset, __instance.currentBlock, BlockPreviewFieldPatch.blockPreview.get(__instance)
          ?: 0)
      }
    }
  }
}