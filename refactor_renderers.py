import os
import re

source_file = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\renderers\entityRenderers.js'
types_dir = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\renderers\types'
color_utils = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\renderers\colorUtils.js'

os.makedirs(types_dir, exist_ok=True)

with open(source_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Extract color utils
utils_content = """export function hexToHSL(hex) {
    hex = hex.replace('#', '');
    let r = parseInt(hex.substring(0, 2), 16) / 255;
    let g = parseInt(hex.substring(2, 4), 16) / 255;
    let b = parseInt(hex.substring(4, 6), 16) / 255;
    
    let max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;

    if (max === min) {
        h = s = 0;
    } else {
        let d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = (g - b) / d + (g < b ? 6 : 0); break;
            case g: h = (b - r) / d + 2; break;
            case b: h = (r - g) / d + 4; break;
        }
        h /= 6;
    }
    return { h: h * 360, s: s * 100, l: l * 100 };
}

export function hslToHex(h, s, l) {
    s /= 100;
    l /= 100;
    let c = (1 - Math.abs(2 * l - 1)) * s;
    let x = c * (1 - Math.abs((h / 60) % 2 - 1));
    let m = l - c/2;
    let r = 0, g = 0, b = 0;
    
    if (0 <= h && h < 60) { r = c; g = x; b = 0; }
    else if (60 <= h && h < 120) { r = x; g = c; b = 0; }
    else if (120 <= h && h < 180) { r = 0; g = c; b = x; }
    else if (180 <= h && h < 240) { r = 0; g = x; b = c; }
    else if (240 <= h && h < 300) { r = x; g = 0; b = c; }
    else if (300 <= h && h < 360) { r = c; g = 0; b = x; }

    let rHex = Math.round((r + m) * 255).toString(16).padStart(2, '0');
    let gHex = Math.round((g + m) * 255).toString(16).padStart(2, '0');
    let bHex = Math.round((b + m) * 255).toString(16).padStart(2, '0');
    return `#${rHex}${gHex}${bHex}`;
}

export function getProceduralColor(baseHex, linkId) {
    if (!linkId) return baseHex;
    let hash = 0;
    const s = String(linkId);
    for (let i = 0; i < s.length; i++) {
        hash = s.charCodeAt(i) + ((hash << 5) - hash);
    }
    hash = Math.abs(hash);
    const hsl = hexToHSL(baseHex);
    const hueShift = (hash % 12) * 30;
    hsl.h = (hsl.h + hueShift) % 360;
    return hslToHex(hsl.h, hsl.s, hsl.l);
}
"""
with open(color_utils, 'w', encoding='utf-8') as f:
    f.write(utils_content)

# Extract cases
cases = re.findall(r"case '([^']+)':\s*{([\s\S]*?)break;\s*}", content)

imports = ["import { state } from '../../state.js';\n"]
main_imports = []
main_dict = []

for case_name, case_body in cases:
    # clean up the body indentation
    case_body = '\n'.join([line.replace('            ', '    ', 1) for line in case_body.split('\n')])
    
    func_name = 'draw' + ''.join([p.capitalize() for p in case_name.split('_')])
    
    file_content = "import { state } from '../../state.js';\n"
    if 'getProceduralColor' in case_body:
        file_content += "import { getProceduralColor } from '../colorUtils.js';\n"
        
    file_content += f"\nexport function {func_name}(ctx, entity, color, activeTheme) {{\n"
    file_content += case_body
    file_content += "\n}\n"
    
    with open(os.path.join(types_dir, f"{case_name}.js"), 'w', encoding='utf-8') as f:
        f.write(file_content)
        
    main_imports.append(f"import {{ {func_name} }} from './types/{case_name}.js';")
    main_dict.append(f"    '{case_name}': {func_name},")

new_main = "import { state } from '../state.js';\n" + '\n'.join(main_imports) + "\n\n"
new_main += "const renderers = {\n" + '\n'.join(main_dict) + "\n};\n\n"
new_main += """export function drawEntity(entity, ctx) {
    const themeInput = document.getElementById('themeInput');
    const selectedTheme = themeInput ? themeInput.value : 'industrial';
    const activeTheme = state.themes[selectedTheme] || state.themes.industrial;

    const color = activeTheme[entity.type] || '#fff';
    
    if (renderers[entity.type]) {
        renderers[entity.type](ctx, entity, color, activeTheme);
    } else {
        ctx.fillStyle = color;
        ctx.fillRect(entity.x, entity.y, entity.w, entity.h);
        
        ctx.strokeStyle = 'rgba(255,255,255,0.1)';
        ctx.lineWidth = 1;
        ctx.strokeRect(entity.x, entity.y, entity.w, entity.h);
    }
}
"""

with open(source_file, 'w', encoding='utf-8') as f:
    f.write(new_main)

print('Phase 4 extraction complete!')
