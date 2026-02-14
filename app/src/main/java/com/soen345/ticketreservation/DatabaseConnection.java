package com.soen345.ticketreservation;

import com.soen345.ticketreservation.BuildConfig;

public class DatabaseConnection {
    // Supabase credentials from BuildConfig
    private final String url = BuildConfig.SUPABASE_URL;
    private final String key = BuildConfig.SUPABASE_KEY;

    public DatabaseConnection() {
        // You can initialize your Supabase client here later
        System.out.println("Supabase URL: " + url);
        System.out.println("Supabase Key: " + key);
    }

    // Example method to get connection info
    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }
}
