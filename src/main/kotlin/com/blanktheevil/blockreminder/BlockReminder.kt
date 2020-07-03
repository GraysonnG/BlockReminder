package com.blanktheevil.blockreminder

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import java.io.IOException
import java.lang.NullPointerException

@Suppress("unused")
@SpireInitializer
class BlockReminder {
  companion object Statics {
    private const val endTurnBlockClassesKey = "CLASSES"
    private const val endTurnBlockClassesDelimiter = ","
    private var config: SpireConfig? = null
    val endTurnBlockClasses = ArrayList<String>()
    private val endOfTurnMethodNames = mutableListOf<String>()

    init {
      endOfTurnMethodNames.add("atEndOfTurn")
      endOfTurnMethodNames.add("atEndOfTurnPreEndTurnCards")
      endOfTurnMethodNames.add("onEndOfTurn")
      endOfTurnMethodNames.add("onPlayerEndTurn")
    }

    @JvmStatic
    fun initialize() {
      BlockReminder()
      log("Version", "1.1.0")
      initConfig()
      loadEndTurnClasses()
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

    fun initConfig() {
      try {
        config = SpireConfig("BlockPreview", "endTurnClasses")
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }

    fun saveEndTurnClasses() {
      try {
        config!!.setString(
          endTurnBlockClassesKey,
          endTurnBlockClasses.joinToString(endTurnBlockClassesDelimiter))
        config!!.save()
      } catch (e: IOException) {
        e.printStackTrace()
      } catch (e: NullPointerException) {
        e.printStackTrace()
      }
    }

    private fun loadEndTurnClasses() {
      try {
        config!!.load()
        endTurnBlockClasses.addAll(
          config!!.getString(endTurnBlockClassesKey)
            .split(endTurnBlockClassesDelimiter))
      } catch (e: IOException) {
        e.printStackTrace()
      } catch (e: NullPointerException) {
        e.printStackTrace()
      }
    }

    private fun log(vararg items: String) {
      println(items.asList().joinToString(" : ", "Block Reminder"))
    }
  }
}