import os
import re

utils_path = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\utils.js'
core_dir = r'c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\core'
os.makedirs(core_dir, exist_ok=True)

with open(utils_path, 'r', encoding='utf-8') as f:
    content = f.read()

def extract_function(name):
    global content
    search_str = f'export function {name}('
    start_idx = content.find(search_str)
    if start_idx == -1: return ''
    
    brace_count = 0
    end_idx = -1
    started = False
    
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            brace_count += 1
            started = True
        elif content[i] == '}':
            brace_count -= 1
        
        if started and brace_count == 0:
            end_idx = i + 1
            break
            
    if end_idx != -1:
        func_str = content[start_idx:end_idx]
        content = content[:start_idx] + content[end_idx:]
        return func_str + '\n\n'
    return ''

# 1. math.js
math_funcs = ['getCanvasCoords', 'checkSmartGuides', 'centerLevel', 'optimizeEntities']
math_content = "import { state } from '../state.js';\n\n"
for f in math_funcs:
    math_content += extract_function(f)

# 2. serializer.js
serializer_funcs = ['updateJSON', 'getLevelJSON']
serializer_content = "import { state } from '../state.js';\nimport { BASE_WIDTH, BASE_HEIGHT } from '../constants.js';\n\n"
for f in serializer_funcs:
    serializer_content += extract_function(f)

# 3. transform.js
transform_funcs = ['transformSelection', 'scaleSelection', 'bringSelectionToFront', 'sendSelectionToBack', 'alignSelection', 'distributeSelection']
transform_content = "import { state, saveState } from '../state.js';\nimport { updateProperties, updateJSON, updateSelectionStats } from '../utils.js';\nimport { Entity } from '../entities.js';\n\n"
for f in transform_funcs:
    transform_content += extract_function(f)

with open(os.path.join(core_dir, 'math.js'), 'w', encoding='utf-8') as f:
    f.write(math_content)

with open(os.path.join(core_dir, 'serializer.js'), 'w', encoding='utf-8') as f:
    f.write(serializer_content)

with open(os.path.join(core_dir, 'transform.js'), 'w', encoding='utf-8') as f:
    f.write(transform_content)

# update utils.js to export them
re_exports = """
export { getCanvasCoords, checkSmartGuides, centerLevel, optimizeEntities } from './core/math.js';
export { updateJSON, getLevelJSON } from './core/serializer.js';
export { transformSelection, scaleSelection, bringSelectionToFront, sendSelectionToBack, alignSelection, distributeSelection } from './core/transform.js';
"""
content = content.replace('// Re-exports de UI', '// Re-exports\n' + re_exports + '\n// Re-exports de UI')

with open(utils_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Phase 3 extraction complete!')
