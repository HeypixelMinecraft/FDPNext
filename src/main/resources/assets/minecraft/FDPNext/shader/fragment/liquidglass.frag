/**
 * Liquid Glass (Clear) - adapted from Jacquesqwq/LiquidGlassShader liquidGlassV2Clear.frag.
 *
 * The original relies on a custom vertex shader (liquidGlassV2.vsh, not published) to supply
 * vMidPointNDC / vLocalOffsetNDC varyings. FDPNext shares a single passthrough vertex.vert, so
 * this version derives those values in-fragment from gl_FragCoord + uniforms (uScreenSize,
 * uQuadCenter, uQuadSize) instead. Everything else (superellipse SDF, refraction, grain, rim
 * glow) is kept faithful to the original.
 */

#version 120

uniform sampler2D uBlurTex;

uniform float uPowerFactor;
uniform float uNoise;
uniform float uRefractionPower;
uniform float uGlowWeight;
uniform float uGlowBias;
uniform float uGlowEdge0;
uniform float uGlowEdge1;

// Replaces the missing vertex shader: quad geometry in display pixels (bottom-left origin,
// matching gl_FragCoord) so we can rebuild the NDC midpoint / local offset here.
uniform vec2 uScreenSize;
uniform vec2 uQuadCenter;
uniform vec2 uQuadSize;

const float M_E = 2.718281828459045;
const vec2 CENTER = vec2(0.5);

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

float f(float x) {
    return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7);
}

float sdSuperellipse(vec2 p, float n, float r) {
    vec2 absP = abs(p);
    float numerator = pow(absP.x, n) + pow(absP.y, n) - pow(r, n);
    float denominator = n * sqrt(pow(absP.x, 2.0 * n - 2.0) + pow(absP.y, 2.0 * n - 2.0)) + 0.00001;
    return numerator / denominator;
}

float Glow(vec2 uv) {
    vec2 glowUV = uv * 2.0 - 1.0;
    return sin(atan(glowUV.y, glowUV.x) - 0.5);
}

bool OutOfBounds(vec2 uv) { return max(uv.x, uv.y) > 1.0 || min(uv.x, uv.y) < 0.0; }

void main() {
    vec2 localUV = gl_TexCoord[0].xy;

    // Rebuild the varyings the original vertex shader produced.
    vec2 fragNDC = (gl_FragCoord.xy / uScreenSize) * 2.0 - 1.0;
    vec2 midNDC = (uQuadCenter / uScreenSize) * 2.0 - 1.0;
    vec2 vMidPointNDC = midNDC;
    vec2 vLocalOffsetNDC = fragNDC - midNDC;

    vec2 p = (localUV - CENTER) * 2.0;

    float d = sdSuperellipse(p, uPowerFactor, 1.0);

    // Small edge smoothing only for corner stair-stepping.
    float edge = 1.0 - smoothstep(-0.003, 0.003, d);

    if (edge <= 0.0) discard;

    float dist = max(-d, 0.0);

    // Refraction.
    float refraction = pow(f(dist), uRefractionPower);

    // Refracted screen-space position.
    vec2 targetNDC = vMidPointNDC + vLocalOffsetNDC * refraction;

    vec2 sampleUV = targetNDC * 0.5 + vec2(0.5);

    // Off-screen sampling: fade out gracefully instead of debug magenta.
    if (OutOfBounds(sampleUV)) { discard; }

    vec4 color = texture2D(uBlurTex, sampleUV);

    // Stable screen-space grain.
    float noise = (rand(gl_FragCoord.xy * 1e-3) - 0.5) * uNoise;

    // Subtle glass grain.
    color.rgb += vec3(noise);

    // Keep RGB coverage aligned with alpha.
    color.rgb *= edge;

    // Directional rim lighting.
    float glow = Glow(localUV);

    // Edge glow mask.
    float glowMask = smoothstep(uGlowEdge0, uGlowEdge1, dist);

    // Directional rim lighting.
    float glowStrength = glow * uGlowWeight * glowMask + 1.0 + uGlowBias;

    color.rgb *= glowStrength;

    gl_FragColor = vec4(color.rgb, edge);
}
