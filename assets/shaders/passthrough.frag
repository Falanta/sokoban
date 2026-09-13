#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float u_time;

varying vec4 v_color;
varying vec2 v_texCoords;

float getBayer4x4(vec2 p) {
    vec2 a = mod(p, 4.0);
    float result = 0.0;
    result += mod(a.x, 2.0) * 8.0 + floor(mod(a.x, 4.0) / 2.0) * 2.0;
    result += mod(a.y, 2.0) * 4.0 + floor(mod(a.y, 4.0) / 2.0) * 1.0;
    return (result + 0.5) / 16.0;
}

void main() {
    vec4 tex_color = texture2D(u_texture, v_texCoords);

    float mosaic_scale = 64.0;
    float mosaic_scale_width = mosaic_scale * (u_resolution.x / u_resolution.y);

    vec2 cell_coord = vec2(
            floor(v_texCoords.x * mosaic_scale_width),
            floor(v_texCoords.y * mosaic_scale)
    );

    float bayer_threshold = getBayer4x4(cell_coord);

    float step_time = floor(u_time * 0.1) / mosaic_scale;
    float gradient_value = -v_texCoords.y + 2 + (1-step_time);

    if (gradient_value >= bayer_threshold) {
        tex_color += vec4(cell_coord.x / mosaic_scale, cell_coord.y / mosaic_scale, 0.0, 0.0);
    }

    gl_FragColor = tex_color * v_color;
}
