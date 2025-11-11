package com.yomirein.sochatclient.config;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;

import java.util.List;
import java.util.Optional;

public final class CookieUtils {

    private CookieUtils() {}

    // Создаёт потокобезопасную копию CookieStore (для фоновых задач)
    public static CookieStore snapshotCookieStore(CookieStore original) {
        BasicCookieStore copy = new BasicCookieStore();
        if (original == null) return copy;
        synchronized (original) {
            for (Cookie c : original.getCookies()) {
                BasicClientCookie bc = new BasicClientCookie(c.getName(), c.getValue());
                bc.setDomain(c.getDomain());
                bc.setPath(c.getPath());
                bc.setExpiryDate(c.getExpiryDate());
                copy.addCookie(bc);
            }
        }
        return copy;
    }

    // Мержим куки из source в target (используется в UI-потоке/внутри vaadinSession.access/lock)
    public static void mergeCookieStore(CookieStore target, CookieStore source) {
        if (target == null || source == null) return;
        synchronized (target) {
            for (Cookie c : source.getCookies()) {
                BasicClientCookie bc = new BasicClientCookie(c.getName(), c.getValue());
                bc.setDomain(c.getDomain());
                bc.setPath(c.getPath());
                bc.setExpiryDate(c.getExpiryDate());
                target.addCookie(bc);
            }
        }
    }
    public static Optional<String> getCookieValue(CookieStore cookieStore, String name) {
        if (cookieStore == null) return Optional.empty();
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }
}
