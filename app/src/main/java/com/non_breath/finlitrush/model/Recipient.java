package com.non_breath.finlitrush.model;

import java.util.List;

public class Recipient {
    private final String id;
    private final String name;
    private final String role;
    private final List<String> specialties;

    public Recipient(String id, String name, String role, List<String> specialties) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.specialties = specialties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public List<String> getSpecialties() {
        return specialties;
    }
}
