package blockreminder

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
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

    @JvmStatic
    fun initialize() {
      BlockReminder()
      log("Version", "1.1.0")
      initConfig()
      loadEndTurnClasses()
    }

    fun initConfig() {
      try {
        config = SpireConfig("BlockPreview", "endTurnClasses");
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

    fun loadEndTurnClasses() {
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

    fun log(vararg items: String) {
      println(items.asList().joinToString(" : ", "Block Reminder"))
    }
  }
}