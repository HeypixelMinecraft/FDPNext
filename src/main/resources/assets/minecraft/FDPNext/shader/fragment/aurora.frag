#version 120

uniform vec2 resolution;
uniform float time;

// Simplex-like noise using sin/cos (cheap, no texture needed)
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Fractal noise for organic flow
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = rot * p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    vec2 p = uv;

    // Slow time for smooth flow
    float t = time * 0.15;

    // Warp the UV for organic flow
    vec2 q = vec2(
        fbm(p + vec2(0.0, t * 0.3)),
        fbm(p + vec2(5.2, t * 0.2))
    );

    vec2 r = vec2(
        fbm(p + 4.0 * q + vec2(1.7, 9.2) + t * 0.15),
        fbm(p + 4.0 * q + vec2(8.3, 2.8) + t * 0.12)
    );

    float f = fbm(p + 4.0 * r);

    // Aurora color palette: deep dark base + blue/purple/cyan/green bands
    vec3 col1 = vec3(0.05, 0.05, 0.08);   // deep dark
    vec3 col2 = vec3(0.10, 0.08, 0.25);   // deep purple
    vec3 col3 = vec3(0.15, 0.25, 0.55);   // blue
    vec3 col4 = vec3(0.10, 0.45, 0.50);   // cyan
    vec3 col5 = vec3(0.15, 0.40, 0.25);   // green

    // Mix colors based on noise
    vec3 color = mix(col1, col2, clamp(f * f * 2.0, 0.0, 1.0));
    color = mix(color, col3, clamp(length(q) * 0.6, 0.0, 1.0));
    color = mix(color, col4, clamp(length(r.x) * 0.4, 0.0, 1.0));
    color = mix(color, col5, clamp(f * r.y * 1.5, 0.0, 1.0));

    // Add subtle brightness variation for aurora shimmer
    float shimmer = sin(uv.x * 6.0 + t * 2.0) * 0.02 + 0.98;
    color *= shimmer;

    // Slight vignette
    float vig = 1.0 - length((uv - 0.5) * 1.2) * 0.5;
    color *= vig;

    gl_FragColor = vec4(color, 1.0);
}
