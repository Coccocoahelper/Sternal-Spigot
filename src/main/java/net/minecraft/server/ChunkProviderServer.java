package net.minecraft.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Type;
import org.bukkit.event.world.ChunkLoadEvent;
// CraftBukkit end

public class ChunkProviderServer implements IChunkProvider {

    private LongHashset a = new LongHashset(); // CraftBukkit
    private Chunk b;
    private IChunkProvider c;
    private IChunkLoader d;
    private LongHashtable<Chunk> e = new LongHashtable<Chunk>(); // CraftBukkit
    private List f = new ArrayList();
    private WorldServer g;

    public ChunkProviderServer(WorldServer worldserver, IChunkLoader ichunkloader, IChunkProvider ichunkprovider) {
        this.b = new EmptyChunk(worldserver, new byte['\u8000'], 0, 0);
        this.g = worldserver;
        this.d = ichunkloader;
        this.c = ichunkprovider;
    }

    // CraftBukkit start
    public org.bukkit.Chunk[] getLoadedChunks() {
        Object[] chunks = e.values().toArray();
        org.bukkit.Chunk[] craftChunks = new CraftChunk[chunks.length];

        for(int cnt =0;cnt<chunks.length;cnt++) {
            craftChunks[cnt] = ((Chunk)chunks[cnt]).bukkitChunk;
        }

        return craftChunks;
    }

    public boolean a(int i, int j) {
        return this.e.containsKey(i, j);
    }

    public void saveChunk(int x, int z) {
        Chunk chunk = this.b(x, z);
        chunk.e();
        this.b(chunk);
        this.a(chunk);
    }

    public boolean unloadChunk(int x, int z, boolean save, boolean safe) {
        if (safe && isChunkInUse(x, z))
            return false;
        
        Chunk chunk = this.b(x, z);
        
        if (save)
            saveChunk(x, z);

        this.a.remove(x, z);
        this.e.remove(x, z);
        this.f.remove(chunk);
        return true;
    }
    
    public boolean unloadChunkRequest(int x, int z, boolean safe) {
        if (safe && isChunkInUse(x, z))
            return false;
        c(x, z);
        return true;
    }

    public boolean isChunkInUse(int x, int z) {
        CraftServer server = g.getServer();
        Player[] players = server.getOnlinePlayers();
        for (Player player : players) {
            Location loc = player.getLocation();
            if (loc.getWorld() != g.getWorld()) {
                continue;
            }
            // If the chunk is within 256 blocks of a player, refuse to accept the unload request
            // This is larger than the distance of loaded chunks that actually surround a player
            // The player is the center of a 21x21 chunk grid, so the edge is 10 chunks (160 blocks) away from the player
            if (Math.abs(loc.getBlockX() - (x << 4)) <= 256 && Math.abs(loc.getBlockZ() - (z << 4)) <= 256) {
                return false;
            }
        }
        return true;
    }

    public Chunk loadChunk(int x, int z, boolean generate) {
        if (generate) {
            // Use the default variant of loadChunk when generate == true.
            return d(x, z);
        }

        this.a.remove(x, z);
        Chunk chunk = (Chunk) this.e.get(x, z);

        if (chunk == null) {
            chunk = this.e(x, z);

            if (chunk != null) {
                this.e.put(x, z, chunk);
                this.f.add(chunk);

                chunk.c();
                chunk.d();

                if (!chunk.n && this.a(x + 1, z + 1) && this.a(x, z + 1) && this.a(x + 1, z)) {
                    this.a(this, x, z);
                }

                if (this.a(x - 1, z) && !this.b(x - 1, z).n && this.a(x - 1, z + 1) && this.a(x, z + 1) && this.a(x - 1, z)) {
                    this.a(this, x - 1, z);
                }

                if (this.a(x, z - 1) && !this.b(x, z - 1).n && this.a(x + 1, z - 1) && this.a(x, z - 1) && this.a(x + 1, z)) {
                    this.a(this, x, z - 1);
                }

                if (this.a(x - 1, z - 1) && !this.b(x - 1, z - 1).n && this.a(x - 1, z - 1) && this.a(x, z - 1) && this.a(x - 1, z)) {
                    this.a(this, x - 1, z - 1);
                }
            }
        }
        return chunk;
    }
    // CraftBukkit end
    
    public void c(int i, int j) {
        int k = i * 16 + 8 - this.g.spawnX;
        int l = j * 16 + 8 - this.g.spawnZ;
        short short1 = 128;

        if (k < -short1 || k > short1 || l < -short1 || l > short1) {
            this.a.add(i, j); // CraftBukkit
        }
    }

    public Chunk d(int i, int j) {
        // CraftBukkit start
        this.a.remove(i, j);
        Chunk chunk = (Chunk) this.e.get(i, j);
        // CraftBukkit end

        if (chunk == null) {
            chunk = this.e(i, j);
            if (chunk == null) {
                if (this.c == null) {
                    chunk = this.b;
                } else {
                    chunk = this.c.b(i, j);
                }
            }

            this.e.put(i, j, chunk); // CraftBukkit
            this.f.add(chunk);
            if (chunk != null) {
                chunk.c();
                chunk.d();
            }

            // CraftBukkit start
            CraftServer server = g.getServer();
            if (server != null) {
                /*
                 * If it's a new world, the first few chunks are generated inside
                 * the World constructor. We can't reliably alter that, so we have
                 * no way of creating a CraftWorld/CraftServer at that point.
                 */
                server.getPluginManager().callEvent(new ChunkLoadEvent(Type.CHUNK_LOADED, chunk.bukkitChunk));
            }
            // CraftBukkit end

            if (!chunk.n && this.a(i + 1, j + 1) && this.a(i, j + 1) && this.a(i + 1, j)) {
                this.a(this, i, j);
            }

            if (this.a(i - 1, j) && !this.b(i - 1, j).n && this.a(i - 1, j + 1) && this.a(i, j + 1) && this.a(i - 1, j)) {
                this.a(this, i - 1, j);
            }

            if (this.a(i, j - 1) && !this.b(i, j - 1).n && this.a(i + 1, j - 1) && this.a(i, j - 1) && this.a(i + 1, j)) {
                this.a(this, i, j - 1);
            }

            if (this.a(i - 1, j - 1) && !this.b(i - 1, j - 1).n && this.a(i - 1, j - 1) && this.a(i, j - 1) && this.a(i - 1, j)) {
                this.a(this, i - 1, j - 1);
            }
        }

        return chunk;
    }

    public Chunk b(int i, int j) {
        Chunk chunk = (Chunk) this.e.get(i, j); // CraftBukkit

        return chunk == null ? (this.g.x ? this.d(i, j) : this.b) : chunk;
    }

    private Chunk e(int i, int j) {
        if (this.d == null) {
            return null;
        } else {
            try {
                Chunk chunk = this.d.a(this.g, i, j);

                if (chunk != null) {
                    chunk.r = this.g.e;
                }

                return chunk;
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        }
    }

    private void a(Chunk chunk) {
        if (this.d != null) {
            try {
                this.d.b(this.g, chunk);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void b(Chunk chunk) {
        if (this.d != null) {
            try {
                chunk.r = this.g.e;
                this.d.a(this.g, chunk);
            } catch (Exception ioexception) { // CraftBukkit - IOException -> Exception
                ioexception.printStackTrace();
            }
        }
    }

    public void a(IChunkProvider ichunkprovider, int i, int j) {
        Chunk chunk = this.b(i, j);

        if (!chunk.n) {
            chunk.n = true;
            if (this.c != null) {
                this.c.a(ichunkprovider, i, j);
                chunk.f();
            }
        }
    }

    public boolean a(boolean flag, IProgressUpdate iprogressupdate) {
        int i = 0;

        for (int j = 0; j < this.f.size(); ++j) {
            Chunk chunk = (Chunk) this.f.get(j);

            if (flag && !chunk.p) {
                this.a(chunk);
            }

            if (chunk.a(flag)) {
                this.b(chunk);
                chunk.o = false;
                ++i;
                if (i == 24 && !flag) {
                    return false;
                }
            }
        }

        if (flag) {
            if (this.d == null) {
                return true;
            }

            this.d.b();
        }

        return true;
    }

    public boolean a() {
        if (!this.g.C) {
            for (int i = 0; i < 100; ++i) {
                if (!this.a.isEmpty()) {
                    // CraftBukkit start
                    long chunkcoordinates = this.a.popFirst();
                    Chunk chunk = e.get(chunkcoordinates);
                    if (chunk == null) continue;
                    // CraftBukkit end
                    chunk.e();
                    this.b(chunk);
                    this.a(chunk);
                    // this.a.remove(chunkcoordinates); // CraftBukkit
                    this.e.remove(chunkcoordinates);
                    this.f.remove(chunk);
                }
            }

            if (this.d != null) {
                this.d.a();
            }
        }

        return this.c.a();
    }

    public boolean b() {
        return !this.g.C;
    }
}
