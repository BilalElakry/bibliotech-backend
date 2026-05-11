package com.bibliotech.audit;

public final class AuditAction {
    private AuditAction() {}

    public static final String LOGIN           = "LOGIN";
    public static final String LOGOUT          = "LOGOUT";
    public static final String REGISTER        = "REGISTER";
    public static final String FAILED_LOGIN    = "FAILED_LOGIN";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    public static final String UPLOAD          = "UPLOAD";
    public static final String UPDATE          = "UPDATE";
    public static final String DELETE          = "DELETE";
    public static final String DOWNLOAD        = "DOWNLOAD";
    public static final String READ_ONLINE     = "READ_ONLINE";

    public static final String USER_CREATE     = "USER_CREATE";
    public static final String USER_TOGGLE     = "USER_TOGGLE";
    public static final String USER_DELETE     = "USER_DELETE";

    public static final String CATEGORY_CREATE = "CATEGORY_CREATE";
    public static final String CATEGORY_UPDATE = "CATEGORY_UPDATE";
    public static final String CATEGORY_DELETE = "CATEGORY_DELETE";
}