package io.redspace.irons_artifice.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

public class MuzzleFlashParticle extends TextureSheetParticle {
    private static final String FIRE_SUFFIX = "_fire";
    private static final String TINTED_MASKED_SUFFIX = "_tinted_masked";
    private static final String WHITE_MASK_SUFFIX = "_white_mask";

    private final SpriteSet sprites;
    private final boolean tinted;
    private final boolean mirrorHorizontal;
    private final boolean mirrorVertical;

    @Nullable
    private TextureAtlasSprite whiteMaskSprite;

    public MuzzleFlashParticle(ClientLevel level, double x, double y, double z,
                               double xa, double ya, double za, SpriteSet sprites,
                               float tintR, float tintG, float tintB) {
        super(level, x, y, z, xa, ya, za);
        this.setSprite(sprites.get(0, 1));
        this.sprites = sprites;
        this.tinted = !(tintR < 0f || tintG < 0 || tintB < 0);
        this.lifetime = 3;
        this.xd = xa;
        this.yd = ya;
        this.zd = za;
        this.quadSize = 1;
        this.rCol = tinted ? tintR : 1f;
        this.gCol = tinted ? tintG : 1f;
        this.bCol = tinted ? tintB : 1f;
        this.mirrorHorizontal = level.getRandom().nextBoolean();
        this.mirrorVertical = level.getRandom().nextBoolean();
        this.roll = level.getRandom().nextInt(4) * Mth.HALF_PI;
        this.oRoll = roll;
        updateSprites();
    }

    @Override
    protected float getU0() {
        return mirrorHorizontal ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return mirrorHorizontal ? super.getU0() : super.getU1();
    }

    @Override
    protected float getV0() {
        return mirrorVertical ? super.getV1() : super.getV0();
    }

    @Override
    protected float getV1() {
        return mirrorVertical ? super.getV0() : super.getV1();
    }

    @Override
    public void tick() {
        super.tick();
        updateSprites();
    }

    private void updateSprites() {
        TextureAtlasSprite fireSprite = sprites.get(age, lifetime);
        if (!tinted) {
            setSprite(fireSprite);
            whiteMaskSprite = null;
            return;
        }

        ResourceLocation fireName = fireSprite.contents().name();
        String path = fireName.getPath();
        if (!path.endsWith(FIRE_SUFFIX)) {
            setSprite(fireSprite);
            whiteMaskSprite = null;
            return;
        }

        String basePath = path.substring(0, path.length() - FIRE_SUFFIX.length());
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_PARTICLES);
        setSprite(atlas.getSprite(fireName.withPath(basePath + TINTED_MASKED_SUFFIX)));
        whiteMaskSprite = atlas.getSprite(fireName.withPath(basePath + WHITE_MASK_SUFFIX));
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTick) {
        super.render(buffer, camera, partialTick);
        if (!tinted || whiteMaskSprite == null) {
            return;
        }
        TextureAtlasSprite saved = this.sprite;
        float savedR = this.rCol;
        float savedG = this.gCol;
        float savedB = this.bCol;
        this.sprite = whiteMaskSprite;
        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;
        super.render(buffer, camera, partialTick);
        this.sprite = saved;
        this.rCol = savedR;
        this.gCol = savedG;
        this.bCol = savedB;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }
    public static class Provider implements ParticleProvider<MuzzleFlashParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public @Nullable Particle createParticle(MuzzleFlashParticleOption options, ClientLevel level,
                                                 double x, double y, double z,
                                                 double xa, double ya, double za) {
            return new MuzzleFlashParticle(level, x, y, z, xa, ya, za, this.sprite, options.r(), options.g(), options.b());
        }
    }
}
