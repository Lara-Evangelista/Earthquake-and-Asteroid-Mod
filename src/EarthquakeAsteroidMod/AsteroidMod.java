package EarthquakeAsteroidMod;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.core.World;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.content.Sounds;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.maps.planet.*;
import mindustry.world.Tile;
import mindustry.world.TileGen;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.Block;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.content.Blocks.*;

public class AsteroidMod{
    private Block[] oresSerpulo;
    private Block[] oresErekir;
    private final Seq<Asteroid> activeAsteroids = new Seq<>();
    // Sprite references loaded from assets/sprites/
    private final static PlanetGenerator[] planetsGen = new PlanetGenerator[]{Planets.serpulo.generator, Planets.erekir.generator, Planets.tantros.generator};
    private static TextureRegion asteroidSprite;
    private static TextureRegion[] tailSprites;
    private static TextureRegion shadowSprite;

    // Inner class for Asteroid
    //
    private static class Asteroid {
        float startX, startY;
        float targetX, targetY;
        float x, y;
        float angle;
        float speed = 4f;
        float progress = 0f;
        float totalDistance;
        float warningTime = 500f;
        float currentWarning = 0f;
        int impactRadius;
        float size;
        boolean impacted = false;

        /**
         * String for ease of logging
         * @return string with a few of the most important values
         */
        public String toString() {
            return "Asteroid[pos=(" + x + "," + y + "), target=(" + targetX + "," + targetY +
                    "), warning=" + currentWarning + "/" + warningTime + ", impacted=" + impacted + "]";
        }
    }

    /**
     * Initializes the asteroid, asteroid will spawn based on a pre-defined chance
     */
    public void init() {
        initializeOres();

        // Try to load sprites
        Events.on(EventType.ClientLoadEvent.class, e -> {
            loadSprites();
        });

        Events.run(EventType.Trigger.update, () -> {
            if (!MainMod.isRunning()) return;

            // Chance, on average, of one event every 6 hours
            if (Mathf.chanceDelta(0.000000772f)) {
                Log.info("[ASTEROID] ========================================");
                Log.info("[ASTEROID] *** ASTEROID SPAWN TRIGGERED! ***");
                Log.info("[ASTEROID] ========================================");
                spawnAsteroid();
            }


            // Update active asteroids
            if (activeAsteroids.size > 0) {
                updateAsteroids();
            }
        });

        // Render asteroids
        Events.run(EventType.Trigger.draw, () -> {
            if (activeAsteroids.size > 0) {
                renderAsteroids();
            }
        });

        Log.info("[ASTEROID] Mod initialization complete!");
    }

    /**
     * Intializes ores from certain planets (needed since the original code doesn't store them inside planets)
     */
    // New method to initialize ores after content loads
    private void initializeOres() {
        Log.info("[ASTEROID] ========================================");
        Log.info("[ASTEROID] Initializing ores after content load...");

        Seq<Block> oresS = new Seq<>();

        oresS.add(oreCopper);
        oresS.add(oreLead);
        oresS.add(oreScrap);
        oresS.add(oreCoal);

        oresSerpulo = oresS.toArray(Block.class);

        Seq<Block> oresE = new Seq<>();

        oresS.add(oreTungsten);
        oresS.add(oreCrystalThorium);
        oresS.add(wallOreBeryllium);

        oresErekir = oresE.toArray(Block.class);
    }

    /**
     * Loads sprites into the game
     */
    public static void loadSprites() {
        Log.info("[ASTEROID] ========================================");
        Log.info("[ASTEROID] LOADING SPRITES - START");
        Log.info("[ASTEROID] ========================================");

        if (Core.atlas == null) {
            Log.err("[ASTEROID] ERROR: Core.atlas is null!");
            return;
        }

        Log.info("[ASTEROID] Core.atlas exists");

        // Log what we're looking for
        String asteroidName = MainMod.name("asteroid");
        String shadowName = MainMod.name("asteroid-shadow");
        String tail1Name = MainMod.name("asteroid-tail-1");
        String tail2Name = MainMod.name("asteroid-tail-2");
        String tail3Name = MainMod.name("asteroid-tail-3");

        Log.info("[ASTEROID] Looking for sprite: " + asteroidName);
        Log.info("[ASTEROID] Looking for sprite: " + shadowName);
        Log.info("[ASTEROID] Looking for sprite: " + tail1Name);
        Log.info("[ASTEROID] Looking for sprite: " + tail2Name);
        Log.info("[ASTEROID] Looking for sprite: " + tail3Name);

        // List ALL sprites that match our mod prefix
        Log.info("[ASTEROID] ========================================");
        Log.info("[ASTEROID] Searching for sprites with 'Earthquake' or 'asteroid' in name...");
        Seq<TextureAtlas.AtlasRegion> regions = Core.atlas.getRegions();
        int foundCount = 0;
        for (TextureAtlas.AtlasRegion region : regions) {
            if (region.name.contains("Earthquake") || region.name.contains("asteroid") ||
                    region.name.contains("Asteroid")) {
                Log.info("[ASTEROID] Found matching region: '" + region.name + "'");
                foundCount++;
            }
        }
        Log.info("[ASTEROID] Total matching sprites found: " + foundCount);
        Log.info("[ASTEROID] ========================================");

        // Try to load sprites
        asteroidSprite = Core.atlas.find(asteroidName);
        shadowSprite = Core.atlas.find(shadowName);

        tailSprites = new TextureRegion[3];
        tailSprites[0] = Core.atlas.find(tail1Name);
        tailSprites[1] = Core.atlas.find(tail2Name);
        tailSprites[2] = Core.atlas.find(tail3Name);

        // Log what we actually got
        Log.info("[ASTEROID] ========================================");
        Log.info("[ASTEROID] Loaded asteroidSprite: " + asteroidSprite);
        Log.info("[ASTEROID] asteroidSprite.found(): " + asteroidSprite.found());
        Log.info("[ASTEROID] ----------------------------------------");
        Log.info("[ASTEROID] Loaded shadowSprite: " + shadowSprite);
        Log.info("[ASTEROID] shadowSprite.found(): " + shadowSprite.found());
        Log.info("[ASTEROID] ----------------------------------------");

        for (int i = 0; i < tailSprites.length; i++) {
            Log.info("[ASTEROID] Loaded tailSprite[" + i + "]: " + tailSprites[i]);
            Log.info("[ASTEROID] tailSprite[" + i + "].found(): " + tailSprites[i].found());
        }
        Log.info("[ASTEROID] ========================================");

        // Check if sprites loaded successfully
        boolean hasAsteroid = asteroidSprite.found();
        boolean hasShadow = shadowSprite.found();
        boolean hasTails = tailSprites[0].found() && tailSprites[1].found() && tailSprites[2].found();

        Log.info("[ASTEROID] Summary:");
        Log.info("[ASTEROID] - Asteroid sprite found: " + hasAsteroid);
        Log.info("[ASTEROID] - Shadow sprite found: " + hasShadow);
        Log.info("[ASTEROID] - All tail sprites found: " + hasTails);

        if (!hasAsteroid || !hasShadow || !hasTails) {
            Log.warn("[ASTEROID] ========================================");
            Log.warn("[ASTEROID] WARNING: Some sprites not found!");
            Log.warn("[ASTEROID] Using fallback rendering.");
            Log.warn("[ASTEROID] ========================================");
            Log.warn("[ASTEROID] TROUBLESHOOTING:");
            Log.warn("[ASTEROID] 1. Check files exist in: assets/sprites/");
            Log.warn("[ASTEROID] 2. Required files:");
            Log.warn("[ASTEROID]    - asteroid.png");
            Log.warn("[ASTEROID]    - asteroid-shadow.png");
            Log.warn("[ASTEROID]    - asteroid-tail-1.png");
            Log.warn("[ASTEROID]    - asteroid-tail-2.png");
            Log.warn("[ASTEROID]    - asteroid-tail-3.png");
            Log.warn("[ASTEROID] 3. Rebuild with: gradlew jar");
            Log.warn("[ASTEROID] 4. Check mod.hjson name field (currently looking for prefix: '" + MainMod.name("") + "')");
            Log.warn("[ASTEROID] ========================================");
        } else {
            Log.info("[ASTEROID] ========================================");
            Log.info("[ASTEROID] SUCCESS: All sprites loaded!");
            Log.info("[ASTEROID] ========================================");
        }
    }

    /**
     * Spawns an asteroid, assigns values to variables in Asteroid, such as the target tile position and asteroid distance
     */
    private void spawnAsteroid() {
        Log.info("[ASTEROID] spawnAsteroid() called");

        Asteroid asteroid = new Asteroid();

        // Random impact radius
        asteroid.impactRadius = Mathf.random(4, 12);
        asteroid.size = Mathf.map(asteroid.impactRadius, 4, 12, 0.7f, 1.5f);
        Log.info("[ASTEROID] Impact radius: " + asteroid.impactRadius + ", size: " + asteroid.size);

        // Find valid impact location (using shared method)
        int maxRange = 80; // Same as earthquake
        Log.info("[ASTEROID] Searching for valid impact location near player...");
        Tile impactTile = BlockLogic.findValidTargetNearPlayer(asteroid.impactRadius, maxRange);

        if (impactTile == null) {
            Log.warn("[ASTEROID] !!!! NO VALID IMPACT LOCATION FOUND !!!!");
            return;
        }

        Log.info("[ASTEROID] Found valid tile at (" + impactTile.x + ", " + impactTile.y + ")");

        asteroid.targetX = impactTile.worldx();
        asteroid.targetY = impactTile.worldy();
        Log.info("[ASTEROID] Target world coords: (" + asteroid.targetX + ", " + asteroid.targetY + ")");

        // Choose random diagonal direction
        int[] angles = {45, 135, 225, 315};
        asteroid.angle = angles[Mathf.random(angles.length - 1)];
        Log.info("[ASTEROID] Chosen angle: " + asteroid.angle + " degrees");

        // Calculate starting position - REDUCED from 400f to 200f
        float distance = 200f; // Closer starting distance
        asteroid.startX = asteroid.targetX + Mathf.cosDeg(asteroid.angle + 180) * distance;
        asteroid.startY = asteroid.targetY + Mathf.sinDeg(asteroid.angle + 180) * distance;
        asteroid.x = asteroid.startX;
        asteroid.y = asteroid.startY;
        Log.info("[ASTEROID] Start position: (" + asteroid.startX + ", " + asteroid.startY + ")");

        asteroid.totalDistance = Mathf.dst(asteroid.startX, asteroid.startY,
                asteroid.targetX, asteroid.targetY);
        Log.info("[ASTEROID] Total distance: " + asteroid.totalDistance);

        asteroid.speed = 4f;

        activeAsteroids.add(asteroid);
        Log.info("[ASTEROID] Added to activeAsteroids list. Total active: " + activeAsteroids.size);
        Log.info("[ASTEROID] ========================================");
    }

    /**
     * Updates asteroid parameters as they advance towards the floor
     */
    private void updateAsteroids() {
        if (activeAsteroids.size == 0) return;

        Seq<Asteroid> toRemove = new Seq<>();

        for (int i = 0; i < activeAsteroids.size; i++) {
            Asteroid asteroid = activeAsteroids.get(i);

            /*if (updateCounter % 60 == 0)
                Log.info("[ASTEROID] Updating asteroid " + i + ": " + asteroid);*/


            if (asteroid.impacted) { toRemove.add(asteroid); continue; }

            // Warning phase
            if (asteroid.currentWarning < asteroid.warningTime) {
                asteroid.currentWarning += Time.delta;
                continue;
            }

            // Move asteroid in a STRAIGHT LINE towards target
            asteroid.progress += (asteroid.speed * Time.delta) / asteroid.totalDistance;
            asteroid.progress = Mathf.clamp(asteroid.progress, 0f, 1f);

            // Interpolate position along the straight line from start to target
            asteroid.x = Mathf.lerp(asteroid.startX, asteroid.targetX, asteroid.progress);
            asteroid.y = Mathf.lerp(asteroid.startY, asteroid.targetY, asteroid.progress);

            float distToTarget = Mathf.dst(asteroid.x, asteroid.y, asteroid.targetX, asteroid.targetY);

            // Check for impact
            if (asteroid.progress >= 0.99f || distToTarget < 16f) {
                Log.info("[ASTEROID] *** ASTEROID " + i + " REACHED TARGET! ***");
                handleAsteroidImpact(asteroid);
                asteroid.impacted = true;
            }
        }

        if (toRemove.size > 0) activeAsteroids.removeAll(toRemove);

    }

    /**
     * Renders asteroids
     */
    private void renderAsteroids() {
        for (int i = 0; i < activeAsteroids.size; i++) {
            Asteroid asteroid = activeAsteroids.get(i);

            if (asteroid.impacted) continue;

            float alpha;
            // Pulsing during warning phase
            if (asteroid.currentWarning < asteroid.warningTime)
                alpha = Mathf.absin(Time.time * 0.1f, 1f, 0.6f);
            else alpha = 0.5f + (asteroid.progress * 0.5f); // Starts at 0.5, goes to 1.0

            Draw.z(29f);
            Draw.color(Color.red, alpha);
            Draw.rect(shadowSprite, asteroid.targetX, asteroid.targetY,
                    asteroid.impactRadius * Vars.tilesize * 2.5f,
                    asteroid.impactRadius * Vars.tilesize * 2.5f);
            Draw.reset();

            // Only draw the flying asteroid AFTER warning phase
            if (asteroid.currentWarning < asteroid.warningTime) continue;

            // Draw the flying asteroid
            Draw.z(110f);

            // Draw tail
            int frameIndex = ((int) (Time.time * 0.3f)) % tailSprites.length;
            TextureRegion tail = tailSprites[frameIndex];

            float tailLength = tail.width * asteroid.size * 3 / 2f;
            float tailX = asteroid.x + Mathf.cosDeg(asteroid.angle + 180) * tailLength;
            float tailY = asteroid.y + Mathf.sinDeg(asteroid.angle + 180) * tailLength;

            Draw.color(Color.white, 1.0f);
            Draw.rect(tail, tailX, tailY,
                    tail.width * asteroid.size * 3f,
                    tail.height * asteroid.size * 2f,
                    asteroid.angle + 180);

            // Draw asteroid body
            Draw.color(Color.white, 1.0f);
            Draw.rect(asteroidSprite, asteroid.x, asteroid.y,
                    asteroidSprite.width * asteroid.size * 2,
                    asteroidSprite.height * asteroid.size * 2,
                    Time.time * 2f);

            Draw.reset();
        }
    }

    /**
     * Applies asteroid effects once it impacts, such as visual effects, or having blocks destroyed
     * @param asteroid - asteroid that will impact
     */
    private void handleAsteroidImpact(Asteroid asteroid) {
        Log.info("[ASTEROID] ========================================");
        Log.info("[ASTEROID] *** IMPACT EVENT ***");
        Log.info("[ASTEROID] Position: (" + asteroid.targetX + ", " + asteroid.targetY + ")");

        int impactTileX = World.toTile(asteroid.targetX);
        int impactTileY = World.toTile(asteroid.targetY);

        // Create impact effect
        Effect impactEffect = new Effect(120f, e -> {
            color(Color.white, Color.orange, e.fin());
            stroke(e.fout() * 6f + 1f);
            Lines.circle(e.x, e.y, e.fin() * asteroid.impactRadius * Vars.tilesize * 2);

            color(Color.yellow, Color.red, e.fin());
            Lines.circle(e.x, e.y, e.fin() * asteroid.impactRadius * Vars.tilesize);
        });

        impactEffect.at(asteroid.targetX, asteroid.targetY);

        // Screen shake
        float shakeIntensity = Mathf.map(asteroid.impactRadius, 4, 12, 15f, 30f);
        if (Vars.renderer != null)
            Vars.renderer.shake(shakeIntensity, 80f);

        // Play sound
        Sounds.explosionbig.at(asteroid.targetX, asteroid.targetY, 1.5f);

        // Damage player if in range
        if (Vars.player != null && Vars.player.unit() != null) {
            float playerDist = Mathf.dst(Vars.player.x, Vars.player.y,
                    asteroid.targetX, asteroid.targetY);
            float damageRadius = asteroid.impactRadius * Vars.tilesize * 1.2f;

            if (playerDist < damageRadius) {
                float damageMult = 1f - (playerDist / damageRadius);
                float damage = 200f * damageMult;
                Vars.player.unit().damage(damage);
                Log.info("[ASTEROID] *** PLAYER HIT FOR " + damage + " DAMAGE! ***");
            }
        }

        // Destroy blocks in radius
        destroyBlocksInRadius(impactTileX, impactTileY, asteroid.impactRadius);

        // Place crater
        placeCrater(impactTileX, impactTileY, asteroid.impactRadius);

        Log.info("[ASTEROID] ========================================");
    }

    /**
     * Destroys blocks around a tile based on a defined radius
     * @param centerX - tile x position
     * @param centerY - tile y position
     * @param radius - radius to check around the tile
     */
    private void destroyBlocksInRadius(int centerX, int centerY, int radius) {
        Log.info("[ASTEROID] destroyBlocksInRadius(" + centerX + ", " + centerY + ", " + radius + ")");
        PlanetGenerator pGen = planetsGen[Mathf.random(0,2)];
        int destroyed = 0;
        Seq<Tile> checkedMultiblocks = new Seq<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                float dist = Mathf.dst(0, 0, dx, dy);
                if (dist > radius) continue;

                Tile tile = Vars.world.tile(centerX + dx, centerY + dy);

                if (tile.build != null && tile.build.block.size > 1) {
                    if (!checkedMultiblocks.contains(tile.build.tile)) {
                        checkedMultiblocks.add(tile.build.tile);
                        Seq<Tile> linkedTiles = new Seq<>();
                        tile.build.tile.getLinkedTiles(linkedTiles);
                        for (Tile t : linkedTiles) {
                            if (!(t.block() instanceof CoreBlock)) { genTile(t, pGen); destroyed++; }
                        }
                    }
                } else { if (!(tile.block() instanceof CoreBlock)) { genTile(tile, pGen); destroyed++; } }

            }
        }

        Log.info("[ASTEROID] Destroyed " + destroyed + " blocks");
    }

    /**
     * Places a crater on the impact zone of the asteroid
     * May also spawn ores from other planets
     * @param centerX - tile x position
     * @param centerY - tile y position
     * @param radius - radius to check around the tile
     */
    private void placeCrater(int centerX, int centerY, int radius) {
        PlanetGenerator pGen = planetsGen[Mathf.random(0, 2)];
        int placedCharred = 0;
        int placedAlien = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                float dist = Mathf.dst(0, 0, dx, dy);
                if (dist > radius) continue; // Outside impact area

                Tile tile = Vars.world.tile(centerX + dx, centerY + dy);
                if (tile == null) continue;

                // Inner area (0-80% radius): Mix of charred and alien floor
                if (dist <= radius * 0.8f) {
                    if (Mathf.random() < 0.8f) {
                        // 80% chance for charred floor
                        Floor burnedFloor = (Floor) Blocks.charr;
                        if (burnedFloor != null) {
                            tile.setFloor(burnedFloor);
                            placedCharred++;
                        }
                    } else {
                        // 20% chance for alien floor in inner area
                        genTile(tile, pGen);
                        placedAlien++;
                    }
                }
                // Border area (80-100% radius): ALL alien floor
                else {
                    genTile(tile, pGen);
                    placedAlien++;
                }
            }
        }

        Log.info("[ASTEROID] Placed " + placedCharred + " charred floor tiles");
        Log.info("[ASTEROID] Placed " + placedAlien + " alien floor tiles");

        // 25% chance to spawn ores
        if (Mathf.random() < 0.25f) {
            Log.info("[ASTEROID] Attempting to spawn ores...");

            // 75% chance for 1-3 ores, 25% chance for 4-7 ores
            int oreCount;
            if (Mathf.random() < 0.75f) {
                oreCount = Mathf.random(1, 3);
            } else {
                oreCount = Mathf.random(4, 7);
            }

            Log.info("[ASTEROID] Will attempt to spawn " + oreCount + " ores");

            Block[] ores;
            if (Vars.ui != null && Vars.ui.planet.state.planet == Planets.erekir) {
                ores = oresErekir.clone();
            } else {
                ores = oresSerpulo.clone();
            }

            if (ores.length == 0) {
                Log.warn("[ASTEROID] No ores available for this planet!");
                return;
            }

            int oresSpawned = 0;

            while (oresSpawned < oreCount) {

                // Random position within the impact radius
                int oreX = centerX + Mathf.range(radius);
                int oreY = centerY + Mathf.range(radius);

                Tile oreTile = Vars.world.tile(oreX, oreY);

                // Check if tile is valid for ore placement
                if (oreTile != null &&
                        oreTile.floor() != null &&
                        oreTile.floor().hasSurface() &&
                        oreTile.overlay() == Blocks.air &&
                        !(oreTile.block() instanceof CoreBlock)) {

                    Block randomOre = ores[Mathf.random(ores.length - 1)];

                    if (randomOre != null) {
                        oreTile.setOverlay(randomOre);
                        oresSpawned++;
                        Log.info("[ASTEROID] Spawned ore: " + randomOre.name + " at (" + oreX + ", " + oreY + ")");
                    }
                }
            }

            Log.info("[ASTEROID] Successfully spawned " + oresSpawned + " out of " + oreCount);
        } else {
            Log.info("[ASTEROID] No ores spawned this impact (75% chance to skip)");
        }
    }

    /**
     * Brings a planet invasion to a tile (floor from another planet in the game)
     * @param t - tile to change
     * @param gen - generator from a planet in mindustry
     */
    private static void genTile(Tile t, PlanetGenerator gen){
        if (t.data > 0) return;
        if (t.block() instanceof CoreBlock) return;

        TileGen tg = new TileGen();
        Vec3 pos = new Vec3();
        float fx = t.x / (float) Vars.world.width();
        float fy = t.y / (float) Vars.world.height();

        // Map to unit sphere
        pos.set(fx * 2 - 1, 0, fy * 2 - 1).nor();

        if (gen instanceof ErekirPlanetGenerator e) e.genTile(pos, tg);
        else if (gen instanceof SerpuloPlanetGenerator s) s.genTile(pos, tg);
        else if (gen instanceof TantrosPlanetGenerator a) a.genTile(pos, tg);

        BlockLogic.destroyTile(t);
        t.setFloor(tg.floor.asFloor());

        // 15% chance to preserve existing ore in that tile
        if (t.overlay() == Blocks.air || Math.random() > 0.15) {
            t.setOverlay(Blocks.air);
        }
    }
}

