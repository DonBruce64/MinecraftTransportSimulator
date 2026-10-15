#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    if (clipPosition.w > 0.0 && clipPosition.z > clipPosition.w * 0.9999) {
        clipPosition.z = clipPosition.w * 0.9999;
    }
    gl_Position = clipPosition;
    vertexColor = Color;
    texCoord0 = UV0;
}
