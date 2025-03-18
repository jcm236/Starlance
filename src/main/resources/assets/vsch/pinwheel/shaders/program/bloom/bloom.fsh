//#include veil:color_utilities
//#define E 2.71828

uniform sampler2D DiffuseSampler0;
uniform sampler2D BlurSampler;
uniform sampler2D BloomSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec4 blurColor = texture(BlurSampler, texCoord);
	vec4 bloomColor = texture(BloomSampler, texCoord);
	fragColor = texture(DiffuseSampler0, texCoord) * (1 - blurColor.a);

	fragColor += bloomColor;
	fragColor += blurColor * (1.5 - blurColor.a);
}
