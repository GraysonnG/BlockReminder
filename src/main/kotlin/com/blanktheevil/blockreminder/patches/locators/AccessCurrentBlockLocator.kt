package com.blanktheevil.blockreminder.patches.locators

import com.evacipated.cardcrawl.modthespire.lib.LineFinder
import com.evacipated.cardcrawl.modthespire.lib.Matcher
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator
import com.megacrit.cardcrawl.core.AbstractCreature
import javassist.CtBehavior

class AccessCurrentBlockLocator : SpireInsertLocator() {
  override fun Locate(ctBehavior: CtBehavior): IntArray {
    val matcher = Matcher.FieldAccessMatcher(AbstractCreature::class.java, "currentBlock")
    return LineFinder.findInOrder(ctBehavior, matcher)
  }
}