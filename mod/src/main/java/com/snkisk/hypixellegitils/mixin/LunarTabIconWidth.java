package com.snkisk.hypixellegitils.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import net.minecraft.client.gui.FontRenderer;

/** Reads Lunar's own measured Tab-logo width without a compile-time dependency on Lunar internals.
 * Lunar reports the full footprint for both its left- and right-side icon preferences. */
final class LunarTabIconWidth {
    private LunarTabIconWidth() {
    }

    static int measuredWidth(Object tabOverlay, UUID playerId, FontRenderer font, String text, int fallbackWidth) {
        if (tabOverlay == null || playerId == null || font == null) return fallbackWidth;
        try {
            Field playerField = tabOverlay.getClass().getDeclaredField("lunar$modifyingPlayerEntry");
            playerField.setAccessible(true);
            Object activePlayer = playerField.get(tabOverlay);
            // Lunar sets this field only while it is about to add the corresponding client-logo.
            if (!playerId.equals(activePlayer)) return fallbackWidth;
            Method widthMethod = lunarWidthMethod(tabOverlay.getClass());
            if (widthMethod == null) return fallbackWidth;
            Class<?> referenceType = widthMethod.getParameterTypes()[2];
            if (!supportsBooleanReference(referenceType)) return fallbackWidth;
            Object reference = Proxy.newProxyInstance(
                referenceType.getClassLoader(), new Class<?>[] {referenceType}, new TrueBooleanReference()
            );
            widthMethod.setAccessible(true);
            Object measured = widthMethod.invoke(tabOverlay, font, text, reference);
            // Lunar's helper consumes its transient context; restore it for Lunar's normal width pass.
            playerField.set(tabOverlay, activePlayer);
            return measured instanceof Number ? Math.max(fallbackWidth, ((Number) measured).intValue()) : fallbackWidth;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallbackWidth;
        }
    }

    static boolean supportsBooleanReference(Class<?> referenceType) {
        return referenceType != null && referenceType.isInterface();
    }

    private static Method lunarWidthMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().endsWith("$lunar$onGetStringWidth") && method.getParameterTypes().length == 3) return method;
        }
        return null;
    }

    private static final class TrueBooleanReference implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if ("get".equals(method.getName())) return Boolean.TRUE;
            return null;
        }
    }
}
