package com.example.monsterhunter.service;

import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * 測試用的簡易假物件：用 JDK 內建的 Proxy 實作 Repository 介面，只回應測試有用到的方法。
 *
 * 為什麼不用 Mockito：Mockito 的 inline mock maker 要在執行時動態掛 Java agent，
 * 在這台電腦的 JDK 24 上會直接失敗（Could not initialize plugin: MockMaker），
 * 用 Proxy 就不依賴 agent，任何 JDK 版本都能跑。
 */
final class Fakes {

    private Fakes() {
    }

    @SuppressWarnings("unchecked")
    static <T> T of(Class<T> type, String methodName, Function<Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals(methodName)) {
                return handler.apply(args);
            }
            return switch (method.getName()) {
                case "toString" -> "Fake(" + type.getSimpleName() + ")";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                // save(entity) 之類的直接回傳傳進來的物件，其餘沒用到的方法回 null
                case "save" -> args[0];
                default -> null;
            };
        });
    }
}
