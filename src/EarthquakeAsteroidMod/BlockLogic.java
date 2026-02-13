package EarthquakeAsteroidMod;

import arc.math.Mathf;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.World;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;


public class BlockLogic {

    /**
     * Checks if a tile is valid
     * @param tile - tile in the center of the radius
     * @param radius - radius around the tile
     * @return true if tile is valid false otherwise
     */
    private static boolean isTileValid(Tile tile, int radius) {
        if (tile == null) return false;

        // ensure the tile AND its impact radius stay within map bounds
        int buffer = radius + 5; // radius + extra safety margin

        if (tile.x < buffer || tile.x >= Vars.world.width() - buffer ||
                tile.y < buffer || tile.y >= Vars.world.height() - buffer)
            return false;

        // Check if floor exists and has surface
        return tile.floor() != null && tile.floor().hasSurface() && tile.data <= 0;
    }

    /**
     * Counts amount of breakable builds around the tile
     * @param x - x position of the tile
     * @param y - y position of the tile
     * @param radius - radius to check around the tile
     * @return - number of breakables builds around the tile
     */
    private static int countBreakableAround(int x, int y, int radius){
        int breakableCount = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Mathf.dst(0, 0, dx, dy) > radius) continue;

                Tile checkTile = Vars.world.tile(x + dx, y + dy);
                if (checkTile == null) continue;

                if (isBreakable(checkTile) && checkTile.build != null) breakableCount++;
            }
        }
        return breakableCount;
    }

    /**
     * Finds the best target around the player, if not found asteroid/earthquake doesn't spawn/happen
     * @param radius - radius to check around tile
     * @param maxRange - max range to check around player
     * @return the tile with the most breakable builds, if no tile with breakable builds around the player, returns null
     */
    // Common method to find valid target location near player
    //Asteroid and maybe earthquakes WILL NOT SPAWN if no blocks are placed
    public static Tile findValidTargetNearPlayer(int radius, int maxRange) {
        if (Vars.player == null || Vars.player.unit() == null) {
            Log.warn("[SHARED] Player or player unit is null!");
            return null;
        }

        float playerX = Vars.player.unit().x;
        float playerY = Vars.player.unit().y;
        int playerTileX = World.toTile(playerX);
        int playerTileY = World.toTile(playerY);
        Tile tile = null;
        Tile testTile;
        int mostBreakableBlocks = 0;

        Log.info("[SHARED] Player at tile (" + playerTileX + ", " + playerTileY + ")");
        Log.info("[SHARED] Searching for target within " + maxRange + " tiles of player, radius=" + radius);

        for (int attempt = 0; attempt < 150; attempt++) {

            int testX = playerTileX + Mathf.range(maxRange);
            int testY = playerTileY + Mathf.range(maxRange);

            // Clamp with buffer to stay within map bounds
            int buffer = radius + 5;
            testX = Mathf.clamp(testX, buffer, Vars.world.width() - buffer);
            testY = Mathf.clamp(testY, buffer, Vars.world.height() - buffer);

            //if (attempt % 30 == 0)
                //Log.info("[SHARED] Attempt " + attempt + " - testing (" + testX + ", " + testY + ")");

            testTile = Vars.world.tile(testX, testY);

            // Check if tile is valid
            if (!isTileValid(testTile, radius))
                continue;

            int breakableCount = countBreakableAround(testX, testY, radius);
            if (breakableCount > mostBreakableBlocks) {
                mostBreakableBlocks = breakableCount;
                tile = testTile;
                Log.info("found tile");
            }
        }

        Log.warn("[SHARED] Could not find valid location after 150 attempts!");
        return tile;
    }

    /**
     * Checks if the block on the tile is breakable
     * @param tile - tile to check
     * @return true if breakable, false otherwise
     */
    public static boolean isBreakable(Tile tile) {
        if (tile == null) return false;
        if (tile.isDarkened()) return false;
        if (tile.block() == Blocks.air) return false;
        if (tile.block() instanceof CoreBlock) return false;
        return tile.breakable();
    }

    /**
     * Destroys build/block on tile
     * @param t - tile to destroy build/block on
     * @return true if destroyed
     */
    public static boolean destroyTile(Tile t) {
        if (t.build != null)  t.build.kill();

        t.setBlock(Blocks.air);

        return true;
    }
}
