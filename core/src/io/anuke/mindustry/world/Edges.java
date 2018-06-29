package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.util.Arrays;

public class Edges {
    private static final int maxSize = 11;
    private static final int maxRadius = 12;
    private static GridPoint2[][] edges = new GridPoint2[maxSize][0];
    private static GridPoint2[][] edgeInside = new GridPoint2[maxSize][0];
    private static Vector2[][] polygons = new Vector2[maxRadius*2][0];

    static{
        for(int i = 0; i < maxRadius*2; i ++){
            polygons[i] = Geometry.pixelCircle((i + 1)/2f);
        }

        for(int i = 0; i < maxSize; i ++){
            int bot = -(int)(i/2f) - 1;
            int top = (int)(i/2f+0.5f) + 1;
            edges[i] = new GridPoint2[(i + 1) * 4];

            int idx = 0;

            for(int j = 0; j < i + 1; j ++){
                //bottom
                edges[i][idx ++] = new GridPoint2(bot + 1 + j, bot);
                //top
                edges[i][idx ++] = new GridPoint2(bot + 1 + j, top);
                //left
                edges[i][idx ++] = new GridPoint2(bot, bot + j + 1);
                //right
                edges[i][idx ++] = new GridPoint2(top, bot + j + 1);
            }

            Arrays.sort(edges[i], (e1, e2) -> Float.compare(Mathf.atan2(e1.x, e1.y), Mathf.atan2(e2.x, e2.y)));

            edgeInside[i] = new GridPoint2[edges[i].length];

            for(int j = 0; j < edges[i].length; j ++){
                GridPoint2 point = edges[i][j];
                edgeInside[i][j] = new GridPoint2(Mathf.clamp(point.x, -(int)((i)/2f), (int)(i/2f + 0.5f)),
                        Mathf.clamp(point.y, -(int)((i)/2f), (int)(i/2f + 0.5f)));
            }
        }
    }

    public static Vector2[] getPixelPolygon(float radius){
        if(radius < 1 || radius > maxRadius) throw new RuntimeException("Polygon size must be between 1 and " + maxRadius);
        return polygons[(int)(radius*2) - 1];
    }

    public static synchronized GridPoint2[] getEdges(int size){
        if(size < 0 || size > maxSize) throw new RuntimeException("Block size must be between 0 and " + maxSize);

        return edges[size - 1];
    }

    public static synchronized GridPoint2[] getInsideEdges(int size){
        if(size < 0 || size > maxSize) throw new RuntimeException("Block size must be between 0 and " + maxSize);

        return edgeInside[size - 1];
    }

    public static synchronized int getEdgeAmount(int size){
        return getEdges(size).length;
    }
}
