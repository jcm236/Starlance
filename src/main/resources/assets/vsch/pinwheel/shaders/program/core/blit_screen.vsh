out vec2 texCoord;

layout(location = 0) in vec3 Position;

void main() {
	gl_Position = vec4(Position, 1.0);
	texCoord = Position.xy / 2.0 + 0.5;
}
