#version 120
#extension GL_ARB_shader_texture_lod : enable

/**
 * Liquid Glass Shader for FDPNext
 * Based on Jacquesqwq/LiquidGlassShader V2 Tinted
 *
 * Uses gl_TexCoord[0] (0-1 local UV) for shape, and gl_FragCoord for
 * screen-space refraction sampling. No custom vertex shader needed -
 * works with the standard vertex.vsh that passes gl_MultiTexCoord0.
 */

uniform sampler2D uBlurTex;
uniform vec2 uScreenSize;    // scaledWidth, scaledHeight (matches HUD coordinate space)
uniform float uPowerFactor;  // superellipse power (controls corner roundness)
uniform float uNoise;
uniform float uRefractionPower;
uniform vec3 uTintColor;
uniform float uTintStrength;
uniform float uChromaStrength;
uniform float uDarkness;
uniform float uBlurRadius;   // mipmap LOD for blur
uniform float uGlobalAlpha;

const float M_E = 2.718281828459045;
const vec2 CENTER = vec2(0.5);

float f(float x) {
    return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7);
}

float sdSuperellipse(vec2 p, float n, float r) {
    vec2 absP = abs(p);
    float numerator = pow(absP.x, n) + pow(absP.y, n) - pow(r, n);
    float denominator = n * sqrt(pow(absP.x, 2.0 * n - 2.0) + pow(absP.y, 2.0 * n - 2.0)) + 0.00001;
    return numerator / denominator;
}

bool OutOfBounds(vec2 uv) { return max(uv.x, uv.y) > 1.0 || min(uv.x, uv.y) < 0.0; }

void main() {
    // gl_TexCoord[0].xy is 0-1 local UV from the vertex shader
    vec2 localUV = gl_TexCoord[0].xy;

    vec2 p = (localUV - CENTER) * 2.0;
    float d = sdSuperellipse(p, uPowerFactor, 1.0);
    float edge = 1.0 - smoothstep(-0.003, 0.003, d);
    if (edge <= 0.0) discard;

    // Material sampling.
    float dist = clamp(max(-d, 0.0), 0.0, 1.0);
    float fresnel = pow(1.0 - dist, 3.0);
    float refraction = pow(f(dist), uRefractionPower);

    // Screen-space UV from gl_FragCoord (flip Y: HUD is top-left origin, GL is bottom-left)
    vec2 screenUV = vec2(gl_FragCoord.x, uScreenSize.y - gl_FragCoord.y) / uScreenSize;

    // Refraction offset: shift the screen UV based on local position
    vec2 localOffset = (localUV - CENTER) * 2.0;
    vec2 sampleUV = screenUV + localOffset * refraction * 0.05;
    sampleUV = clamp(sampleUV, vec2(0.0), vec2(1.0));

    // Chromatic softness
    vec2 chromaDir = normalize(localOffset + 0.00001);
    vec2 chromaOffset = chromaDir * fresnel * uChromaStrength;

    // Sample with mipmap LOD for blur effect
    float lodLevel = uBlurRadius * (1.0 - dist * 0.5);

    vec4 color = vec4(
        texture2DLod(uBlurTex, sampleUV + chromaOffset, lodLevel).r,
        texture2DLod(uBlurTex, sampleUV, lodLevel).g,
        texture2DLod(uBlurTex, sampleUV - chromaOffset, lodLevel).b,
        1.0
    );

    // Low-frequency material diffusion.
    float micro = sin(gl_FragCoord.x * 0.015 + gl_FragCoord.y * 0.008) * sin(gl_FragCoord.y * 0.012 - gl_FragCoord.x * 0.006);
    color.rgb += micro * uNoise * 0.015;

    // Material shaping.
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float grain = (micro - 0.5) * uNoise * 0.22;
    color.rgb += grain;

    vec3 grayscale = vec3(luma);
    float saturation = mix(0.45, 0.75, luma);
    color.rgb = mix(grayscale, color.rgb, saturation);

    vec3 adaptiveTintColor = mix(uTintColor, vec3(0.08, 0.09, 0.11), uDarkness);
    float adaptiveTint = uTintStrength * (1.0 - luma * 0.5);
    color.rgb = mix(color.rgb, adaptiveTintColor, adaptiveTint);

    float adaptiveFresnel = mix(0.12, 0.06, luma);
    color.rgb += uTintColor * fresnel * adaptiveFresnel;

    color.rgb *= 0.96 + smoothstep(0.0, 1.0, dist) * 0.04;
    color.rgb *= mix(1.08, 0.98, luma) * mix(1.0, 0.82, uDarkness);

    // Optical edge falloff.
    float opticalEdge = pow(edge, 1.35);
    color.rgb *= opticalEdge;

    gl_FragColor = vec4(color.rgb, opticalEdge * uGlobalAlpha);
}
