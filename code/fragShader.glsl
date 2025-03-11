#version 430 core
in vec2 tc;           // Texture coordinates
out vec4 color;       // Output color

uniform mat4 mv_matrix;
uniform mat4 p_matrix;
layout (binding=0) uniform sampler2D s; // Texture sampler
uniform bool isPlane;  // Flag to determine if we're rendering the plane

void main(void)
{
    if (isPlane) {
        // If rendering the plane, use white color
        color = vec4(1.0, 1.0, 1.0, 1.0);  // White color for the plane
    } else {
        // Otherwise, use the texture (car model)
        color = texture(s, tc);  // Apply texture to the car
    }
}