package io.anuke.mindustry.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class FloorRenderer {
    private final static int chunksize = 64;

    private Chunk[][] cache;
    private CacheBatch cbatch;
    private IntSet drawnLayerSet = new IntSet();
    private IntArray drawnLayers = new IntArray();

    public FloorRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, this::clearTiles);
    }

    public void drawFloor(){
        if(cache == null){
            return;
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        for(int x = -crangex; x <= crangex; x++) {
            for (int y = -crangey; y <= crangey; y++) {
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                fillChunk(worldx * chunksize * tilesize, worldy * chunksize * tilesize);
            }
        }

        int layers = CacheLayer.values().length;

        drawnLayers.clear();
        drawnLayerSet.clear();

        //preliminary layer check:
        for(int x = -crangex; x <= crangex; x++){
            for(int y = -crangey; y <= crangey; y++){
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if (!Mathf.inBounds(worldx, worldy, cache))
                    continue;

                Chunk chunk = cache[worldx][worldy];

                //loop through all layers, and add layer index if it exists
                for(int i = 0; i < layers - 1; i ++){
                    if(chunk.caches[i] != -1){
                        drawnLayerSet.add(i);
                    }
                }
            }
        }

        IntSetIterator it = drawnLayerSet.iterator();
        while(it.hasNext){
            drawnLayers.add(it.next());
        }

        drawnLayers.sort();

        Graphics.end();
        beginDraw();

        for(int i = 0; i < drawnLayers.size; i ++) {
            CacheLayer layer = CacheLayer.values()[drawnLayers.get(i)];

            drawLayer(layer);
        }

        endDraw();
        Graphics.begin();
    }

    public void beginDraw(){
        if(cache == null){
            return;
        }

        cbatch.setProjectionMatrix(Core.camera.combined);
        cbatch.beginDraw();

        Gdx.gl.glEnable(GL20.GL_BLEND);
    }

    public void endDraw(){
        if(cache == null){
            return;
        }

        cbatch.endDraw();
    }

    public void drawLayer(CacheLayer layer){
        if(cache == null){
            return;
        }

        OrthographicCamera camera = Core.camera;

        int crangex = (int)(camera.viewportWidth * camera.zoom / (chunksize * tilesize))+1;
        int crangey = (int)(camera.viewportHeight * camera.zoom / (chunksize * tilesize))+1;

        layer.begin();

        for (int x = -crangex; x <= crangex; x++) {
            for (int y = -crangey; y <= crangey; y++) {
                int worldx = Mathf.scl(camera.position.x, chunksize * tilesize) + x;
                int worldy = Mathf.scl(camera.position.y, chunksize * tilesize) + y;

                if(!Mathf.inBounds(worldx, worldy, cache)){
                    continue;
                }

                Chunk chunk = cache[worldx][worldy];
                if(chunk.caches[layer.ordinal()] == -1) continue;
                cbatch.drawCache(chunk.caches[layer.ordinal()]);
            }
        }

        layer.end();
    }

    private void fillChunk(float x, float y){
        Draw.color(Color.GRAY);
        Draw.crect("white", x, y, chunksize * tilesize, chunksize * tilesize);
        Draw.color();
    }

    private void cacheChunk(int cx, int cy){
        Chunk chunk = cache[cx][cy];
        //long time = TimeUtils.nanoTime();

        ObjectSet<CacheLayer> used = new ObjectSet<>();

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++) {
            for (int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++) {
                Tile tile = world.tile(tilex, tiley);
                if (tile != null){
                    used.add(tile.block().cacheLayer == CacheLayer.walls ?
                            CacheLayer.walls  : tile.floor().cacheLayer);
                }
            }
        }

        for(CacheLayer layer : used){
            cacheChunkLayer(cx, cy, chunk, layer);
        }

       // Log.info("Time to cache a chunk: {0}", TimeUtils.timeSinceNanos(time) / 1000000f);
    }

    private void cacheChunkLayer(int cx, int cy, Chunk chunk, CacheLayer layer){

        Graphics.useBatch(cbatch);
        cbatch.begin();

        for(int tilex = cx * chunksize; tilex < (cx + 1) * chunksize; tilex++){
            for(int tiley = cy * chunksize; tiley < (cy + 1) * chunksize; tiley++){
                Tile tile = world.tile(tilex, tiley);
                if(tile == null) continue;

                if(tile.floor().cacheLayer == layer && tile.block().cacheLayer != CacheLayer.walls){
                    tile.floor().draw(tile);
                }else if(tile.floor().cacheLayer.ordinal() < layer.ordinal() && tile.block().cacheLayer != CacheLayer.walls && layer != CacheLayer.walls){
                    tile.floor().drawNonLayer(tile);
                }

                if(tile.block().cacheLayer == layer && layer == CacheLayer.walls){
                    Block block = tile.block();
                    block.draw(tile);
                }
            }
        }

        cbatch.end();
        Graphics.popBatch();
        chunk.caches[layer.ordinal()] = cbatch.getLastCache();
    }

    private class Chunk{
        int[] caches = new int[CacheLayer.values().length];
    }

    public void clearTiles(){
        if(cbatch != null) cbatch.dispose();

        Timers.mark();

        int chunksx = Mathf.ceil((float)world.width() / chunksize), chunksy = Mathf.ceil((float)world.height() / chunksize);
        cache = new Chunk[chunksx][chunksy];
        cbatch = new CacheBatch(world.width()*world.height()*4*4);

        Log.info("Time to create: {0}", Timers.elapsed());

        Timers.mark();

        for (int x = 0; x < chunksx; x++) {
            for (int y = 0; y < chunksy; y++) {
                cache[x][y] = new Chunk();
                Arrays.fill(cache[x][y].caches, -1);

                cacheChunk(x, y);
            }
        }

        Log.info("Time to cache: {0}", Timers.elapsed());
    }

    static ShaderProgram createDefaultShader () {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "varying vec2 v_texCoords;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "void main()\n"//
                + "{\n" //
                + "  gl_FragColor = texture2D(u_texture, v_texCoords);\n" //
                + "}";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
