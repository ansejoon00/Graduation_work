package com.example.graduation_work;

public class GlobalSettings {
    private static String userId;
    private static String userEmail;
    private static String userPhone;
    private static String userName;
    private static String userDate;


    public static void setUserId(String id) {
        userId = id;
    }

    public static String getUserId() {
        return userId;
    }

    public static void setUserEmail(String email) {
        userEmail = email;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserPhone(String phone) {
        userPhone = phone;
    }

    public static String getUserPhone() {
        return userPhone;
    }

    public static void setUserName(String name) {
        userName = name;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserDate(String date) {
        userDate = date;
    }

    public static String getUserDate() {
        return userDate;
    }

    public static void clearUser() {
        userId = null;
        userEmail = null;
        userPhone = null;
        userName = null;
        userDate = null;
    }
}
