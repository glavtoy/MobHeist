package ru.glavtoy.mobheist.plot;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public final class PlotTemplate {

    private Vector shop;
    private Vector door;
    private Vector spawn;
    private float spawnYaw;
    private int size;
    private Vector manager;
    private Vector managerPad;
    private Vector incomePad;
    private Vector luckPad;
    private Vector storagePad;
    private Vector rebirthPad;
    private List<Vector> pens = new ArrayList<>();
    private List<Vector> penPads = new ArrayList<>();
}
