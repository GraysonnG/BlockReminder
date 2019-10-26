package blockreminder.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.CloakClasp;
import com.megacrit.cardcrawl.relics.Orichalcum;
import com.megacrit.cardcrawl.stances.AbstractStance;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class DetectEndTurnBlock {
    private static Map<String, Boolean> isEndTurnBlockClass = new HashMap<>();

    public static void calculateEndTurnClass(Class clazz, String endOfTurnMethodName) {
        try {
            ClassPool pool = Loader.getClassPool();
            CtClass ctClass = pool.get(clazz.getName());
            ctClass.defrost();
            CtMethod endOfTurnMethod;
            try {
                endOfTurnMethod = ctClass.getDeclaredMethod(endOfTurnMethodName);
            } catch (NotFoundException e) {
                isEndTurnBlockClass.putIfAbsent(clazz.getName(), false);
                return;
            }

            endOfTurnMethod.instrument(new ExprEditor() {
                @Override
                public void edit(NewExpr expr) {
                    try {
                        CtConstructor ctConstructor = expr.getConstructor();
                        CtClass cls = ctConstructor.getDeclaringClass();
                        if (cls != null) {
                            CtClass parent = cls;
                            while (parent != null && parent.getName().equals(GainBlockAction.class.getName())) {
                                parent = parent.getSuperclass();
                            }
                            if (parent != null && (cls.getName().equals(GainBlockAction.class.getName()) || parent.getName().equals(GainBlockAction.class.getName()))) {
                                // found a class with specified end of turn method
                                System.out.println("Found Class with end of turn block:" + clazz.getName());
                                isEndTurnBlockClass.put(clazz.getName(), true);
                            }
                        }

                    } catch (Exception e) {

                    }
                }
            });
        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        }
        isEndTurnBlockClass.putIfAbsent(clazz.getName(), false);
    }

    public static int getAmountOfEndTurnBlockPower(AbstractPower p) {
        if (!isEndTurnBlockClass.containsKey(p.getClass().getName())) {
            calculateEndTurnClass(p.getClass(), "atEndOfTurn");
        }

        if (isEndTurnBlockClass.get(p.getClass().getName())) {
            return p.amount;
        }

        return 0;
    }

    public static int getAmountOfEndTurnBlockOrb(AbstractOrb o) {
        if (!isEndTurnBlockClass.containsKey(o.getClass().getName())) {
            calculateEndTurnClass(o.getClass(), "onEndOfTurn");
        }

        if (isEndTurnBlockClass.get(o.getClass().getName())) {
            return o.passiveAmount;
        }

        return 0;
    }

    public static int getAmountOfEndTurnBlockRelics(AbstractPlayer __instance, AbstractRelic r) {
        switch (r.relicId) {
            case Orichalcum.ID:
                return __instance.currentBlock <= 0 ? 6 : 0;
            case CloakClasp.ID:
                return AbstractDungeon.player.hand.group.size();
            default:
                return 0;
        }
    }

    @SuppressWarnings("unused")
    @SpirePatch(clz = AbstractCreature.class, method = SpirePatch.CLASS)
    @SpirePatch(clz = AbstractPlayer.class, method = "update")
    @SpirePatch(clz = AbstractMonster.class, method = "update")
    public static class BlockPreview {

        public static SpireField<Integer> blockPreview = new SpireField<>(() -> 0);

        @SuppressWarnings("unused")
        @SpirePostfixPatch
        public static void updateBlockPreview(AbstractCreature __instance) {
            if (__instance instanceof AbstractCreature) {
                BlockPreview.blockPreview.set(__instance, 0);

                for (AbstractPower p : __instance.powers) {
                    increaseBlockPreview(__instance, getAmountOfEndTurnBlockPower(p));
                }

                if (__instance instanceof AbstractPlayer) {
                    for (AbstractOrb o : ((AbstractPlayer) __instance).orbs) {
                        increaseBlockPreview(__instance, getAmountOfEndTurnBlockOrb(o));
                    }

                    for (AbstractRelic r : ((AbstractPlayer) __instance).relics) {
                        increaseBlockPreview(__instance, getAmountOfEndTurnBlockRelics((AbstractPlayer) __instance, r));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @SpirePatch(clz = AbstractCreature.class, method = "renderHealth")
    public static class BlockPreviewRenderer {

        @SuppressWarnings("unused")
        @SpireInsertPatch(rloc = 18, localvars = {
            "blockOffset",
            "x",
            "y"
        })
        public static void render(AbstractCreature __instance, SpriteBatch sb, float blockOffset, float x, float y) {
            if(BlockPreview.blockPreview.get(__instance) > 0 && AbstractDungeon.overlayMenu.endTurnButton.enabled) {
                renderBlockPreviewIconAndValue(
                    sb,
                    __instance.currentBlock,
                    BlockPreview.blockPreview.get(__instance),
                    blockOffset,
                    x,
                    y);
            }
        }

        private static void renderBlockPreviewIconAndValue(SpriteBatch sb, int currentBlock, int futureBlock, float blockOffset, float x, float y) {
            Color blockPreviewColor = Color.CYAN.cpy();
            int BLOCK_W = 64;
            float BLOCK_ICON_X = -14 * Settings.scale;
            float BLOCK_ICON_Y = -14 * Settings.scale;
            float xOffset = (currentBlock > 0 ? BLOCK_W / 2 * Settings.scale: 0);

            blockPreviewColor.a = 0.5f;

            sb.setColor(blockPreviewColor);
            sb.draw(
                ImageMaster.BLOCK_ICON,
                (x + BLOCK_ICON_X - BLOCK_W / 2f),
                y + BLOCK_ICON_Y - BLOCK_W / 2f + blockOffset + xOffset,
                BLOCK_W / 2f,
                BLOCK_W / 2f,
                BLOCK_W,
                BLOCK_W,
                Settings.scale,
                Settings.scale,
                0f,
                0,
                0,
                BLOCK_W,
                BLOCK_W,
                false,
                false);

            FontHelper.renderFontCentered(
                sb,
                FontHelper.blockInfoFont,
                "+" + futureBlock,
                x + BLOCK_ICON_X,
                (y - 16f * Settings.scale) + xOffset,
                Color.WHITE.cpy(),
                1f);
        }
    }

    private static void increaseBlockPreview(AbstractCreature c, int amount) {
        BlockPreview.blockPreview.set(c, BlockPreview.blockPreview.get(c) + amount);
    }
}
