import re
import os

utils_path = r"c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\utils.js"
ui_dir = r"c:\Users\Robert\Desarrollo-Juego-de-Pelota-con-Sensores\Constructor-de-Niveles-Web-para-Juego-de-Pelota-con-Sensores\js\ui"
os.makedirs(ui_dir, exist_ok=True)

prop_panel_path = os.path.join(ui_dir, "PropertiesPanel.js")
osd_path = os.path.join(ui_dir, "OSD.js")

with open(utils_path, "r", encoding="utf-8") as f:
    content = f.read()

def extract_function(name):
    global content
    search_str = f"export function {name}("
    start_idx = content.find(search_str)
    if start_idx == -1: return ""
    
    brace_count = 0
    end_idx = -1
    started = False
    
    for i in range(start_idx, len(content)):
        if content[i] == "{":
            brace_count += 1
            started = True
        elif content[i] == "}":
            brace_count -= 1
        
        if started and brace_count == 0:
            end_idx = i + 1
            break
            
    if end_idx != -1:
        func_str = content[start_idx:end_idx]
        content = content[:start_idx] + content[end_idx:]
        return func_str + "\n\n"
    return ""

func_update_props = extract_function("updateProperties")
func_update_stats = extract_function("updateSelectionStats")
func_show_osd = extract_function("showOSD")

prop_content = """import { state } from '../state.js';
import { updateJSON } from '../utils.js';

""" + func_update_props + func_update_stats

osd_content = """export function showOSD(title, value, icon) {
    let container = document.getElementById('osd-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'osd-container';
        container.className = 'osd-container';
        document.body.appendChild(container);
    }

    const osd = document.createElement('div');
    osd.className = 'osd-toast';
    
    let iconHtml = '';
    if (icon) {
        iconHtml = `<div class="osd-icon">${icon}</div>`;
    }

    osd.innerHTML = `
        ${iconHtml}
        <div class="osd-content">
            <div class="osd-title">${title}</div>
            <div class="osd-value">${value}</div>
        </div>
    `;

    container.appendChild(osd);

    // Trigger animation
    requestAnimationFrame(() => {
        osd.classList.add('show');
    });

    setTimeout(() => {
        osd.classList.remove('show');
        setTimeout(() => {
            if (osd.parentNode === container) {
                container.removeChild(osd);
            }
        }, 300); // Wait for transition
    }, 2000);
}
"""

if func_update_props:
    with open(prop_panel_path, "w", encoding="utf-8") as f:
        f.write(prop_content)

if func_show_osd:
    with open(osd_path, "w", encoding="utf-8") as f:
        f.write(osd_content)

# add imports to utils.js at the top if needed
# We actually removed these functions, we need to export them from their new homes?
# Actually, editor.js and others might still import them from utils.js, so let's re-export them from utils.js!

reexports = """
// Re-exports de UI
export { updateProperties, updateSelectionStats } from './ui/PropertiesPanel.js';
export { showOSD } from './ui/OSD.js';
"""

# Insert re-exports after the last import in utils.js
last_import_idx = content.rfind("import ")
if last_import_idx != -1:
    end_of_line = content.find("\\n", last_import_idx)
    content = content[:end_of_line+1] + reexports + content[end_of_line+1:]
else:
    content = reexports + content

with open(utils_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Split completed successfully.")
