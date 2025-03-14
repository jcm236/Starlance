uniform sampler2D DiffuseDepthSampler;
uniform sampler2D DiffuseSampler0;
uniform sampler2D MainDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {

    gl_FragDepth = texture(MainDepthSampler, texCoord).r;
    float fragDepth = texture(DiffuseDepthSampler, texCoord).r;
    fragColor = vec4(0.0, 0.0, 0.0, 0.0);

    if (gl_FragDepth >= fragDepth) {
        gl_FragDepth = fragDepth;
        fragColor = texture(DiffuseSampler0, texCoord);
    }
}
