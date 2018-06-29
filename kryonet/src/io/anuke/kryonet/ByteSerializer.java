package io.anuke.kryonet;

import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.serialization.Serialization;
import io.anuke.mindustry.net.Packet;
import io.anuke.mindustry.net.Registrator;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.net.Net.packetPoolLock;

public class ByteSerializer implements Serialization {

    @Override
    public void write(ByteBuffer byteBuffer, Object o) {
        if(o instanceof FrameworkMessage){
            byteBuffer.put((byte)-2); //code for framework message
            FrameworkSerializer.write(byteBuffer, (FrameworkMessage)o);
        }else {
            if (!(o instanceof Packet))
                throw new RuntimeException("All sent objects must implement be Packets! Class: " + o.getClass());
            byte id = Registrator.getID(o.getClass());
            if (id == -1)
                throw new RuntimeException("Unregistered class: " + ClassReflection.getSimpleName(o.getClass()));
            byteBuffer.put(id);
            ((Packet) o).write(byteBuffer);
            synchronized (packetPoolLock) {
                Pools.free(o);
            }
        }
    }

    @Override
    public Object read(ByteBuffer byteBuffer) {
        byte id = byteBuffer.get();
        if(id == -2){
           return FrameworkSerializer.read(byteBuffer);
        }else{
            Class<?> type = Registrator.getByID(id);
            synchronized (packetPoolLock) {
                Packet packet = (Packet) Pools.obtain(type);
                packet.read(byteBuffer);
                return packet;
            }
        }
    }

    @Override
    public int getLengthLength() {
        return 2;
    }

    @Override
    public void writeLength(ByteBuffer byteBuffer, int i) {
        byteBuffer.putShort((short)i);
    }

    @Override
    public int readLength(ByteBuffer byteBuffer) {
        return byteBuffer.getShort();
    }
}
