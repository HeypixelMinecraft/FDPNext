#version 120

/**
 * Copies the main framebuffer to the blur framebuffer.
 * Used before glGenerateMipmap to create a mipmap chain for blur sampling.
 */

uniform sampler2D textureIn;

void main() {
    gl_FragColor = texture2D(textureIn, gl_TexCoord[0].st);
}
