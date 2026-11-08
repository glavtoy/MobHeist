package ru.glavtoy.mobheist.mob;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class OwnedMob {

    private final MobType type;
    private final Mutation mutation;
    @Setter
    private int level;
    @Setter
    private double stored;

    public OwnedMob(MobType type, Mutation mutation, int level, double stored) {
        this.type = type;
        this.mutation = mutation;
        this.level = level;
        this.stored = stored;
    }

    public String getName() {
        return mutation == null ? type.getName() : mutation.getName() + " " + type.getName();
    }
}
