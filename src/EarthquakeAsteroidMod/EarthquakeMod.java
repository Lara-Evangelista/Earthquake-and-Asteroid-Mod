package EarthquakeAsteroidMod;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;

public class EarthquakeMod {

    /**
     * Initializes an earthquake
     */
    public void init() {
        Log.info("[EARTHQUAKE] ========================================");
        Log.info("[EARTHQUAKE] MOD CONSTRUCTOR CALLED!");
        Log.info("[EARTHQUAKE] ========================================");

        Events.run(EventType.Trigger.update, () -> {
            if (!MainMod.isRunning()) return;

            // Chance, on average, of one event every 6 hours
            if (Mathf.chanceDelta(0.000000772f)) {
                Log.info("[EARTHQUAKE] *** EARTHQUAKE TRIGGERED! ***");
                destroyBlocksAroundEpicenter();
            }
        });

        Log.info("[EARTHQUAKE] Mod initialization complete!");
    }

    /**
     * Determines earthquake size
     * @return max radius of the earthquake
     */
    // ------------------- EARTHQUAKE SYSTEM --------------------------
    private int determineEarthquakeMax(){
        // Determine earthquake size
        int blocksToDestroy = Mathf.random(5, 25);
        int maxRadius = (blocksToDestroy >= 15) ? Mathf.random(14, 20) : Mathf.random(6, 12);
        Log.info("[EARTHQUAKE] Target: " + blocksToDestroy + " blocks, Radius: " + maxRadius);
        return maxRadius;
    }

    /**
     * Applies earthquake visual effects
     * @param epicenterX - tile x position
     * @param epicenterY - tile y position
     * @param maxRadius - radius around the tile
     * @param intensity - intensity of the earthquake (for shaking the screen)
     * @param duration - duration of the earthquake (for shaking the screen)
     */
    private void applyEarthquakeEffects(int epicenterX, int epicenterY, int maxRadius, float intensity, float duration){
        float worldX = epicenterX * Vars.tilesize;
        float worldY = epicenterY * Vars.tilesize;

        Effect slowShock = new Effect(150f, 400f, e -> {
            color(Color.white, Color.lightGray, e.fin());
            stroke(e.fout() * 3f + 0.5f);

            float[] radius = { maxRadius*2f, maxRadius*4f, maxRadius*8f };
            for (float r : radius) {
                Lines.circle(e.x, e.y, e.fin() * r);
            }
        });

        slowShock.at(worldX, worldY);
        Vars.renderer.shake(intensity, duration);
    }

    /**
     * Find breakable blocks around a tile
     * @param maxRadius - radius to check around tile
     * @param epicenterX - tile position x
     * @param epicenterY - tile position y
     * @param checkedMultiBlocks - multiblocks that have been found inside the radius
     * @param breakableBlocks - blocks that have been found inside the radius
     */
    private void findBreakableBlocks(int maxRadius, int epicenterX, int epicenterY, Seq<Tile> checkedMultiBlocks, Seq<Tile> breakableBlocks){
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                if (Mathf.dst(0, 0, dx, dy) > maxRadius) continue;

                Tile tile = Vars.world.tile(epicenterX + dx, epicenterY + dy);
                if (!BlockLogic.isBreakable(tile)) continue;

                if (tile.build != null && tile.build.block.size > 1) {
                    if (!checkedMultiBlocks.contains(tile.build.tile)) {
                        checkedMultiBlocks.add(tile.build.tile);
                        breakableBlocks.add(tile.build.tile);
                    }
                } else {
                    breakableBlocks.add(tile);
                }
            }
        }
    }

    /**
     * Destroy blocks
     * @param blocksToDestroy - number of blocks to destroy
     * @param breakableBlocks - minimum blocks destroyed
     * @return number of blocks destroyed
     */
    private int destroyBlocks(int blocksToDestroy, Seq<Tile> breakableBlocks){
        int toDestroy = Math.min(blocksToDestroy, breakableBlocks.size);
        breakableBlocks.shuffle();

        int destroyed = 0;
        for (int i = 0; i < toDestroy; i++) {
            Tile tile = breakableBlocks.get(i);

            if (tile.build != null && tile.build.block.size > 1) {
                Seq<Tile> linkedTiles = new Seq<>();
                tile.getLinkedTiles(linkedTiles);
                for (Tile t : linkedTiles) {
                    if (destroyTile(t)) destroyed++;
                }
            } else {
                if (destroyTile(tile)) destroyed++;
            }
        }
        return destroyed;
    }

    private boolean destroyTile(Tile t){
        Floor newFloor = getNeighborFloor(t);
        if (newFloor != null) t.setFloor(newFloor);

        return BlockLogic.destroyTile(t);
    }

    /**
     * Get neighbor floor
     * @param tile - tile that will get the floor
     * @return true if floor found, false otherwise
     */
    private static Floor getNeighborFloor(Tile tile) {
        for (int i = 0; i < 4; i++) {
            Tile neighbor = tile.nearby(i);
            if (neighbor != null && neighbor.floor() != null) return neighbor.floor();
        }

        if (tile.floor() != null) return tile.floor();

        return null;
    }

    /**
     * Gets valid tile (needs to have a breakable build/block) and destroys blocks around said tile
     */
    private void destroyBlocksAroundEpicenter() {
        Log.info("[EARTHQUAKE] === START ===");
        int blocksToDestroy = Mathf.random(5, 25);
        int maxRadius = determineEarthquakeMax();

        // Use shared method to find epicenter near player
        int maxRange = 80;
        Tile epicenterTile = BlockLogic.findValidTargetNearPlayer(maxRadius, maxRange);

        if (epicenterTile == null)
            { Log.warn("[EARTHQUAKE] No valid epicenter found!"); return; }

        int epicenterX = epicenterTile.x;
        int epicenterY = epicenterTile.y;
        Log.info("[EARTHQUAKE] Epicenter at (" + epicenterX + ", " + epicenterY + ")");

        // Apply effects
        float intensity, duration;

        if (blocksToDestroy >= 12) {
            intensity = Mathf.random(10.0f, 20.0f);
            duration = Mathf.random(100.0f, 140.0f);
        } else {
            intensity = Mathf.random(7.5f, 10.0f);
            duration = Mathf.random(50.0f, 90.0f);
        }

        applyEarthquakeEffects(epicenterX, epicenterY, maxRadius, intensity, duration);

        // Find ALL breakable blocks in radius
        Seq<Tile> breakableBlocks = new Seq<>();
        Seq<Tile> checkedMultiBlocks = new Seq<>();

        findBreakableBlocks(maxRadius, epicenterX, epicenterY, checkedMultiBlocks, breakableBlocks);

        int destroyed = destroyBlocks(blocksToDestroy, breakableBlocks);



        Log.info("[EARTHQUAKE] === END === Destroyed " + destroyed + " blocks");
    }
}