out vec2 texCoord;
out vec2 blurTextureCoords[11];

layout(location = 0) in vec3 Position;

uniform vec2 OutSize;

void main() {
	gl_Position = vec4(Position, 1.0);
	texCoord = Position.xy / 2.0 + 0.5;
	float pixelSize = 1.0 / OutSize.x;

	for (int i = -5; i <= 5; i++) {
		blurTextureCoords[i + 5] = texCoord + vec2(0, pixelSize * i);
	}
}
