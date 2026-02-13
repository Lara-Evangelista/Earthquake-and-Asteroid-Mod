package EarthquakeAsteroidMod;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MainMod extends Mod {
    private AsteroidMod asteroid;
    private EarthquakeMod earthquake;

    private static final String MOD_NAME = "earthquake-asteroid-mod";

    /**
     * This function intializes the mod
     */
    @Override
    public void init() {
        Events.run(EventType.Trigger.update, () -> {
            if (!isRunning()) return;
        });

        asteroid = new AsteroidMod();
        earthquake = new EarthquakeMod();

        asteroid.init();
        earthquake.init();
    }

    /**
     * This function if the game is running (not just in the menu)
     * @return true if the game is running
     */
    public static boolean isRunning() {
        if (!Vars.state.isGame()) return false;
        if (Vars.state.isPaused()) return false;
        if (!Vars.state.isPlaying()) return false;
        if (Vars.player == null) return false;
        if (Vars.player.unit() == null) return false;
        return true;
    }

    /**
     * This function returns the object's name as per mindustry's standards
     * (every sprite is named with the mod name first a "-" and then the actual sprite name)
     * @param name - the object's name
     * @return the actual name inside mindustry
     */
    public static String name(String name) {
        return MOD_NAME + "-" + name;
    }

}
