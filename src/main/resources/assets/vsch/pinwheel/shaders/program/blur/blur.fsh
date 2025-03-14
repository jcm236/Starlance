uniform sampler2D DiffuseSampler0;
uniform vec2 OutSize;

in vec2 texCoord;
in vec2 blurTextureCoords[11];

out vec4 fragColor;

const float offset = 3;

void main() {
    fragColor = vec4(0.0);
    fragColor += texture(DiffuseSampler0, blurTextureCoords[0]) * 0.0093;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[1]) * 0.028002;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[2]) * 0.065984;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[3]) * 0.121703;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[4]) * 0.175713;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[5]) * 0.198596;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[6]) * 0.175713;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[7]) * 0.121703;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[8]) * 0.065984;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[9]) * 0.028002;
    fragColor += texture(DiffuseSampler0, blurTextureCoords[10]) * 0.0093;
}