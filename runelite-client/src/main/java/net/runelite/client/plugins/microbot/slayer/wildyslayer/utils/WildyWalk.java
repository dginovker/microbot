package net.runelite.client.plugins.microbot.slayer.wildyslayer.utils;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import static net.runelite.client.plugins.microbot.slayer.wildyslayer.WildySlayerPlugin.wildySlayerRunning;
import static net.runelite.client.plugins.microbot.slayer.wildyslayer.utils.Combat.task;
import static net.runelite.client.plugins.microbot.slayer.wildyslayer.utils.MonsterEnum.getConfig;
import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;
import static net.runelite.client.plugins.microbot.util.paintlogs.PaintLogsScript.debug;

public class WildyWalk {
    public static void toResetAggroSpot() {
         debug("Walking to aggro reset spot...");
         while (inWildy() && wildySlayerRunning && distTo(task().getAggroResetSpot()) > 3) {
             Microbot.getWalker().walkTo(task().getAggroResetSpot());
             sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
         }
    }

    public static int distTo(int x, int y) {
         return distTo(new WorldPoint(x, y, Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane()));
    }

    public static int distTo(LocalPoint point) {
        if (point == null) return 0;
        return Microbot.getClient().getLocalPlayer().getLocalLocation().distanceTo(point) / 128;
    }

    public static int distTo(WorldPoint point) {
        if (point == null) return 0;
        return Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(point);
    }

    public static void toSlayerLocation(String taskName) {
         if (inFerox()) {
             exitFerox();
         }
         while (wildySlayerRunning && distTo(new WorldPoint(3122, 3629, 0)) < 25) {
             debug("Getting unstuck from West of Ferox...");
             Microbot.getWalker().walkTo(Microbot.getClient().getLocalPlayer().getWorldLocation().dx(-2).dy(7));
             sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
         }
         if (getConfig(taskName).isInSlayerCave() && Microbot.getClient().getLocalPlayer().getWorldLocation().getY() < 10000) {
             toSlayerCave();
             return;
         }
         if (getConfig(taskName).getLocation().getY() > 3903 && Microbot.getClient().getLocalPlayer().getWorldLocation().getY() <= 3903) {
             if (Rs2GameObject.interact(1728) || Rs2GameObject.interact(1569)) {
                 debug("Opening northern gate...");
                 sleep(5000);
             }
             debug("Walking to the northern gate to get to " + taskName);
             Microbot.getWalker().walkTo(new WorldPoint(3223, 3906, 0));
             sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
             return;
         }
         debug("Walking to " + taskName);
         Microbot.getWalker().walkTo(getConfig(taskName).getLocation(), false);
         sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
         sleep(1200, 3600);
        if (inGraveyardOfShadows()) {
            debug("Sleeping another 6 seconds because walker easily gets stuck in Graveyard of Shadows..");
            sleep(6000);
        }
    }

    private static void exitFerox() {
        debug("Leaving barrier..");
        WorldPoint barrierPoint = null;
        switch (task().getFeroxExitDir()) {
            case WEST:
                barrierPoint = new WorldPoint(3124, 3629, 0);
                break;
            case NORTH:
                barrierPoint = new WorldPoint(3135, 3639, 0);
                break;
            case EAST:
                barrierPoint = new WorldPoint(3154, 3635, 0);
                break;
            case SOUTH:
                barrierPoint = new WorldPoint(3135, 3617, 0);
                break;
        }
        while (wildySlayerRunning && distTo(barrierPoint) > 8 && inFerox()) {
            Microbot.getWalker().walkTo(barrierPoint);
            sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
        }
        Rs2GameObject.interact(39652);
        sleepUntil(() -> !inFerox());
    }

    private static void toSlayerCave() {
        debug("Going to the slayer cave...");
        if (Rs2GameObject.interact(40388)) {
            debug("Entering slayer cave...");
            sleep(5000);
            return;
        }
        Microbot.getWalker().walkTo(new WorldPoint(3259, 3662, 0));
        sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
    }

    private final static WorldPoint slayerCaveEntrance = new WorldPoint(3385, 10053, 0);
    public static void toFerox() {
        if (inFerox()) {
            debug("Already in Ferox.. Sleeping 25 seconds");
            sleep(25_000);
            return;
        }
        if (inSlayerCave() && Microbot.getClient().getLocalPlayer().getWorldLocation().getY() <= 10079 ||
                !inSlayerCave() && Microbot.getClient().getLocalPlayer().getWorldLocation().getY() <= 3679) {
            while (wildySlayerRunning && Microbot.getClient().getLocalPlayer().getHealthScale() != -1) {
                debug("Can't use dueling ring while in combat! Trying to run away");
                Microbot.getWalker().walkTo(Microbot.getClient().getLocalPlayer().getWorldLocation().dy(-10));
                sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
            }
            debug("Using dueling ring");
            Inventory.useItemSafe("Ring of Dueling"); // assumes your dueling rings are left-click rub
            sleepUntil(() -> Rs2Widget.findWidget("Ferox Enclave.") != null);
            Rs2Widget.clickWidget("Ferox Enclave.");
            sleep(3000);
            return;
        }
        if (inSlayerCave()) {
            debug("Walking to slayer cave entrance..");
            Microbot.getWalker().walkTo(slayerCaveEntrance);
            sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
            return;
        }
        if (Microbot.getClient().getLocalPlayer().getWorldLocation().getY() > 3903) {
            if (Rs2GameObject.interact(1728) || Rs2GameObject.interact(1569)) {
                debug("Opening northern gate...");
                sleep(5000);
            }
            debug("Walking to the northern gate..");
            Microbot.getWalker().walkTo(new WorldPoint(3224, 3902, 0));
            sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
            return;
        }
        debug("Walking south..");
        Microbot.getWalker().walkTo(Microbot.getClient().getLocalPlayer().getWorldLocation().dy(-20));
        sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
    }

    private static final WorldPoint fallyBank = new WorldPoint(2946, 3370, 0);
    public static void toFallyBank() {
        while (wildySlayerRunning && distTo(fallyBank) > 10 && distTo(fallyBank) < 100) {
            Microbot.getWalker().walkTo(fallyBank);
            sleepUntil(() -> distTo(Microbot.getClient().getLocalDestinationLocation()) < 5);
        }
    }

    public static boolean inFerox() {
        return Rs2Npc.getNpc("Banker") != null && Microbot.getWalker().canReach(Rs2Npc.getNpc("Banker").getWorldLocation()) ||
                Rs2Npc.getNpc("Marten") != null && Microbot.getWalker().canReach(Rs2Npc.getNpc("Marten").getWorldLocation());
    }

    private static boolean inSlayerCave() {
        return Microbot.getClient().getLocalPlayer().getWorldLocation().getY() > 10000;
    }

    private static boolean inGraveyardOfShadows() {
        int x1 = 3147;
        int x2 = 3184;
        int y1 = 3656;
        int y2 = 3689;
        int x = Microbot.getClient().getLocalPlayer().getWorldLocation().getX();
        int y = Microbot.getClient().getLocalPlayer().getWorldLocation().getY();
        return x > x1 && x < x2 && y > y1 && y < y2;
    }


    public static boolean inWildy() {
        return Microbot.getClient().getLocalPlayer().getWorldLocation().getY() > 3520;
    }


}
