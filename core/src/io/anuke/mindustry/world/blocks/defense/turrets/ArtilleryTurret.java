package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

/**Artillery turrets have special shooting calculations done to hit targets.*/
public class ArtilleryTurret extends ItemTurret {

    public ArtilleryTurret(String name) {
        super(name);
    }

    @Override
    protected void shoot(Tile tile, AmmoType ammo){
        TurretEntity entity = tile.entity();

        entity.recoil = recoil;
        entity.heat = 1f;

        AmmoType type = peekAmmo(tile);
        useAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2);

        Vector2 predict = Predict.intercept(tile, entity.target, type.bullet.speed);

        float dst = entity.distanceTo(predict.x, predict.y);
        float maxTraveled = type.bullet.lifetime * type.bullet.speed;

        Bullet.create(ammo.bullet, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y,
                entity.rotation + Mathf.range(inaccuracy + type.inaccuracy), dst/maxTraveled);

        effects(tile);
    }
}
