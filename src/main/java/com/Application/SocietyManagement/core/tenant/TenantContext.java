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

    public static void runAsTenant(String societyId, Runnable action) {
        String previous = getSocietyId();
        try {
            setSocietyId(societyId);
            action.run();
        } finally {
            if (previous != null) {
                setSocietyId(previous);
            } else {
                clear();
            }
        }
    }

    public static <T> T callAsTenant(String societyId, java.util.concurrent.Callable<T> action) throws Exception {
        String previous = getSocietyId();
        try {
            setSocietyId(societyId);
            return action.call();
        } finally {
            if (previous != null) {
                setSocietyId(previous);
            } else {
                clear();
            }
        }
    }
}
