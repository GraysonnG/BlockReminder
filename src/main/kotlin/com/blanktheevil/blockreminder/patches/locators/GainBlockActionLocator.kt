package com.blanktheevil.blockreminder.patches.locators

import com.evacipated.cardcrawl.modthespire.lib.LineFinder
import com.evacipated.cardcrawl.modthespire.lib.Matcher
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator
import com.megacrit.cardcrawl.actions.common.GainBlockAction
import javassist.CtBehavior

class GainBlockActionLocator : SpireInsertLocator() {
  override fun Locate(ctBehavior: CtBehavior?): IntArray {
    val matcher = Matcher.NewExprMatcher(GainBlockAction::class.java)
    return try {
      LineFinder.findInOrder(ctBehavior, matcher)
    } catch (e: Exception) {
      IntArray(0)
    }
  }
}