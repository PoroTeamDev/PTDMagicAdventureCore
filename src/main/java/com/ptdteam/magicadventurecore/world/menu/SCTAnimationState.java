package com.ptdteam.magicadventurecore.world.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Клиентское состояние анимации крафта SCT.
 *
 * Анимация состоит из двух больших фаз:
 * 1) перекачка крови/маны от боковых шкал к центру (длительность = стоимость / 50 тиков,
 *    то есть 1000 единиц ресурса в секунду);
 * 2) поглощение ингредиентов из сетки и финальная вспышка (длительность из {@code craft_time}).
 *
 * Поля статические, потому что нужны и экрану (отрисовка), и слотам меню: во время второй
 * фазы они обязаны отдавать пустой стек, чтобы ванильный рендер не накладывался на анимацию.
 * Слоты обращаются к состоянию только на клиенте.
 */
public final class SCTAnimationState {
    /** Доля времени на «раскрутку» внутри фазы ингредиентов. */
    public static final float CHARGE_END = 0.15F;
    /** Доля времени, после которой начинается финальная вспышка. */
    public static final float FINISH_START = 0.85F;
    /** Доля времени, за которую предмет долетает до центра. */
    public static final float FLIGHT = 0.06F;

    private static final int[] RANK = new int[SCTCraftingMenu.INPUT_SLOTS];

    private static long startTick = Long.MIN_VALUE;
    private static int resourceTicks;
    private static int itemTicks;
    private static boolean itemPhase;

    private SCTAnimationState() {
    }

    /** Синхронизирует состояние с блок-сущностью; перестраивает порядок поглощения при новом крафте. */
    public static void update(long startGameTime, int resourceDuration, int itemDuration) {
        if (startGameTime == startTick && resourceDuration == resourceTicks && itemDuration == itemTicks) {
            return;
        }
        startTick = startGameTime;
        resourceTicks = Math.max(0, resourceDuration);
        itemTicks = Math.max(1, itemDuration);
        List<Integer> order = new ArrayList<>(SCTCraftingMenu.INPUT_SLOTS);
        for (int i = 0; i < SCTCraftingMenu.INPUT_SLOTS; i++) {
            order.add(i);
        }
        // Детерминированный порядок: у клиента и всех наблюдателей он одинаковый.
        Collections.shuffle(order, new Random(startTick));
        for (int rank = 0; rank < order.size(); rank++) {
            RANK[order.get(rank)] = rank;
        }
        itemPhase = false;
    }

    public static void reset() {
        startTick = Long.MIN_VALUE;
        resourceTicks = 0;
        itemTicks = 0;
        itemPhase = false;
    }

    public static boolean isActive() {
        return startTick != Long.MIN_VALUE && itemTicks > 0;
    }

    /** Устанавливается экраном каждый кадр: идёт ли уже фаза поглощения (а не перекачки). */
    public static void setItemPhase(boolean value) {
        itemPhase = value;
    }

    public static boolean isItemPhase() {
        return itemPhase;
    }

    /** Скрывать ли предметы в слотах от ванильного рендера. */
    public static boolean isHidingActive() {
        return isActive() && itemPhase;
    }

    public static boolean isHidden(int slotIndex) {
        return isHidingActive()
                && slotIndex >= 0
                && slotIndex <= SCTCraftingMenu.OUTPUT_SLOT;
    }

    private static float elapsed(long gameTime, float partialTick) {
        return gameTime + partialTick - startTick;
    }

    public static boolean inResourcePhase(long gameTime, float partialTick) {
        return isActive() && resourceTicks > 0 && elapsed(gameTime, partialTick) < resourceTicks;
    }

    public static float resourceProgress(long gameTime, float partialTick) {
        if (!isActive() || resourceTicks <= 0) {
            return 1F;
        }
        return clamp(elapsed(gameTime, partialTick) / resourceTicks);
    }

    public static float itemProgress(long gameTime, float partialTick) {
        if (!isActive()) {
            return 0F;
        }
        return clamp((elapsed(gameTime, partialTick) - resourceTicks) / itemTicks);
    }

    public static double absorbStart(int inputIndex) {
        int rank = inputIndex >= 0 && inputIndex < RANK.length ? RANK[inputIndex] : 0;
        return CHARGE_END + (FINISH_START - CHARGE_END) * (rank / (double) SCTCraftingMenu.INPUT_SLOTS);
    }

    /** 1 — раскрутка, 2 — поглощение, 3 — финал (внутри фазы ингредиентов). */
    public static int itemPhaseKind(float itemProgress) {
        if (itemProgress < CHARGE_END) {
            return 1;
        }
        return itemProgress < FINISH_START ? 2 : 3;
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }
}
