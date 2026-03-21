package com.joyhill.demo.domain;

public enum Role {
    member,
    leader,
    village_leader,
    pastor,
    admin;

    public boolean atLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}
