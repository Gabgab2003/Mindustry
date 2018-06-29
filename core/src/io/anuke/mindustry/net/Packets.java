package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.io.IOUtils;
import io.anuke.ucore.util.Mathf;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.world;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect implements ImportantPacket{
        public int id;
        public String addressTCP;
    }

    public static class Disconnect implements ImportantPacket{
        public int id;
    }

    public static class WorldStream extends Streamable{

    }

    public static class ConnectPacket implements Packet{
        public int version;
        public String name, uuid, usid;
        public boolean mobile;
        public int color;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(Version.build);
            IOUtils.writeString(buffer, name);
            IOUtils.writeString(buffer, usid);
            buffer.put(mobile ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put(Base64Coder.decode(uuid));
        }

        @Override
        public void read(ByteBuffer buffer) {
            version = buffer.getInt();
            name = IOUtils.readString(buffer);
            usid = IOUtils.readString(buffer);
            mobile = buffer.get() == 1;
            color = buffer.getInt();
            byte[] idbytes = new byte[8];
            buffer.get(idbytes);
            uuid = new String(Base64Coder.encode(idbytes));
        }
    }

    public static class InvokePacket implements Packet{
        public byte type;

        public ByteBuffer writeBuffer;
        public int writeLength;

        @Override
        public void read(ByteBuffer buffer) {
            type = buffer.get();
            writeLength = buffer.getShort();
            byte[] bytes = new byte[writeLength];
            buffer.get(bytes);
            writeBuffer = ByteBuffer.wrap(bytes);
        }

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(type);
            buffer.putShort((short)writeLength);

            writeBuffer.position(0);
            for(int i = 0; i < writeLength; i ++){
                buffer.put(writeBuffer.get());
            }
        }
    }

    public static class SnapshotPacket implements Packet, UnimportantPacket{
        @Override
        public void read(ByteBuffer buffer) {

        }

        @Override
        public void write(ByteBuffer buffer) {

        }
    }

    public static class ClientSnapshotPacket implements Packet{
        //snapshot meta
        public int lastSnapshot;
        public int snapid;
        public long timeSent;
        //player snapshot data
        public float x, y, pointerX, pointerY, rotation, baseRotation, xv, yv;
        public Tile mining;
        public boolean boosting, shooting;

        @Override
        public void write(ByteBuffer buffer) {
            Player player = Vars.players[0];

            buffer.putInt(lastSnapshot);
            buffer.putInt(snapid);
            buffer.putLong(TimeUtils.millis());

            buffer.putFloat(player.x);
            buffer.putFloat(player.y);
            buffer.putFloat(player.pointerX);
            buffer.putFloat(player.pointerY);
            buffer.put(player.isBoosting ? (byte)1 : 0);
            buffer.put(player.isShooting ? (byte)1 : 0);

            buffer.put((byte)(Mathf.clamp(player.getVelocity().x, -Unit.maxAbsVelocity, Unit.maxAbsVelocity) * Unit.velocityPercision));
            buffer.put((byte)(Mathf.clamp(player.getVelocity().y, -Unit.maxAbsVelocity, Unit.maxAbsVelocity) * Unit.velocityPercision));
            //saving 4 bytes, yay?
            buffer.putShort((short)(player.rotation*2));
            buffer.putShort((short)(player.baseRotation*2));

            buffer.putInt(player.getMineTile() == null ? -1 : player.getMineTile().packedPosition());
        }

        @Override
        public void read(ByteBuffer buffer) {
            lastSnapshot = buffer.getInt();
            snapid = buffer.getInt();
            timeSent = buffer.getLong();

            x = buffer.getFloat();
            y = buffer.getFloat();
            pointerX = buffer.getFloat();
            pointerY = buffer.getFloat();
            boosting = buffer.get() == 1;
            shooting = buffer.get() == 1;
            xv = buffer.get() / Unit.velocityPercision;
            yv = buffer.get() / Unit.velocityPercision;
            rotation = buffer.getShort()/2f;
            baseRotation = buffer.getShort()/2f;
            mining = world.tile(buffer.getInt());
        }
    }

    public enum KickReason{
        kick, invalidPassword, clientOutdated, serverOutdated, banned, gameover(true), recentKick, nameInUse, idInUse, fastShoot, nameEmpty;
        public final boolean quiet;

        KickReason(){ quiet = false; }

        KickReason(boolean quiet){
            this.quiet = quiet;
        }
    }

    public enum AdminAction{
        kick, ban, trace
    }

    /**Marks the beginning of a stream.*/
    public static class StreamBegin implements Packet{
        private static int lastid;

        public int id = lastid ++;
        public int total;
        public Class<? extends Streamable> type;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
            buffer.putInt(total);
            buffer.put(Registrator.getID(type));
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
            total = buffer.getInt();
            type = (Class<? extends Streamable>)Registrator.getByID(buffer.get());
        }
    }

    public static class StreamChunk implements Packet{
        public int id;
        public byte[] data;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
            buffer.putShort((short)data.length);
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
            data = new byte[buffer.getShort()];
            buffer.get(data);
        }
    }
}
