#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform float Time;

out vec4 fragColor;

float hash(vec2 value) {
    return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec2 uv = texCoord;
    uv.x += sin(uv.y * 90.0 + Time * 7.0) * 0.0016;
    float shift = oneTexel.x * 1.5;
    vec3 color;
    color.r = texture(DiffuseSampler, uv + vec2(shift, 0.0)).r;
    color.g = texture(DiffuseSampler, uv).g;
    color.b = texture(DiffuseSampler, uv - vec2(shift, 0.0)).b;
    float luma = dot(color, vec3(0.3, 0.59, 0.11));
    color = mix(vec3(luma), color, 0.62);
    color *= vec3(1.04, 0.98, 0.84);
    float scan = 0.84 + 0.16 * sin((uv.y * InSize.y + Time * 12.0) * 3.14159);
    color *= scan;
    float roll = fract(Time * 0.13);
    float bar = smoothstep(0.0, 0.018, abs(uv.y - roll));
    color *= mix(0.62, 1.0, bar);
    float noise = hash(vec2(floor(uv.x * InSize.x), floor(uv.y * InSize.y) + Time * 30.0));
    color += (noise - 0.5) * 0.07;
    vec2 centered = uv - 0.5;
    float vignette = clamp(1.0 - dot(centered, centered) * 1.25, 0.28, 1.0);
    color *= vignette;
    fragColor = vec4(color, 1.0);
}
