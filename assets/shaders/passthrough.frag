#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float u_time;

varying vec4 v_color;
varying vec2 v_texCoords;

float getBayer4x4(vec2 p) {
    ivec2 c = ivec2(mod(p, 4.0));
    const mat4 m = mat4(
            vec4(0.0, 12.0, 3.0, 15.0), // x = 0
            vec4(8.0, 4.0, 11.0, 7.0),  // x = 1
            vec4(2.0, 14.0, 1.0, 13.0), // x = 2
            vec4(10.0, 6.0, 9.0, 5.0)   // x = 3
    );
    return m[c.x][c.y] / 16.0;
}

void main() {
    vec4 tex_color = texture2D(u_texture, v_texCoords);

    float mosaic_scale = 128.0;
    float mosaic_scale_width = mosaic_scale * (u_resolution.x / u_resolution.y);

    vec2 cell_coord = vec2(
            floor(v_texCoords.x * mosaic_scale_width),
            floor(v_texCoords.y * mosaic_scale)
    );

    if(u_time <= 2.0){

        float bayer_threshold = getBayer4x4(cell_coord);

        //    float gradient_value = v_texCoords.y;
        float gradient_value = (1.0 - abs(u_time - 1.0)) * 2.0;

        if (gradient_value >= bayer_threshold) {
            //tex_color = vec4(cell_coord.x / mosaic_scale, cell_coord.y / mosaic_scale, 0.0, 0.0);
            gl_FragColor = vec4(0.078, 0.078, 0.078, 1.0);
        } else {
            gl_FragColor = tex_color * v_color;
        }
    }else{
        gl_FragColor = tex_color * v_color;
    }

}
