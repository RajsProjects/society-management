package com.Application.SocietyManagement.core.tenant;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT_SOCIETY =
            new ThreadLocal<>();

    public static void setSocietyId(String societyId) {
        CURRENT_SOCIETY.set(societyId);
    }

    public static String getSocietyId() {
        return CURRENT_SOCIETY.get();
    }

    public static void clear() {
        CURRENT_SOCIETY.remove();
    }
}
